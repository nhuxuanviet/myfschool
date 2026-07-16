# My FSchool

Student-facing Flutter application backed by a Spring Boot modular monolith and
PostgreSQL. Delivery is tracked in [the implementation roadmap](docs/IMPLEMENTATION_ROADMAP.md).

## Requirements

- Flutter 3.41 or newer with Dart 3.11
- Java 21
- Docker Desktop with Compose v2
- Node.js 22 or newer for Playwright

## Run locally

Start PostgreSQL and the backend:

```powershell
docker compose up -d postgres
Set-Location backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

The development database is exposed on `127.0.0.1:5433`. The API and Swagger
UI are available at `http://localhost:8080` and
`http://localhost:8080/swagger-ui.html`.

In another terminal, run Flutter Web:

```powershell
flutter run -d chrome --web-hostname 127.0.0.1 --web-port 4173 --dart-define=API_BASE_URL=http://127.0.0.1:8080
```

Run the separate Admin Web in another terminal:

```powershell
Set-Location admin-web
npm install
npm run dev
```

Admin Web is available at `http://127.0.0.1:4174`. The `dev` profile seeds
`0900000000` / `Admin@123` exclusively for local Admin testing. Admin accounts
use a role-isolated API and a secure browser session; the production profile
does not seed an Admin account.

For an Android emulator, the debug default is `http://10.0.2.2:8080`. Release
builds require an explicit HTTPS `API_BASE_URL` dart define.

The `dev` profile seeds a primary student account for local demonstration:

- Phone: `0912345678`
- Password: `123`

A grade-11 peer fixture (`0977777777` / `123`) exists only to verify grade- and
ownership-scoped APIs during development. The isolated E2E profile retains its
own strong test password for password-reset coverage.

Password recovery uses a real SMS gateway in `prod`. The deterministic OTP
`123456` exists only in the isolated `e2e` profile and is never enabled by the
development or production profiles.

## Verify

```powershell
Set-Location backend
.\mvnw.cmd test

Set-Location ..
flutter analyze
flutter test

Set-Location admin-web
npm run lint
npm run test
npm run build
npm run test:e2e

Set-Location e2e
npm ci
npm run install:browsers
npm test
```

Playwright builds Flutter Web, starts the real backend against an isolated
PostgreSQL Compose project, runs all phase regression specs, and removes the
isolated database afterward.

## Production deployment

Production container definitions, required secrets, security controls, release
gates, backup checks, and operational sign-off are documented in
[`docs/PRODUCTION_READINESS.md`](docs/PRODUCTION_READINESS.md). Do not deploy
from the local `compose.yaml`; it is intentionally development-only.
