# Security Policy

Do not open a public issue for an exploitable vulnerability or disclose credentials in an issue. Use GitHub's private vulnerability reporting feature when available; otherwise contact the repository maintainer privately.

## Baseline
- Never commit API keys, signing keys, production database credentials, or keystores.
- Keep signing credentials in protected GitHub Actions secrets/environments.
- Never deploy from pull-request workflows.
- Backend container runs as a non-root user.
- Error responses must not expose stack traces, SQL, filesystem paths, or environment values.
- Review dependency alerts and geographic dataset licensing before releases.
- Configure gateway rate limits, TLS, timeouts, and request/response-size limits before production exposure.
