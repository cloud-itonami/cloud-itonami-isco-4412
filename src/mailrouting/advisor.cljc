(ns mailrouting.advisor
  "Mail Advisor — the advisor named in this repository's README,
  proposing a mail-handling operation (sort an item, route it,
  hold it for pickup, flag it undeliverable, return it to sender,
  redirect it to a different address, dispose of an unclaimed item)
  for a registered item. Swappable mock/llm; the advisor ONLY
  proposes — `mailrouting.governor` independently checks the item's
  verified manifest record and protected-class flag, and always
  escalates address redirects and disposals. Modeled on
  cloud-itonami-isco-4214's advisor.

  A proposal is a map:
    {:op :sort-item|:route-item|:hold-for-pickup|:flag-undeliverable|
         :return-to-sender|:redirect-mail|:dispose-item
     :effect :propose              ; the advisor NEVER emits a raw actuation
     :redirect-to-address str|nil  ; only meaningful for :redirect-mail
     :stake :low|:medium|:high
     :confidence 0.0-1.0
     :rationale str}
  LLM parse failures always yield `:confidence 0.0` (never fabricate
  confidence), which forces the governor to escalate/hold."
  (:require [clojure.string :as str]
            #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer
  "Deterministic mock inference: reads the request's declared op/stake
  straight through (a stand-in for what an LLM would extract from
  intake data), with a stake-derived confidence."
  [_store {:keys [op stake redirect-to-address] :as request}]
  {:op op
   :effect :propose
   :redirect-to-address redirect-to-address
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for item " (:item-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a mail advisor. Given a mail-handling operation request,
   propose an :op, an honest :confidence (0.0-1.0), and a :stake
   (:low/:medium/:high). Never fabricate confidence you don't have.
   Redirects and disposals always require human sign-off regardless of
   confidence, and protected-class (certified/registered/court) mail
   may never be redirected or disposed autonomously — the governor
   checks both independently against the item's registered record.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  "Wraps a `langchain.model/ChatModel`. `gen-opts` is passed through to
  `model/-generate`. Kept decoupled from any concrete model so this ns
  has no hard dependency beyond `langchain.model`'s protocol."
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
