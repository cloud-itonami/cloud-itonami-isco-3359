# Operator Guide

## First Deployment

1. Define the office's filing intake and case-registration process.
2. Register the office record and its procurement cost ceiling.
3. Register cases as they open; never accept documentation proposals against
   an unregistered case.
4. Run synthetic operating cases before connecting to real filings.
5. Enable human-reviewed sign-off for `:flag-compliance-concern` and
   above-threshold supply orders (already always-escalate by default —
   confirm no local override weakens this).
6. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- office/case provenance log
- compliance-concern escalation path (always to a human, never automated)
- provenance for all operating records
- human review for every flagged compliance concern
- audit export for all gated actions

## What this operator role explicitly does NOT do

- issue compliance rulings
- impose regulatory penalties
- order enforcement actions

These are structurally absent from the op-allowlist (see the README's
"No-enforcement-authority guarantee"), not merely withheld by policy. An
operator who wants an enforcement-capable system needs a different actor
with a human regulatory official directly in the authorizing loop for every
such decision — that authority is never delegated to this actor or its
governor.

## Certification

Certified operators must prove that the governor gates every
documentation/scheduling/procurement proposal, that every compliance
concern escalates to a human, and that no enforcement-shaped op has been
added to the allowlist.
