# Governance

`cloud-itonami-isco-3359` is an OSS open-occupation blueprint. Governance covers
both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions, disclose records, or
  finalize any regulatory decision.
- RegFiling Governor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- the op-allowlist never grows to include anything resembling issuing a
  compliance ruling, imposing a regulatory penalty, or ordering an
  enforcement action — this is a permanent scope boundary of this actor,
  not a temporary policy setting.
- every commit, hold and approval path is auditable.
- real office/case/filing data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification, license,
or the op-allowlist scope boundary should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and data-flow
review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling office/case/filing data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- adding any op resembling a compliance ruling, penalty, or enforcement
  action to a deployed fork
