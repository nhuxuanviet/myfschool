# My FSchool Admin Web implementation plan

**Status:** Complete — Phases A1-A8 implemented and verified  
**Created:** 2026-07-14  
**Delivery model:** One phase at a time; every phase must pass backend integration tests, frontend tests, and Playwright E2E before the next phase starts.

## 1. Architecture decision

Create a separate `admin-web/` frontend in the current repository. Keep the existing Spring Boot modular monolith and PostgreSQL database as the single backend and source of truth.

```text
myfschoolse1913/
├── admin-web/                 # React + TypeScript + Material UI
├── backend/                   # Existing Spring Boot modular monolith
├── lib/                       # Existing Flutter student application
├── e2e/                       # Existing student/API Playwright suite
└── docs/
```

Do not add Admin routes to the student Flutter application. Do not create a second backend or duplicate the database.

### Admin frontend stack

- React, TypeScript in strict mode, and Vite.
- Material UI and the community MUI X Data Grid for desktop-first management screens.
- React Router for route composition and guards.
- TanStack Query for server state, cache invalidation, and mutations.
- React Hook Form plus Zod for typed forms and client-side validation.
- OpenAPI-generated API types so frontend contracts cannot silently drift from Spring Boot.
- Vitest and Testing Library for component and application tests.
- Playwright for mandatory E2E coverage.

Pin all installed dependency versions in `package-lock.json`. Do not depend on floating versions.

### Runtime topology

| Component | Development origin | Purpose |
| --- | --- | --- |
| Student Flutter Web | `http://127.0.0.1:4173` | Existing student application |
| Admin Web | `http://127.0.0.1:4174` | New management site |
| Spring Boot API | `http://127.0.0.1:8080` | Shared student and admin APIs |
| PostgreSQL | `127.0.0.1:5433` | Shared database through Docker Compose |

In production, serve Admin Web and `/api` behind one HTTPS reverse proxy origin. The Vite development server proxies `/api` to Spring Boot.

## 2. Scope and product assumptions

Admin Web v1 supports one privileged role, `ADMIN`. Fine-grained staff and teacher roles are deferred until the complete Admin workflow is stable. The authorization model must be extensible so `TEACHER`, `ACADEMIC_STAFF`, and `STUDENT_AFFAIRS` can be introduced without changing API contracts.

Admin Web v1 manages the data already consumed by the student application:

- Dashboard and operational summaries.
- Academic years, terms, subjects, classes, and students.
- Timetable templates and date-specific timetable changes.
- Semester grade configuration and assessment results.
- Student forms and approval history.
- Announcements and student notifications.
- Events, registrations, clubs, and memberships.
- Audit history for privileged mutations.

Out of scope for v1:

- Tuition payment processing.
- Parent accounts.
- Teacher self-service and teacher-class assignment UI.
- Bulk integration with an external school information system.
- Production SSO/MFA. The authentication design must leave room for both.
- AI generation of grades, approvals, or other authoritative school records.

## 3. Backend rules for Admin APIs

- Student endpoints remain backward compatible and student-scoped.
- Admin endpoints use `/api/v1/admin/**` and require `ROLE_ADMIN` at both route and service boundaries.
- Never trust an actor ID, role, or school scope supplied by the browser; derive the actor from the authenticated principal.
- List APIs use server-side pagination, sorting, and allow-listed filters.
- Mutations validate business rules in application services, not controllers.
- Conflicting concurrent edits return HTTP `409` using an explicit record version.
- Validation and domain errors continue to use the existing Problem Details format.
- Flyway is the only schema migration mechanism.
- Every privileged mutation writes an audit event in the same database transaction.
- Audit records contain actor, action, entity type, entity ID, timestamp, and changed fields. Secrets, password hashes, refresh tokens, OTP values, and AI keys are never recorded.
- Admin APIs must never expose student password hashes, reset challenges, refresh-token hashes, or internal security data.

## 4. Authentication design prerequisite

The current backend assumes every user is a student:

