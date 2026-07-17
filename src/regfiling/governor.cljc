(ns regfiling.governor
  "RegFiling Governor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating every
  documentation, scheduling, escalation and procurement-coordination
  action an advisor may propose for a government regulatory office.
  The governor never dispatches hardware itself and — this is the
  central invariant of this whole actor — it has NO capability to
  issue a compliance ruling, impose a regulatory penalty, or order an
  enforcement action, because no such op exists in this namespace's
  closed allowlist at all. That is not a policy the governor chooses
  to enforce at runtime; it is a structural absence upstream of any
  runtime check. `closed-op-allowlist` below is the entire universe of
  actions this actor can ever propose, and it contains four
  documentation/logistics ops and nothing resembling enforcement.
  Modeled on cloud-itonami-isco-3313's accountingsupport.governor.
  Task twist: no per-op ceiling is ever a HARD block in this
  domain — the only permanent, un-overridable blocks are provenance,
  no-actuation, the closed allowlist itself, and a defense-in-depth
  scope-exclusion text scan; a supply-order cost ceiling is an
  ESCALATE-only signal (this actor never blocks procurement outright,
  it always lets a human decide above-threshold spend).

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. office provenance        — the government office/regulatory
                                unit must be registered.
    2. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never finalizes any regulatory
                                decision; it only gates what the
                                advisor may log/schedule/flag/order).
    3. disallowed-op          — the proposal's :op must be a member of
                                `closed-op-allowlist`. THIS IS THE
                                STRUCTURAL ENFORCEMENT-AUTHORITY GUARD:
                                issuing a compliance ruling, imposing a
                                regulatory penalty, or ordering an
                                enforcement action are not merely
                                gated behind escalation — those op
                                names are absent from the allowlist
                                entirely, so any such proposal (from a
                                buggy advisor, a compromised advisor,
                                or a caller bypassing the advisor)
                                fails this check and is permanently
                                held, never committed, never
                                overridable by human approval.
    4. case basis             — a documentation proposal (inspection
                                log, review appointment, compliance
                                flag) must cite a REGISTERED case
                                belonging to the request's office.
    5. scope-exclusion-hit    — defense-in-depth free-text scan: if
                                any free-text field of the proposal
                                (:rationale, :record-detail,
                                :concern-detail) contains a
                                finalization/execution phrase like
                                \"issue the compliance ruling\" or
                                \"impose the regulatory penalty\", the
                                proposal is hard-blocked regardless of
                                :op. Phrased as ACTION phrases (verb +
                                object), never bare nouns like
                                \"ruling\"/\"penalty\" — a bare-noun
                                term list would false-trip on entirely
                                legitimate :flag-compliance-concern
                                rationale text that merely *mentions*
                                that a ruling or penalty might
                                eventually be warranted by a human (see
                                `regfiling.advisor`'s mock rationale and
                                the
                                `default-mock-advisor-proposals-never-self-trip`
                                test below).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :flag-compliance-concern (any observation that might
                                warrant regulatory action is surfaced
                                ONLY through this op, and it always
                                escalates immediately to a human
                                regulatory official — it never
                                auto-commits).
    7. supply-order cost above the office's registered
                                `:max-supply-order-cost` (not a hard
                                block — a human decides above-threshold
                                procurement spend).
    8. low confidence (< `confidence-floor`)."
  (:require [regfiling.store :as store]
            [clojure.string :as str]))

(def confidence-floor 0.6)

(def closed-op-allowlist
  "The entire universe of ops this actor can ever propose. Nothing
  resembling issuing a compliance ruling, imposing a regulatory
  penalty, or ordering an enforcement action exists here — this set
  IS the enforcement-authority guarantee, not just its enforcement
  mechanism."
  #{:log-inspection-record
    :schedule-review-appointment
    :flag-compliance-concern
    :coordinate-supply-order})

(def ^:private always-escalate-ops #{:flag-compliance-concern})

(def ^:private case-basis-ops
  #{:log-inspection-record :schedule-review-appointment :flag-compliance-concern})

(def scope-exclusion-phrases
  "Finalization/execution ACTION phrases (verb + object), never bare
  nouns -- see the governor docstring and
  `default-mock-advisor-proposals-never-self-trip` for why bare nouns
  like \"ruling\" or \"penalty\" would false-trip on legitimate
  :flag-compliance-concern rationale text."
  ["issue the compliance ruling"
   "issue a compliance ruling"
   "impose the regulatory penalty"
   "impose a regulatory penalty"
   "order the enforcement action"
   "order an enforcement action"])

(defn- proposal-text-blob [proposal]
  (str/lower-case (str/join " " (keep (fn [[_ v]] (when (string? v) v)) proposal))))

(defn scope-exclusion-hit? [proposal]
  (let [blob (proposal-text-blob proposal)]
    (boolean (some #(str/includes? blob %) scope-exclusion-phrases))))

(defn- hard-violations [{:keys [request proposal]} office-record c]
  (let [{:keys [op case-id]} proposal
        needs-case? (contains? case-basis-ops op)]
    (cond-> []
      (nil? office-record)
      (conj {:rule :no-office :detail "未登録 office"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はいかなる規制判断も自動実行しない）"})

      (not (contains? closed-op-allowlist op))
      (conj {:rule :disallowed-op
             :detail (str "op " op " は許可された administrative allowlist に存在しない — "
                          "compliance ruling / penalty / enforcement action 相当の op は構造的に未実装")})

      (and needs-case? (nil? c))
      (conj {:rule :unknown-case :detail "未登録 case への提案は不可"})

      (and needs-case? c (not= (:office-id c) (:office-id request)))
      (conj {:rule :case-wrong-office :detail "case が別 office のもの"})

      (scope-exclusion-hit? proposal)
      (conj {:rule :scope-exclusion-hit
             :detail "提案テキストに compliance ruling / regulatory penalty / enforcement action の実行を示す文言が含まれる"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `regfiling.store/Store`. Pure — never mutates
  the store, never issues a ruling, never imposes a penalty, never
  orders an enforcement action (no such write path exists)."
  [request context proposal store]
  (let [office-record (store/office store (:office-id request))
        c (some->> (:case-id proposal) (store/filing-case store))
        hard (hard-violations {:request request :proposal proposal} office-record c)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        over-threshold? (and (= :coordinate-supply-order (:op proposal))
                              office-record
                              (number? (:cost proposal))
                              (> (:cost proposal) (:max-supply-order-cost office-record)))
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?) (not over-threshold?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky? over-threshold?))}))
