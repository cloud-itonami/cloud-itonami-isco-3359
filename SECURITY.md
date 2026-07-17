# Security Policy

This project handles government regulatory associate professionals operating
workflows. Treat vulnerabilities as potentially high impact even when the
demo data is synthetic.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real office, case or filing data exposure
- authorization bypass
- RegFiling Governor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- any code path that would let this actor issue a compliance ruling, impose
  a regulatory penalty, or order an enforcement action — this is treated as
  a critical-severity design breach, not a routine bug, because it violates
  this repository's entire scope boundary

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
gftdcojp organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on office/case data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real office/case/filing data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