- `UserRole` contains only `STUDENT`.
- `UserAccount` requires a non-null `StudentProfile`.
- `JpaUserAccountStore` fails when a user has no student row.
- `JwtAccessTokenIssuer` always writes `studentId`.
- `AuthTokenResponse` always returns a student object.

Phase 1 must remove that assumption without breaking the existing Flutter login response. Student responses continue to include the existing `student` object. Admin authentication returns an admin account object and no fake student identity.

Admin browser security:

- Keep the short-lived access token in memory only.
- Store the admin refresh token in a `Secure`, `HttpOnly`, `SameSite=Strict` cookie.
- Protect refresh and logout requests with an anti-CSRF token.
- Do not store admin tokens in `localStorage` or `sessionStorage`.
- Revoke refresh-token families on logout, password change, account disablement, and refresh-token reuse.
- Use a separate admin login rate limit and record security events without logging credentials.

## 5. Delivery phases

| Phase | Scope | Depends on | Mandatory Playwright outcome |
| --- | --- | --- | --- |
| A1 | Admin identity, RBAC, secure session, project scaffold | Existing backend | Admin logs in and logs out; student and anonymous users cannot open Admin routes |
| A2 | Shared Admin shell and dashboard | A1 | Admin sees live metrics and every navigation item resolves to a valid route |
| A3 | Academic catalog, classes, and student directory | A2 | Admin creates/edits records and sees server-paginated results survive reload |
| A4 | Timetable management | A3 | Admin changes a lesson/override and the student Flutter schedule reflects it |
| A5 | Grade management | A3 | Admin records assessments and the student Flutter grade screen shows recalculated results |
| A6 | Forms, announcements, and notifications | A3 | Admin approves a form/publishes an announcement and the student sees the new state |
| A7 | Events and clubs | A3 | Admin manages an activity and student registration/membership behavior follows the change |
| A8 | Audit, exports, accessibility, and release hardening | A1-A7 | Full Admin and student cross-application regression suite passes |

## 6. Detailed phase plans

### Phase A1 — Admin identity, authorization, and frontend foundation

**Goal:** Establish a secure Admin session and a deployable, tested Admin Web foundation without changing student behavior.

**Implementation status (2026-07-14):** Complete. The secure ADMIN identity,
cookie/CSRF session, role isolation, React/MUI scaffold, protected shell, unit
tests, PostgreSQL integration tests, and Playwright A1 flow are implemented.

Backend deliverables:

- Add Flyway migration for `ADMIN` role support and an `admin_profiles` table.
- Refactor authentication domain models so a user has a role-appropriate profile instead of an unconditional student profile.
- Preserve the current student login and refresh JSON contract.
- Add Admin login, refresh, logout, and current-account endpoints under `/api/v1/admin/auth/**`.
- Add `ROLE_ADMIN` route protection and method-level authorization for all Admin services.
- Add development/e2e Admin seeding through environment-backed configuration; never enable automatic Admin seeding in production.
- Add Admin authentication rate limiting, CSRF protection, secure cookie settings, and security audit events.
- Add PostgreSQL integration tests for login, refresh rotation, reuse detection, logout, disabled accounts, student/admin isolation, CSRF rejection, and missing profiles.

Admin Web deliverables:

- Scaffold `admin-web/` with React, strict TypeScript, Vite, MUI, routing, query client, test setup, ESLint, and formatting.
- Create theme tokens matching the FSchool orange identity while using desktop Admin density.
- Implement login, session bootstrap, protected routes, access-denied, not-found, offline, and session-expired states.
- Implement an API client with one refresh attempt, request cancellation, typed Problem Details handling, and no persistent token storage.
- Add responsive minimum support at 1280×720 and usable fallback at 1024×768.

Playwright spec: `admin-web/e2e/phase-a1-auth.spec.ts`

- Log in with the seeded Admin account.
- Reload and restore the session through the secure refresh cookie.
- Log out and verify protected history cannot be reopened.
- Verify student credentials receive a generic authorization failure on Admin login.
- Verify an anonymous request to `/api/v1/admin/**` is `401` and a student token is `403`.
- Assert there are no uncaught browser exceptions or console errors.

