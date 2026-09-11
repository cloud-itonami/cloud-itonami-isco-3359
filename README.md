# cloud-itonami-isco-3359

Open Occupation Blueprint for **ISCO-08 3359**: Government Regulatory
Associate Professionals Not Elsewhere Classified.

This repository designs a forkable OSS business for a regulatory-filing
coordination practice: a documentation and logistics-coordination robot
manages inspection/filing-status records, review-appointment scheduling and
office/inspection-equipment procurement under a governor-gated actor, so a
government regulatory office keeps its own operating records instead of
renting a closed case-management SaaS.

**This actor has NO regulatory-enforcement or compliance-ruling authority.**
ISCO-08 3359 is a catch-all class for government officials not covered by the
more specific 335x classes (customs, tax, benefits, licensing, police), and it
broadly covers officials who enforce regulations, issue compliance rulings and
impose penalties. This actor deliberately implements none of that: issuing a
compliance ruling, imposing a regulatory penalty, and ordering an enforcement
action are not merely gated behind escalation — **no such operation exists
anywhere in this actor's closed op-allowlist**
(`regfiling.governor/closed-op-allowlist`). The only op that touches a
potential compliance action is `:flag-compliance-concern`, which always
escalates immediately to a human regulatory official and never resolves
automatically. See "No-enforcement-authority guarantee" below.

**Maturity: `:implemented`.** `src/regfiling/` implements the
`RegFilingActor` as a `langgraph.graph/state-graph`
(`regfiling.actor`) wired to a `Regulatory Filing Advisor`
(`regfiling.advisor`) and an independent `RegFiling Governor`
(`regfiling.governor`), following the itonami actor pattern
(ADR-2607011000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok? true) +-> :request-approval (:escalate? true, human-in-the-loop
interrupt) +-> :hold (:hard? true)`. 19 tests / 45 assertions green
(`kbb -M:test`).

HARD invariants (always hold, never overridable): office provenance (the
government office/regulatory unit must be registered), no-actuation
(`:effect` must be `:propose`), a registered case basis for any
documentation proposal (inspection log, review appointment, compliance
flag), **a closed op-allowlist that structurally excludes any op resembling
issuing a compliance ruling, imposing a regulatory penalty, or ordering an
enforcement action** (`:disallowed-op`), and a defense-in-depth free-text
scan for finalization/execution phrases like "issue the compliance ruling"
(`:scope-exclusion-hit`). Always-escalate: `:flag-compliance-concern` (any
observation that might warrant regulatory action is surfaced only this way,
always to a human, never auto-committed), supply-order proposals above the
office's registered cost threshold, and low confidence.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical/administrative domain work**. Here a regulatory-filing
documentation and logistics-coordination robot performs case-file data entry,
appointment scheduling and office-supply coordination under an actor that
proposes actions and an independent **RegFiling Governor** that gates them.
The governor never dispatches hardware itself and never finalizes any
regulatory decision; `:high`/`:safety-critical` actions (any compliance
concern, or a supply order above the office's registered cost threshold)
require human sign-off. The robot never issues a compliance ruling, imposes a
regulatory penalty, or orders an enforcement action — it is a
documentation/logistics-coordination robot only.

## No-enforcement-authority guarantee

This is the load-bearing design decision of this repository, so it is stated
plainly and mirrored in code, tests and the ADR that registers this repo:

- The proposal op-allowlist (`regfiling.governor/closed-op-allowlist`) has
  exactly four members: `:log-inspection-record`,
  `:schedule-review-appointment`, `:flag-compliance-concern`,
  `:coordinate-supply-order`. None of them issue a ruling, impose a penalty,
  or order enforcement — they are documentation, scheduling, human-escalation
  and procurement-coordination ops only.
- This is enforced **structurally**, not just behaviorally: the governor's
  `:disallowed-op` hard rule rejects any op outside that set, permanently and
  without override, whether it comes from the mock advisor, a future LLM
  advisor, or a caller bypassing the advisor entirely
  (`regfiling.governor-test/hard-on-disallowed-op-issue-compliance-ruling` and
  siblings exercise this against `:issue-compliance-ruling`,
  `:impose-regulatory-penalty` and `:order-enforcement-action`).
- A second, independent layer (`:scope-exclusion-hit`) scans every free-text
  field of a proposal for finalization/execution action phrases ("issue the
  compliance ruling", "impose the regulatory penalty", "order the enforcement
  action") as defense in depth, in case a future op is ever added whose
  underlying text drifts toward enforcement language. These are phrased as
  ACTION phrases (verb + object), never bare nouns like "ruling" or
  "penalty" — a bare-noun scan would false-trip on entirely legitimate
  `:flag-compliance-concern` rationale text that merely observes a ruling or
  penalty *might eventually be warranted* by a human. See
  `regfiling.advisor`'s mock rationale and
  `regfiling.governor-test/default-mock-advisor-proposals-never-self-trip`.
- Any observation the robot logs that suggests a regulatory action might be
  warranted is surfaced ONLY via `:flag-compliance-concern`, which always
  escalates immediately (`regfiling.governor-test/always-escalates-flag-compliance-concern-even-at-high-confidence`)
  for a human regulatory official to review and act on.

## Core Contract

```text
office filing intake + case register + procurement policy
        |
        v
Regulatory Filing Advisor -> RegFiling Governor -> log record/schedule/order, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
issue a compliance ruling, impose a regulatory penalty, order an
enforcement action, or suppress an operating record.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3359`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
