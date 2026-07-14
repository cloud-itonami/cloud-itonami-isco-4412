(ns mailrouting.store
  "SSoT for the ISCO-08 4412 independent mail sorting & delivery
  practice actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md
  Actors section; README's 'Robotics premise' — a sorting and
  local-delivery robot performs mail sorting by route and last-mile
  delivery drop-off under an advisor/governor pair that never
  dispatches hardware itself). Modeled on
  cloud-itonami-isco-4214's debtcollection.store.

  Domain:

    client — a registered mail account holder (small business,
             residential customer, co-op delivery network)
             {:client-id :name}
    item   — a registered mail item with a VERIFIED tracking/manifest
             record {:item-id :client-id :address-on-file
             :protected? boolean}. `:protected?` marks certified/
             registered/court mail — items the governor must never let
             be autonomously rerouted or disposed regardless of
             confidence. An item with no entry in the store has no
             verified tracking/manifest record — the governor treats
             lookup-miss as a hard violation, not an implicit pass.
    record — a committed operating record (a sort/route/hold/flag/
             return/redirect/dispose disposition) — written ONLY via
             commit-record!, never mutated in place.
    ledger — an append-only audit trail of every proposal/verdict/
             disposition, regardless of outcome (commit or hold).")

(defprotocol Store
  (client [s client-id])
  (item [s item-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-item! [s it])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (item [_ item-id] (get-in @a [:items item-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-item! [s it]
    (swap! a assoc-in [:items (:item-id it)] it) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :items {} :records [] :ledger []}
                                    seed)))))