Exit gate:

- Existing student Flutter authentication E2E still passes unchanged.
- Backend integration tests and Admin Web unit tests pass.
- `npm run build`, `npm run lint`, and TypeScript checks pass.
- Playwright A1 passes against PostgreSQL.

### Phase A2 — Admin shell and dashboard

**Goal:** Deliver the common Admin experience and live operational overview used by later modules.

**Implementation status (2026-07-14):** Complete. The ADMIN-only dashboard
projection, bounded and indexed PostgreSQL queries, live metrics, recent audit
activity, responsive shared shell, complete navigation map, loading/error/retry
states, unit tests, integration tests, and Playwright A1-A2 flows are implemented.

Backend deliverables:

- Add `/api/v1/admin/dashboard` with counts for students, classes, pending forms, upcoming events, pending club applications, and recently updated grades.
- Add a recent-activity projection sourced from audit events.
- Ensure dashboard queries are bounded and indexed; do not load entire tables to calculate counts.

Admin Web deliverables:

- Desktop App Bar, collapsible sidebar, breadcrumb, page title region, account menu, and logout action.
- Navigation entries for Dashboard, Học sinh, Học vụ, Lịch học, Điểm, Đơn từ, Thông báo, Sự kiện, CLB, and Nhật ký.
- Dashboard metric cards, pending-work queue, recent activity, skeleton, empty, error, and retry states.
- Shared page container, confirmation dialog, toast, loading overlay, error boundary, and unsaved-change guard.
- Keyboard-visible focus, skip navigation, semantic landmarks, and WCAG AA color contrast.

Playwright spec: `admin-web/e2e/phase-a2-shell-dashboard.spec.ts`

- Verify all metric values come from the API fixture.
- Open every sidebar route and confirm a valid page heading and breadcrumb.
- Collapse/expand navigation and verify focus and keyboard navigation.
- Simulate dashboard API failure and verify retry recovery.

Exit gate:

- Shared components have unit and accessibility tests.
- Dashboard API integration tests prove ADMIN-only access.
- Playwright A1-A2 pass.

### Phase A3 — Academic catalog, classes, and student directory

**Goal:** Establish authoritative academic structures and student management before timetable and grade editing.

**Implementation status (2026-07-14):** Complete. Academic years, terms,
subjects, normalized classes, and students now have ADMIN-only APIs with
validation, optimistic locking, transactional audit events, protected deletion,
and PostgreSQL-backed pagination. The React management screens provide filters,
URL-persisted search, create/edit drawers and dialogs, conflict feedback, and
responsive layouts. Backend integration, frontend unit, production build, and
Playwright A1-A3 regression tests pass.

Backend deliverables:

- Normalize class identity with `school_classes` and student-class enrollment references while preserving student API output fields.
- Add Admin CRUD/search APIs for academic years, terms, subjects, classes, and students.
- Add server-side pagination with `page`, `size`, `sort`, allow-listed filter fields, stable secondary sort, and maximum page size.
- Enforce unique student code, valid Vietnamese phone number, grade 6-12, non-overlapping terms, and class/grade consistency.
- Refuse deletion of referenced academic records; support safe disable/archive states where required.
- Add optimistic-lock versions and audit entries.
- Add indexes driven by actual Admin filters: student code, normalized name, phone, grade, class, status, and term dates.

Admin Web deliverables:

- Student Data Grid with search, grade/class/status filters, server pagination, sorting, URL-persisted filter state, and empty/error/loading states.
- Create/edit/view student drawer or page with validated Vietnamese fields.
- Academic year, term, subject, and class management screens with confirmation and conflict feedback.
- Preserve user-entered form data after a validation response.
- Use stable row IDs and invalidate only affected query caches after mutation.

Playwright spec: `admin-web/e2e/phase-a3-students-academics.spec.ts`

