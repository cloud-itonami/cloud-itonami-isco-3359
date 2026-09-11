(ns regfiling.advisor
  "Regulatory Filing Advisor — the advisor named in this repository's
  README, proposing a regulatory-filing documentation/logistics
  operation (log an inspection/filing-status record, schedule a
  compliance-review appointment, flag a compliance concern for human
  review, or coordinate an office/inspection-equipment supply order)
  from an office's filing intake and case register. Swappable
  mock/llm; the advisor ONLY proposes — `regfiling.governor` checks
  office/case provenance and the supply-order cost ceiling
  independently, and always escalates `:flag-compliance-concern` and
  above-threshold supply orders. Modeled on
  cloud-itonami-isco-3313's advisor.

  The advisor NEVER proposes issuing a compliance ruling, imposing a
  regulatory penalty, or ordering an enforcement action — there is no
  such :op in this namespace or anywhere in this actor's closed
  allowlist (`regfiling.governor/closed-op-allowlist`). If a caller
  somehow requests an op outside that allowlist, `infer` below still
  returns only a :propose-effect data map (never dispatches
  anything); the governor then structurally hard-blocks it via
  `:disallowed-op` before any commit.

  A proposal: {:op :log-inspection-record|:schedule-review-appointment
                   |:flag-compliance-concern|:coordinate-supply-order
               :effect :propose :office-id str :case-id str
               :confidence n :stake kw :rationale str, plus
               op-specific fields (:record-detail, :proposed-time,
               :concern-detail, :item/:cost).}"
  )

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(def ^:private confidence-by-stake {:high 0.7 :medium 0.85 :low 0.95})

(defn- base-proposal [{:keys [op office-id stake] :as _request}]
  {:op op
   :effect :propose
   :office-id office-id
   :stake (or stake :low)
   :confidence (get confidence-by-stake (or stake :low) 0.95)})

(defn- infer
  [_store {:keys [op office-id case-id proposed-time record-detail concern-detail item cost]
           :as request}]
  (let [base (base-proposal request)]
    (case op
      :log-inspection-record
      (assoc base
             :case-id case-id
             :record-detail record-detail
             :rationale (str "logged inspection/filing-status record for case "
                              case-id " (documentation only, not a ruling)"))

      :schedule-review-appointment
      (assoc base
             :case-id case-id
             :proposed-time proposed-time
             :rationale (str "proposed compliance-review appointment for case "
                              case-id " at " proposed-time))

      :flag-compliance-concern
      (assoc base
             :case-id case-id
             :concern-detail concern-detail
             ;; NOTE: this rationale deliberately mentions the bare
             ;; nouns "ruling" and "penalty" as part of a routine,
             ;; benign observation -- this is the exact shape of text
             ;; that previously self-tripped a naive governor whose
             ;; scope-exclusion term list matched on bare nouns rather
             ;; than finalization/execution action phrases. See
             ;; regfiling.governor's `scope-exclusion-phrases` and the
             ;; `default-mock-advisor-proposals-never-self-trip` test.
             :rationale (str "flagged compliance concern for case " case-id ": "
                              concern-detail
                              " — pattern suggests a compliance ruling might be"
                              " warranted and potential penalty exposure exists;"
                              " routed to a human regulatory official for review"
                              " (this advisor does not issue rulings or impose"
                              " penalties itself)"))

      :coordinate-supply-order
      (assoc base
             :item item
             :cost cost
             :rationale (str "proposed procurement of " item
                              " (cost " cost ") for office " office-id))

      ;; Unknown/disallowed op: still only ever a :propose-effect data
      ;; map, never an actuation -- the governor's closed allowlist
      ;; hard-blocks this before commit regardless of confidence.
      (assoc base
             :confidence 0.0
             :stake :high
             :rationale (str "unrecognized op " op
                              " — not in the closed administrative allowlist")))))

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a regulatory-filing documentation/logistics-coordination
   advisor for a government regulatory office. Given a request,
   propose one of exactly four ops: :log-inspection-record,
   :schedule-review-appointment, :flag-compliance-concern, or
   :coordinate-supply-order. Never propose issuing a compliance
   ruling, imposing a regulatory penalty, or ordering an enforcement
   action -- you have no authority to do so and no such op exists.
   Any observation that might warrant regulatory action must be
   surfaced only via :flag-compliance-concern, which always escalates
   to a human regulatory official. Give an honest :confidence and
   :stake; the governor checks office/case provenance and the
   supply-order cost ceiling independently.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
