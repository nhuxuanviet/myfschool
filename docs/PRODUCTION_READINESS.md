# Production readiness

This document is the release gate for the Spring Boot API, Flutter student web,
and React Admin web. A release is production-ready only when every automated
gate passes and the environment-specific operational checks are signed off.

## Implemented controls

- Spring profile isolation prevents `prod` from running with `dev`, `e2e`, or
  `test` fixtures.
- PostgreSQL schema is managed exclusively by Flyway and Hibernate validates it
  at startup.
- Student and Admin authorization are role-separated. Admin refresh tokens are
  rotated in HttpOnly cookies and protected by a CSRF token.
- Login bootstrap uses operation ordering so a stale refresh response cannot
  clear a newly authenticated Admin session.
- API and web responses set frame, content-type, referrer, permissions, and CSP
  headers. Production Admin cookies are `Secure`.
- AI tools are read-only and always bind the authenticated student on the
  server. Runtime settings use optimistic locking and every change is audited.
- The OpenAI credential is read only from the backend environment. The API and
  Admin web expose only a boolean readiness flag, never the key.
- AI conversation memory and rate limits are stored in PostgreSQL, so restarts
  and multiple backend instances behave consistently.
- Container images run the application as non-root users. PostgreSQL and the API
  are not published directly by the production Compose topology.

## Required release environment

1. Copy `.env.production.example` outside the repository and source values from
   the deployment secret manager. Never commit the populated file.
2. Generate independent high-entropy values for `DB_PASSWORD`, `JWT_SECRET`,
   and `OTP_PEPPER`. Rotate any credential that has appeared in chat, logs, a
   terminal transcript, or source control.
3. Put a TLS reverse proxy or managed load balancer in front of ports 8081 and
   8082. Redirect HTTP to HTTPS and preserve `X-Forwarded-Proto`.
4. Configure `CORS_ALLOWED_ORIGINS` with exact HTTPS origins; wildcards are not
   permitted with credentialed Admin requests.
5. Configure and verify the SMS gateway sender in its production account.
6. Configure a rotated `OPENAI_API_KEY`, keep `ASSISTANT_PROVIDER=openai`, then
   confirm the Admin “Cấu hình AI” page reports `Sẵn sàng`.

## Build and start

```powershell
docker compose --env-file C:\secure\myschool.production.env `
  -f compose.production.yml build --pull
docker compose --env-file C:\secure\myschool.production.env `
  -f compose.production.yml up -d
docker compose --env-file C:\secure\myschool.production.env `
  -f compose.production.yml ps
```

The example Compose file binds both web containers to loopback intentionally.
The TLS proxy should be the only public ingress. The student build-time
`STUDENT_PUBLIC_URL` must match its final HTTPS origin because Flutter validates
release API URLs.

## Release gates

```powershell
Set-Location backend
.\mvnw.cmd test

Set-Location ..
flutter analyze
flutter test

Set-Location admin-web
npm ci
npm run lint
npm run test
npm run build
npm run test:e2e

Set-Location ..\e2e
npm ci
npm test
```

Also run `docker compose --env-file .env.production.example -f
compose.production.yml config --quiet` and scan the repository for credentials
before creating a release artifact.

## Operational sign-off

- Restore the latest encrypted PostgreSQL backup into a clean environment and
  record the recovery time and recovery point.
- Configure alerts for API health, 5xx rate, p95 latency, authentication
  failures, SMS delivery failures, database saturation, and AI 429/503 rates.
- Define log retention and redact Authorization, cookies, OTP values, phone
  numbers, and provider payloads at the collector.
- Run a student/Admin authorization smoke test using non-production accounts.
- Confirm the school privacy policy defines AI data handling and conversation
  retention. Current messages are bounded per conversation by the Admin setting.
- Roll out with a reversible database backup and a documented image rollback.

## Remaining external go-live decisions

These cannot be completed from source code alone: production domains and TLS,
secret-manager selection, SMS vendor approval, backup storage/retention,
monitoring destination, data-retention policy, and the first real Admin account.

## Latest local release evidence

Verified on 15 July 2026:

- Spring Boot: 145 tests passed; all 18 Flyway migrations were also applied
  successfully to a clean PostgreSQL database during E2E and container smoke
  testing.
- Flutter: static analysis passed and 137 unit/widget tests passed.
- React Admin: lint and production build passed; 10 unit tests and 16
  PostgreSQL-backed Playwright tests passed.
- Student application: 24 PostgreSQL-backed Playwright tests passed. The
  opt-in OpenAI smoke test was then run separately and passed through both the
  API and Flutter streaming UI. Deterministic authorization, multi-turn memory,
  and scrolling coverage also passed.
- Production Compose configuration validated, all three application images
  built, and the four-service topology reached `healthy`. Both web `/healthz`
  endpoints returned `ok`; the API returned `UP` through the Admin reverse
  proxy. The temporary smoke-test volume was removed afterward.
- The repository secret scan found no committed OpenAI key or populated
  production credential.

These checks validate the release artifact, not the external go-live items
listed above. TLS, managed secrets, SMS approval, monitoring, backup restore,
and data-retention sign-off remain deployment-environment responsibilities.