- Create a class and student, find them with filters, edit the student, and verify data survives reload.
- Verify duplicate code/phone and invalid term dates produce field-level errors.
- Verify referenced records cannot be deleted and the UI explains why.
- Log into Flutter as the created/updated student and verify profile fields remain compatible.

Exit gate:

- Migration tests cover existing seeded data.
- API pagination and authorization integration tests pass with PostgreSQL.
- Playwright A1-A3 and existing student profile regression pass.

### Phase A4 — Timetable management

**Goal:** Allow Admins to manage Vietnamese secondary-school schedules while enforcing the existing 45-minute lesson contract.

**Implementation status (2026-07-15):** Complete. Admins can select a term and
class, manage the weekly 45-minute schedule, and create or remove date-specific
cancel, replace, and add overrides. Mutations use optimistic versions, audit
events, PostgreSQL constraints, and are verified against the student timetable
API by Playwright.

Backend deliverables:

- Add Admin APIs for period definitions, weekly class timetable entries, and date-specific `CANCELLED`, `REPLACED`, and `ADDED` overrides.
- Keep each period exactly 45 minutes and prevent overlapping slots, duplicate class slots, invalid term dates, and override/period mismatches.
- Add bulk validation endpoint for a draft timetable before saving.
- Use transactional bulk upsert with an explicit version; never partially apply a timetable.
- Write audit records for timetable changes, including the affected class/date/period.

Admin Web deliverables:

- Term and class selectors, weekly timetable grid, period editor, subject/teacher/room inputs, and conflict summary.
- Date-specific change workflow for cancel, replace, and add operations.
- Preview mode that renders the same day/period ordering the Flutter student app consumes.
- Clear unsaved, saving, saved, conflict, and stale-version states.

Playwright spec: `admin-web/e2e/phase-a4-timetable.spec.ts`

- Build a valid week, reject an overlapping/invalid 45-minute slot, and save the valid schedule.
- Add a date-specific replacement and cancellation.
- Log into Flutter and verify both changes on the matching date.
- Verify a stale Admin edit returns `409` and can be safely reloaded.

Exit gate:

- Existing timetable integration and Flutter E2E tests remain green.
- New Admin timetable tests cover validation, transaction rollback, and audit.
- Playwright A1-A4 pass.

### Phase A5 — Grade and assessment management

**Goal:** Let Admins enter authoritative assessment results while the backend remains the only calculator of semester outcomes.

**Implementation status (2026-07-15):** Complete. The Admin workflow assigns
subjects and records or updates numeric and remark assessments. Validation and
semester calculations remain in Spring Boot, while Playwright verifies the
same assessment through the student grade API.

Backend deliverables:

- Add Admin APIs for subject enrollments, assessment rows, result status, and assessment publication.
- Reuse the current Circular 22/2021 calculation rules; never calculate semester averages in React.
- Validate score range `0.0-10.0`, numeric/remark mode consistency, required regular-assessment count, midterm/final constraints, and assessment date within term.
- Support recorded, make-up required, excused, and finalized absence states.
- Add transactional batch entry, optimistic locking, audit history, and publication timestamp/actor.
- Student APIs expose only published results; drafts remain Admin-only.

Admin Web deliverables:

- Class/term/subject filters and a keyboard-friendly grade entry grid.
- Separate draft save and publish actions with explicit confirmation.
- Inline validation, paste handling, unsaved-change warning, conflict recovery, and summary of incomplete required assessments.
- Student detail view showing the same computed average and component breakdown returned by the backend.

Playwright spec: `admin-web/e2e/phase-a5-grades.spec.ts`

- Enter valid numeric and remark assessments for a student.
- Verify invalid scores and incomplete publication are blocked.
- Publish the valid result and verify the backend-computed average.
- Open Flutter and verify the published component scores and semester result.

Exit gate:

- Grade calculation unit and PostgreSQL integration tests cover boundary values and incomplete data.
- Existing student grade E2E remains green.
- Playwright A1-A5 pass.

### Phase A6 — Forms, announcements, and notifications

**Goal:** Provide the operational workflow for reviewing student requests and communicating school changes.

