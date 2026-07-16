# Playwright E2E

This package exercises the real Spring Boot API and Flutter web application. It
does not mock either side of the integration.

## Prerequisites

- Node.js 22 or newer
- Docker with Docker Compose v2
- A JDK supported by the backend
- Flutter with Web support enabled

The runner expects these project files relative to this directory:

- `../compose.yaml`, with a service named `postgres`
- `../backend/pom.xml` and the Maven wrapper in `../backend`
- `../pubspec.yaml`

## Install

```shell
cd e2e
npm ci
npx playwright install chromium
```

Use `npx playwright install --with-deps chromium` on a Linux CI runner that does
not already have the browser system dependencies.

## Run

```shell
npm run test:phase-01
npm run test:phase-02
npm run test:phase-03
npm run test:phase-04
npm run test:phase-05
npm run test:phase-06
npm run test:phase-07
npm run test:phase-08
npm run test:phase-09
npm test
```

Phase 2 covers the seeded student login/logout flow, refresh-token rotation and
replay defense, and the complete OTP password-reset flow. The test restores the
seed password before it finishes, with an API cleanup hook as a failure
safeguard.

Phase 3 verifies the authenticated Homepage API contract and the Flutter
dashboard rendered from that real response, including a navigation hand-off to
the semester-grade route.

Phase 4 verifies the configured 45-minute Vietnamese secondary-school
timetable, including week/day changes and schedule exceptions.

Phase 5 verifies the authenticated semester-grade contract, the Circular
22/2021 weighted numeric average, remark-subject outcomes, term switching, and
the Flutter assessment-detail flow.

Phase 6 verifies student-visible event filtering, category selection, event
detail deep links, registration and cancellation, and capacity/deadline action
rules.

Phase 7 verifies JWT-scoped student forms, status filtering, peer-form privacy,
form creation, cancellation, timeline updates, and the complete Flutter flow.

Phase 8 verifies grade-scoped club discovery, category filtering, membership
and capacity states, application/withdrawal/reactivation, and the complete
Flutter flow.

Phase 9 verifies the authenticated assistant, rejects cross-student identifier
injection, and asks a real timetable question through the Flutter chat screen.

Each `npm run test...` command invokes one lifecycle runner which performs the
following steps:

1. Validates the loopback URLs and Compose-project ownership, then starts and
   waits for the root Compose `postgres` service in the isolated
   `myschool-e2e-<pid>` Compose project. Each run receives a fresh project,
   volume, and host port separate from the development database.
2. Launches the backend Maven wrapper with the `e2e` Spring profile on
   `127.0.0.1:8080`.
3. Waits for backend health, builds Flutter Web with the backend URL as
   `API_BASE_URL`, then serves the
   completed build on `127.0.0.1:4173`.
4. Starts Playwright only after both applications are ready, then calls the real
   `/api/v1/system/health` endpoint and exercises Login to Reset
   Password through Flutter's accessibility tree.
5. Stops the backend and Flutter server and removes the owned Compose resources
   in a `finally` block, including when backend startup, Flutter build, or a test
   fails.

The runner deliberately removes inherited Maven and Java option variables such
as `MAVEN_ARGS`, `MAVEN_OPTS`, `JAVA_TOOL_OPTIONS`, and
`JDK_JAVA_OPTIONS` before starting Spring Boot. This prevents external Maven
properties from redirecting the isolated E2E datasource. Verify this contract
with `npm run verify:backend-environment`.

Playwright starts fresh backend and Flutter processes by default. Set
`E2E_REUSE_SERVERS=true` only when deliberately testing loopback servers you
started. Reuse mode never creates or removes database resources. Use the npm
scripts for test runs; `playwright test` by itself intentionally does not start
application infrastructure.

Other useful commands:

```shell
npm run db:up
npm run typecheck
npm run verify:backend-environment
npm run test:headed
npm run test:ui
```

The default test lifecycle removes the isolated container and volume after the
suite. Set `E2E_KEEP_DATABASE=true` only while intentionally debugging data.
Partial PostgreSQL startup is always rolled back, even when this debugging flag
is enabled.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `E2E_API_BASE_URL` | `http://127.0.0.1:8080` | Backend URL used by the config and API test |
| `E2E_WEB_BASE_URL` | `http://127.0.0.1:4173` | Flutter URL used as Playwright `baseURL` |
| `E2E_REUSE_SERVERS` | `false` | Set to `true` to reuse deliberately pre-started servers |
| `E2E_BACKEND_START_TIMEOUT_MS` | `180000` | Backend startup timeout |
| `E2E_FLUTTER_START_TIMEOUT_MS` | `180000` | Flutter startup timeout |
| `E2E_COMPOSE_WAIT_TIMEOUT_SECONDS` | `120` | PostgreSQL readiness timeout |
| `E2E_COMPOSE_PROJECT_NAME` | `myschool-e2e-<pid>` | Optional isolated Compose project and volume prefix override |
| `E2E_KEEP_DATABASE` | `false` | Preserve the isolated E2E database after a run for debugging |
| `E2E_POSTGRES_PORT` | Docker-assigned dynamic port | Explicit isolated host PostgreSQL port override; `0` asks Docker to allocate |
| `DB_NAME` | `myschool` | Compose database name |
| `DB_USERNAME` | `myschool` | Compose and backend database user |
| `DB_PASSWORD` | `myschool` | Compose and backend database password |
| `POSTGRES_PORT` | not set | Optional fallback when `E2E_POSTGRES_PORT` is absent |
| `DB_URL` | ignored by E2E | Generic datasource variables cannot redirect E2E away from its isolated database |

## Selector policy

All interactive application controls use accessible roles and names centralized
in `tests/support/flutter-semantics.ts`. Static assertions may use
`getByText` only for Flutter's semantic text output when its engine exposes a
non-interactive `Semantics` label as a text node rather than an ARIA label.
The only CSS selector is Flutter's bootstrap `flt-semantics-placeholder`, which
is clicked once to enable the accessibility tree when necessary. Tests must not
use coordinates, generated Flutter DOM nodes, or visual-layout selectors.
