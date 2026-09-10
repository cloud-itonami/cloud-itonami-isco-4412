(ns mailrouting.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [mailrouting.actor :as actor]
            [mailrouting.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Acme Co-op Delivery"})
    (store/register-item! st {:item-id "I-1" :client-id "client-1"
                               :address-on-file "1 Main St" :protected? false})
    (store/register-item! st {:item-id "I-2" :client-id "client-1"
                               :address-on-file "9 Court Way" :protected? true})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :item-id "I-1" :op :sort-item :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-on-item-without-verified-manifest-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :item-id "I-ghost" :op :sort-item :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "client-1")))
    (is (= :hold (:disposition (:state result))))))

(deftest holds-on-protected-class-redirect-without-interrupt-or-commit
  (testing "protected-class redirect is refused by construction, never reaches human approval"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:client-id "client-1" :item-id "I-2" :op :redirect-mail
                    :redirect-to-address "2 Elsewhere Ave" :stake :low}
          result (actor/run-request! graph request {} "thread-3")]
      (is (= :done (:status result)))
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "client-1"))))))

(deftest interrupts-then-commits-dispose-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; disposal of an unclaimed (unprotected) item always escalates
        request {:client-id "client-1" :item-id "I-1" :op :dispose-item :stake :high}
        interrupted (actor/run-request! graph request {} "thread-4")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-4")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "client-1")))))))

(deftest interrupts-then-commits-redirect-elsewhere-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; redirecting an unprotected item to an address different from
        ;; the one on file is an identity/fraud risk that always escalates
        request {:client-id "client-1" :item-id "I-1" :op :redirect-mail
                  :redirect-to-address "2 Elsewhere Ave" :stake :medium}
        interrupted (actor/run-request! graph request {} "thread-5")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-5")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