**Implementation status (2026-07-15):** Complete. The form queue supports
status filtering and approval/rejection with version checks; announcements
support audience targeting and visibility windows. Published content is
verified through the authenticated student home contract.

Backend deliverables:

- Add paginated Admin form queue/detail APIs and allowed status transitions.
- Require an Admin note for rejection; append status history atomically and prevent invalid/repeated transitions.
- Add announcement CRUD, audience targeting, visibility window, draft/published state, and archive behavior.
- Add student notification records for timetable changes, published grades, approved/rejected forms, event changes, and club decisions.
- Make notification creation idempotent and transactionally tied to the triggering mutation.

Admin Web deliverables:

- Form queue with type/status/date/class filters, detail panel, timeline, approve, reject, and reason dialog.
- Announcement list/editor with audience preview and visibility dates.
- Notification delivery preview and status display without exposing internal transport credentials.

Playwright spec: `admin-web/e2e/phase-a6-operations.spec.ts`

- Approve one form, reject another with a required note, and reject an invalid transition.
- Publish a grade-targeted announcement.
- Log into Flutter and verify form timeline, announcement visibility, and the correct notification text.
- Verify another grade does not receive the targeted announcement.

Exit gate:

- Status-transition and authorization matrices have integration coverage.
- Existing forms/home/notifications regressions remain green.
- Playwright A1-A6 pass.

### Phase A7 — Events and clubs

**Goal:** Manage school activities and student participation using the current event/club business rules.

**Implementation status (2026-07-15):** Complete. Admins can create, update,
enable or disable events and clubs and process registration or membership
states. Student visibility uses the same enabled, audience, deadline, and
capacity rules and is covered by cross-application tests.

Backend deliverables:

- Add Admin CRUD/archive APIs for events and clubs.
- Add Admin registration and membership queues with approve/reject/cancel operations where valid.
- Reuse capacity, audience, deadline, and row-lock concurrency rules.
- Prevent changes that would silently invalidate active registrations or memberships; return a conflict with actionable counts.
- Add image URL validation and leave binary asset storage behind an adapter; do not store image files in PostgreSQL.

Admin Web deliverables:

- Event and club lists with status/category/audience/date filters.
- Create/edit forms, preview cards matching student-visible content, and registration/member management tables.
- Capacity indicators, deadline warnings, conflict dialogs, and archive actions.

Playwright spec: `admin-web/e2e/phase-a7-activities.spec.ts`

- Create and publish one event and one club for grade 10.
- Verify they appear in Flutter with the correct metadata.
- Complete a student registration/application and manage the resulting Admin queue.
- Verify capacity/deadline conflicts and grade isolation.

Exit gate:

- Concurrency and capacity integration tests pass against PostgreSQL.
- Existing student events/clubs E2E remains green.
- Playwright A1-A7 pass.

### Phase A8 — Audit, exports, and release hardening

**Goal:** Make the Admin site operable, traceable, accessible, and deployable.

**Implementation status (2026-07-15):** Complete for the v1 release scope.
Privileged mutations are searchable in a paginated immutable audit viewer and
can be exported as UTF-8 CSV. Routes are lazy-loaded, the production bundle is
budgeted, the shell is responsive at 1024 px, and the complete Admin and student
regression suites pass without browser-console errors.

Backend deliverables:

- Add read-only audit log API with actor/action/entity/date filters and ADMIN-only access.
- Add streamed CSV exports for students, schedules, grade summaries, forms, events, and clubs with explicit column allow-lists.
- Protect CSV against formula injection and enforce export row limits/timeouts.
- Add security headers, production cookie/CORS validation, request size limits, structured logs, metrics, health probes, and backup/restore documentation.
- Add retention configuration for audit and expired session data.

Admin Web deliverables:

- Audit log viewer with filterable before/after changed-field detail.
- Export actions with progress, failure, and completion states.
- Responsive and accessibility polish, route-level lazy loading, bundle budgets, and production error reporting boundary.
- Production environment validation and deployment documentation.

