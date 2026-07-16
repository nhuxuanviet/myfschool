# My FSchool implementation roadmap

This roadmap defines the completed delivery order for the original student
application. The next multi-role mobile milestone is specified in
[`MOBILE_ROLES_ROADMAP.md`](MOBILE_ROLES_ROADMAP.md). A phase is complete only
after its database migrations, Spring Boot APIs, Flutter user flow, automated
tests, and phase-specific Playwright end-to-end tests pass.

## Delivery rules

- Build vertical slices in phase order; do not start a dependent phase while the
  previous phase is failing.
- Keep the backend as a modular monolith with package-by-feature boundaries.
- Keep Flutter feature-first and access backend data through repositories.
- Derive the current student from the authenticated principal; never trust a
  client-provided student identifier for ownership.
- Use Flyway as the only schema migration mechanism and validate Hibernate mappings.
- Exercise PostgreSQL in integration and end-to-end tests; do not substitute H2.
- Add one Playwright spec per phase and retain all earlier specs as regression tests.

## Phases

| Phase | Scope | Playwright acceptance |
| --- | --- | --- |
| 1 | Spring Boot/PostgreSQL foundation, Flutter architecture, E2E harness | API health is UP and Login/Reset screens navigate in Flutter Web |
| 2 | Login, JWT session, logout, and password reset | Student logs in, restores/ends a session, and completes the reset flow |
| 3 | App shell, student profile, and live Homepage | Authenticated student sees their dashboard and navigates the shell |
| 4 | Vietnamese secondary-school timetable | Student changes week/day and sees configured 45-minute periods and exceptions |
| 5 | Semester grades under Circular 22/2021 | Student switches semester and opens assessment details with correct averages |
| 6 | Events and announcements | Student filters events, opens details, registers, and cancels where allowed |
| 7 | Forms and approval workflow | Student creates a request and sees its status timeline |
| 8 | Clubs and membership | Student browses clubs, applies, and sees membership state |
| 9 | Spring AI assistant | Authenticated chat answers from authorized schedule/grade/event tools only |
| 10 | Release hardening | Full cross-feature happy path, authorization isolation, and production smoke test |

## Original milestone assumptions

- The original Flutter milestone was student-facing. Teacher and parent mobile
  roles are now part of the separate R1-R6 milestone so the completed Student
  release remains regression-protected while authorization boundaries expand.
- Login uses a Vietnamese phone number. Password reset uses an SMS OTP provider
  abstraction with a development-only local implementation.
- A regular lesson is 45 minutes, while concrete start times and breaks are school
  and academic-year configuration.
- Grade calculation stores `REGULAR`, `MIDTERM`, and `FINAL` categories. Labels such
  as oral, 15-minute, or 45-minute identify the assessment method and do not define
  separate hard-coded coefficients.
- Spring AI is optional at runtime. No provider credential is stored in Flutter or
  committed to source control.
