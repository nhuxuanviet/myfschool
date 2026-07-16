# MySchool Backend

Spring Boot 4.1 modular-monolith foundation for the MySchool Flutter app.

## Requirements

- Java 21
- Docker with Compose

## Run locally

From the repository root:

```powershell
docker compose up -d postgres
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

The API is available at `http://localhost:8080`. Useful endpoints:

- `GET /actuator/health`
- `GET /api/v1/system/health`
- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

The local `dev` profile idempotently seeds two student fixtures:

- Grade 10 phone: `0912345678` / password: `123`
- Grade 11 peer phone: `0977777777` / password: `123`

The isolated `e2e` profile retains `Student@123` so the password-reset flow can
exercise the production password policy.

## Authentication API

All authentication requests and responses are JSON. Tokens are returned in the
response body; the backend does not set authentication cookies.

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/password-reset/request`
- `POST /api/v1/auth/password-reset/verify`
- `POST /api/v1/auth/password-reset/complete`

## Home API

`GET /api/v1/home` requires a student bearer token and derives the student
strictly from the JWT subject. It returns the current academic term, a stable
summary whose event and pending-form counters come from the enabled features,
and announcements filtered by grade audience and their visibility window.
`academicTerm` is `null` when no term is active, such as during a school break.

## Timetable API

`GET /api/v1/timetable` requires a student bearer token and derives the class
only from the JWT subject. `weekStart` is an optional ISO-8601 Monday; when it
is omitted, the backend derives the current school week in
`Asia/Ho_Chi_Minh`. The response always contains seven Monday-to-Sunday day
objects, uses `HH:mm` local times, and returns an ordered `academicTerms` list
with every term intersecting the requested week. The list is empty when the
week does not overlap a term.

Periods are configured per term and are enforced by PostgreSQL to be exactly
45 minutes. A lesson status is one of `SCHEDULED`, `CANCELLED`, `REPLACED`, or
`ADDED`; date-specific overrides retain the appropriate subject, teacher,
room, and explanatory note. The `dev` and `e2e` profiles seed a representative
THPT schedule, including a cancellation, a replacement lesson, and a Saturday
học bù in the current school week.

## Semester Grades API

`GET /api/v1/grades` requires a student bearer token and derives the student
strictly from the JWT subject. `termId` is an optional UUID that is resolved
only within that student's enrolled semesters; an unavailable term returns the
same 404 response regardless of whether it exists for another student.

Without `termId`, the API selects a term containing the current
`Asia/Ho_Chi_Minh` school date when the student has grade data there; otherwise
it selects the newest available term. `availableTerms` is returned newest
first. Each subject returns its assessment mode, required regular-assessment
count, calculated numeric average or remark result, and ordered assessment
details. Every assessment has a nonblank student-facing label; its duration,
score, outcome, and assessed date remain nullable when its workflow status
does not yet have a final result.

For numeric subjects, the backend follows Circular 22/2021: 35 annual lessons
require two regular assessments, 36--70 require three, and more than 70 require
four. It calculates an average only after the exact required number of regular
assessments plus one midterm and one final are finalized, using
`(sum(regular) + 2 * midterm + 3 * final) / (regularCount + 5)` with
`BigDecimal` half-up rounding to one decimal. Remark subjects require two
regular assessments, a midterm, and a final; they are `ACHIEVED` only when all
finalized outcomes are achieved, otherwise `NOT_ACHIEVED`, or `PENDING` while
incomplete. The `dev` and `e2e` profiles seed a current HK1 and a historical
HK2 with numeric, make-up, and remark examples.

## Events API

`GET /api/v1/events` returns school events visible to the authenticated
student's grade, never from a client-provided student id. It accepts optional
`category` (`ACADEMIC`, `CULTURAL`, `SPORTS`, `CLUB`, or `CAREER`) and
`includePast` (default `false`). The default list excludes completed events and
orders active/upcoming events by start time ascending. With `includePast=true`,
completed events follow them in reverse chronological order.

`GET /api/v1/events/{eventId}` returns the same student-specific event detail;
an event outside the student's grade audience returns the same `EVENT_NOT_FOUND`
404 as an unknown id. The event detail includes capacity, current active
registration count, deadlines, the current student's registration status, and
`canRegister`/`canCancel` action flags.

`POST /api/v1/events/{eventId}/registrations` creates a registration with 201,
or reactivates a previously cancelled one with 200. `DELETE` on the same nested
resource cancels an active registration and returns 200. Both return the event
detail directly. Registration is serialized with a PostgreSQL event-row lock,
so concurrent requests cannot exceed capacity. Typical conflicts are
`EVENT_ALREADY_REGISTERED`, `EVENT_CAPACITY_REACHED`,
`EVENT_REGISTRATION_CLOSED`, and `EVENT_CANCELLATION_CLOSED`.

The `dev` and `e2e` profiles seed an open cultural event, a registered
cancellable academic event, a full/closed sports event, a grade-11-only club
event, an expired-registration event, and a past career event.

## Student Forms API

`GET /api/v1/forms` lists only forms owned by the authenticated student and
accepts an optional `status` filter. `GET /api/v1/forms/{formId}` returns the
reason, optional leave-date range, current status, action flag, and an ordered
status timeline. Another student's identifier is indistinguishable from an
unknown form and returns `FORM_NOT_FOUND`.

`POST /api/v1/forms` submits one of `LEAVE_OF_ABSENCE`,
`STUDENT_CONFIRMATION`, `TRANSCRIPT_REQUEST`, or `STUDENT_CARD_REISSUE`. Leave
forms require an inclusive ISO date range; all other types reject date fields.
The student identity and initial `SUBMITTED` status always come from the JWT
and server workflow. `DELETE /api/v1/forms/{formId}` cancels an owned
`SUBMITTED` or `IN_REVIEW` form and appends `CANCELLED` to its timeline in the
same row-locked transaction. Approved, rejected, and already-cancelled forms
return `FORM_CANNOT_BE_CANCELLED`.

The `dev` and `e2e` profiles seed submitted, approved, and rejected examples
for the grade-10 student plus a peer-owned form for authorization-isolation
tests. The Home API counts only `SUBMITTED` and `IN_REVIEW` forms as pending.

## Clubs API

`GET /api/v1/clubs` lists clubs visible to the authenticated student's grade
and accepts an optional `category` filter (`ACADEMIC`, `SPORTS`, `ARTS`,
`SKILLS`, `COMMUNITY`, or `MEDIA`). `GET /api/v1/clubs/{clubId}` returns the
same scoped detail, including adviser, meeting schedule, location, capacity,
application deadline, current membership status, and server-computed action
flags. A grade-restricted or unknown club returns `CLUB_NOT_FOUND`.

`POST /api/v1/clubs/{clubId}/applications` creates a pending application with
201 or reactivates a withdrawn/rejected application with 200. `DELETE` on the
same nested resource withdraws only a pending application. Application checks
lock the club row before evaluating the deadline and active-member capacity.
The Home API counts only `ACTIVE` memberships.

The `dev` and `e2e` profiles seed open, active, pending, full-capacity, and
grade-restricted examples for action-state and authorization-isolation tests.

## Student Assistant API

`POST /api/v1/assistant/chat` accepts `{"message":"..."}` with 1–500
characters and returns a student-scoped answer plus the active reply mode. The
assistant can read the current timetable, semester grades, upcoming events,
owned forms, visible clubs, and visible announcements. Every data function is
bound to the JWT subject by the server and accepts no student identifier, so a
prompt cannot redirect a lookup to another account.

The `dev` profile uses Spring AI with OpenAI and requires `OPENAI_API_KEY` from
the environment; the key is never stored in source. The isolated `e2e` profile
keeps the deterministic `local` provider so automated tests do not depend on
an external service. `OPENAI_MODEL` defaults to `gpt-5.6-luna` and
`OPENAI_TEMPERATURE` defaults to `0.6`. Model tools return structured,
student-scoped facts rather than prewritten responses, leaving wording and
context-sensitive synthesis to the model. Model responses are not stored,
parallel tool calls are disabled, reasoning effort is `none` for Chat
Completions function-tool compatibility, and the response token budget is
capped with `max_completion_tokens`. An
upstream failure returns `ASSISTANT_UNAVAILABLE` with HTTP 503 without exposing
provider details or credentials.

Access tokens are JWT bearer tokens with a ten-minute lifetime. Refresh and
password-reset tokens are opaque; only SHA-256 hashes are persisted. Refresh
tokens rotate on every use, and replay revokes the entire token family.

Password-reset challenge creation is protected by an atomic PostgreSQL fixed-window
limit per hashed phone number. The defaults are three requests per 15 minutes;
override them with `PASSWORD_RESET_MAX_REQUESTS` and
`PASSWORD_RESET_RATE_LIMIT_WINDOW`.

The `e2e` profile alone uses OTP `123456`. Other profiles generate random OTPs
through `OtpGenerator`. Local profiles use a no-op delivery adapter without
logging OTP values. The `prod` profile sends OTPs to a configured HTTPS SMS
gateway through `OtpDeliveryPort`.

PostgreSQL is exposed on host port `5433` by default to avoid clashing with a
locally installed database. Database settings can be overridden with `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD`. Compose additionally accepts `DB_NAME` and `POSTGRES_PORT`; set
`DB_URL` to match when changing either. The Compose service and backend share
the same username/password variables. CORS origins can be overridden with
`CORS_ALLOWED_ORIGINS`.

## Test

Docker must be running because integration tests use PostgreSQL through
Testcontainers.

```powershell
.\mvnw.cmd test
```

The `test` and `e2e` profiles keep Flyway enabled and Hibernate schema
validation active, so tests use the same migration path as production.

The `prod` profile requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and
`CORS_ALLOWED_ORIGINS`. It also requires `JWT_SECRET` and `OTP_PEPPER`, each
containing at least 32 bytes, plus `SMS_GATEWAY_URL` and `SMS_GATEWAY_API_KEY`.
SMS calls default to a three-second connect timeout and five-second read timeout;
override them with `SMS_CONNECT_TIMEOUT` and `SMS_READ_TIMEOUT`.
It disables OpenAPI/Swagger unless
`OPENAPI_ENABLED=true` is explicitly set.

For safety, `prod` cannot be activated together with `dev`, `e2e`, or `test`;
startup fails before the application context is created.

## Package convention

Business capabilities live in top-level packages under
`vn.edu.fpt.myschool`. Shared technical concerns belong in `shared`; they must
not contain business rules. Each future capability should own its API,
application, domain, and infrastructure packages without directly reaching
into another capability's internals.