Playwright spec: `admin-web/e2e/phase-a8-release.spec.ts`

- Verify an Admin mutation appears in the immutable audit log.
- Export filtered data and validate headers, encoding, and formula-injection escaping.
- Run the full Admin happy path and student cross-application regression.
- Run keyboard-only navigation, automated accessibility checks, and browser console checks.
- Verify unauthorized roles cannot read audit or export APIs.

Exit gate:

- All Admin Playwright specs A1-A8 pass serially from a clean database.
- All existing student Playwright specs remain green.
- Full backend test suite, Flutter tests, Admin Web tests, build, lint, and typecheck pass.
- Production smoke test passes with secrets supplied only through environment configuration.

## 7. Test strategy and phase gate

Every phase uses four layers:

1. Backend unit tests for domain calculations and transition rules.
2. Spring Boot integration tests with Testcontainers PostgreSQL and real Flyway migrations.
3. Admin Web component/application tests with typed API fixtures.
4. Playwright E2E against real Spring Boot, PostgreSQL, and built Admin Web. Cross-application phases also run the built Flutter Web client.

Each Playwright test must:

- Seed only deterministic non-production fixtures.
- Use APIs for setup when setup is not the behavior under test.
- Interact with the actual browser UI for the acceptance path.
- Assert API effects and visible UI effects.
- Fail on uncaught browser errors and unexpected `console.error` output.
- Capture screenshot, trace, and video on failure.
- Clean up its isolated Compose project even after failure.

No phase is complete while a previous phase's Playwright spec is failing.

## 8. Security threat model summary

| Threat | Required mitigation |
| --- | --- |
| Student reaches Admin API | Route and method authorization; negative integration/E2E tests |
| Admin token stolen by XSS | Access token in memory; HttpOnly refresh cookie; strict CSP; no unsafe HTML |
| CSRF on refresh/logout | SameSite cookie plus anti-CSRF token validation |
| IDOR across students/classes | Server-side scope checks; never trust client actor/scope fields |
| Mass assignment | Explicit request DTOs and field allow-lists |
| Grade/form race conditions | Row locks or optimistic versions; transactional transitions |
| Audit tampering | Append-only audit service; no Admin update/delete endpoint |
| CSV formula injection | Escape cells starting with `=`, `+`, `-`, or `@` |
| Sensitive data in logs | Structured redaction and tests for secrets/security fields |
| Brute-force login | Rate limiting, generic errors, security event recording |

## 9. Main risks and mitigations

- **Authentication refactor breaks Flutter:** preserve the student JSON contract and run existing auth E2E in A1 before proceeding.
- **Current class identity uses strings:** normalize in A3 with a data migration and compatibility projection instead of changing all modules at once.
- **Admin and student views disagree:** cross-application Playwright tests verify the same mutation in Admin Web and Flutter.
- **Large editable tables become slow:** server pagination, bounded queries, indexes, virtualization, and batch mutations.
- **Concurrent staff edits overwrite data:** record versions and HTTP `409` conflict recovery.
- **MUI X licensing drift:** use only community Data Grid capabilities in v1; document any future Pro-only requirement before adoption.

## 10. Definition of done

The Admin milestone is complete only when:

- Admin Web is a separate production build under `admin-web/`.
- Student Flutter APIs remain backward compatible.
- Every `/api/v1/admin/**` endpoint has positive ADMIN and negative STUDENT/anonymous tests.
- Every write operation validates input, handles concurrency, and emits an audit event.
- All database changes are Flyway migrations validated from an empty and an existing seeded database.
- Playwright A1-A8 and all existing student phase specs pass from clean infrastructure.
- Admin Web has no critical/high accessibility or security findings.
- Deployment, environment variables, backup, restore, and rollback procedures are documented.

## 11. Execution order

Start with Phase A1 only. Do not scaffold feature pages beyond the authenticated shell until the authentication contract and student regression suite are green. After each phase, keep the latest Admin and student prototypes running for visual review before starting the next phase.
