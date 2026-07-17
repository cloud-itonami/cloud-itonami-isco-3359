# Contributing

`cloud-itonami-isco-3359` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
clojure -M:test
```

Keep changes small and include tests for policy, audit, store or escalation
behavior.

## Rules

- Do not commit real office, case or filing data, credentials or operating
  documents.
- Keep production writes and escalations behind RegFiling Governor.
- **Never add an op resembling issuing a compliance ruling, imposing a
  regulatory penalty, or ordering an enforcement action to
  `regfiling.governor/closed-op-allowlist` or anywhere else in this actor.**
  This repository's entire design premise is that no such op exists; a PR
  proposing one is out of scope for this actor by definition and should be
  redirected to a design with a human regulatory official directly in the
  authorizing loop.
- Treat this occupation's workflows as high-risk: add tests for permission,
  provenance, escalation and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
