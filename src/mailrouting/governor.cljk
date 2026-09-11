(ns mailrouting.governor
  "MailServicesGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating every
  sorting/routing/delivery action a Mail Advisor may propose for a
  mail item. The governor never dispatches hardware itself and never
  releases registered/certified mail without recipient verification.
  Modeled on cloud-itonami-isco-4214's debtcollection.governor. Task
  twist: a proposal must cite an item with a VERIFIED tracking/
  manifest record already on file, and protected-class mail (e.g.
  certified/registered/court mail) is refused for autonomous
  rerouting/disposal by construction, not merely discouraged.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable — not
  even via `:request-approval`/`approve!`, since these are refused by
  construction rather than gated on human sign-off):
    1. client provenance     — the mail account holder must be
                               registered.
    2. verified manifest     — the item must have a verified tracking/
                               manifest record in the store BEFORE any
                               sorting/routing/handling action.
    3. no-actuation          — proposal :effect must be :propose (the
                               actor never directly actuates; it only
                               proposes what a robot/human may do).
    4. protected-class guard — an item flagged `:protected?` in the
                               store (certified/registered/court mail)
                               may never be autonomously `:redirect-
                               mail`ed or `:dispose-item`d, regardless
                               of confidence.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    5. :redirect-mail to an address different from the one on file
                               (identity/fraud risk) — a redirect
                               target matching the address on file is
                               not a risky reroute.
    6. :dispose-item          (irreversible loss of property).
    7. low confidence         (< `confidence-floor`)."
  (:require [mailrouting.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:dispose-item})

(defn- hard-violations [{:keys [proposal]} client-record item-record]
  (let [{:keys [op effect]} proposal
        protected? (:protected? item-record)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (nil? item-record)
      (conj {:rule :no-manifest
             :detail "verified tracking/manifest record が無いアイテムへの取扱は不可"})

      (not= :propose effect)
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（actor は直接作動しない）"})

      (and item-record protected? (= op :redirect-mail))
      (conj {:rule :protected-class-no-redirect
             :detail "保護区分郵便（書留/配達証明/裁判所郵便）は自律的な転送不可（コンストラクトとして拒否）"})

      (and item-record protected? (= op :dispose-item))
      (conj {:rule :protected-class-no-dispose
             :detail "保護区分郵便は自律的な廃棄不可（コンストラクトとして拒否）"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `mailrouting.store/Store`. Pure — never mutates
  the store, never dispatches a robot action. Returns `{:ok?
  :violations :confidence :hard? :escalate?}`."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        item-record (store/item store (:item-id request))
        hard (hard-violations {:proposal proposal} client-record item-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        redirect? (= :redirect-mail (:op proposal))
        redirect-elsewhere? (and redirect? item-record
                                  (not= (:redirect-to-address proposal)
                                        (:address-on-file item-record)))
        always-risky? (or (contains? always-escalate-ops (:op proposal))
                           redirect-elsewhere?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
