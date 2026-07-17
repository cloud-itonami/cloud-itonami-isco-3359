(ns regfiling.store
  "SSoT for the ISCO-08 3359 regulatory-filing coordination practice
  actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a regulatory-filing
  documentation and logistics-coordination robot performs case-file
  data entry, appointment scheduling and office-supply coordination
  under this advisor/governor pair, which never dispatches hardware
  itself and never issues a compliance ruling, imposes a regulatory
  penalty or orders an enforcement action — no such capability exists
  in this store or anywhere in this actor). Modeled on
  cloud-itonami-isco-3313's accountingsupport.store.

  Domain:

    office  — a registered government office/regulatory unit
              {:office-id :name :max-supply-order-cost number}.
              `:max-supply-order-cost` is the registered ceiling a
              proposed supply-order cost above which always requires
              human sign-off (ESCALATE, not a hard block — this actor
              never blocks procurement outright, it only routes
              above-threshold requests to a human).
    case    — a registered filing/inspection case under an office
              {:case-id :office-id :name}. Case records are the basis
              a documentation proposal (inspection log, review
              appointment, compliance flag) must cite — an
              independently-verified/registered case, never an
              invented one.
    record  — a committed operating record (a logged documentation
              entry, a scheduled appointment, a flagged concern, or a
              procurement coordination note) — written ONLY via
              commit-record!. Never a compliance ruling, penalty or
              enforcement order — this store has no such write path.
    ledger  — append-only audit trail, commit or hold.")

(defprotocol Store
  (office [s office-id])
  (filing-case [s case-id])
  (records-of [s office-id])
  (ledger [s])
  (register-office! [s o])
  (register-filing-case! [s c])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (office [_ office-id] (get-in @a [:offices office-id]))
  (filing-case [_ case-id] (get-in @a [:cases case-id]))
  (records-of [_ office-id] (filter #(= office-id (:office-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-office! [s o]
    (swap! a assoc-in [:offices (:office-id o)] o) s)
  (register-filing-case! [s c]
    (swap! a assoc-in [:cases (:case-id c)] c) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:offices {} :cases {} :records [] :ledger []}
                                    seed)))))
