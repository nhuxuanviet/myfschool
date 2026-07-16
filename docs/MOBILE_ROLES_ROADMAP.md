# Mobile roles roadmap

This milestone extends the Flutter application from a student-only client to a
role-aware mobile application for students, teachers, and parents. Admin remains
a separate React application.

## Business decision

The requested capabilities are correct but incomplete for a production school
workflow. A teacher must be authorized by teaching assignment, not merely by the
`TEACHER` role. A parent must be authorized by an active guardian relationship,
not by a student identifier sent by the client.

The implementation therefore includes subject teachers, homeroom teachers,
multiple children per parent, multiple guardians per student, grade publication
and correction controls, and an audit trail. Attendance and roll-call workflows
are explicitly outside this product's scope.

## Regulatory baseline

- Circular 15/2026/TT-BGDDT took effect on 10 May 2026 and replaced Circular
  32/2020/TT-BGDDT. It covers school-family coordination, digital school
  management, assessment, and secure education data handling.
- Circular 22/2021/TT-BGDDT remains the assessment basis for lower- and
  upper-secondary students. Subject teachers directly record the assessments
  for the subjects they teach; homeroom teachers consolidate student outcomes.
- The application keeps `REGULAR`, `MIDTERM`, and `FINAL` as the assessment
  categories. Labels such as oral, 15-minute, and 45-minute remain assessment
  forms or display labels rather than hard-coded weighting rules.

Official references:

- https://chinhphu.vn/?classid=1&docid=217678&pageid=27160&typegroupid=6
- https://chinhphu.vn/?docid=203926&pageid=27160

## Role capability matrix

| Capability | Student | Teacher | Parent |
| --- | --- | --- | --- |
| Own/child profile | Own profile | Own staff profile | Linked children only |
| Timetable | Own class timetable | Own teaching timetable | Selected child's timetable |
| Grades | Own published results | Enter assigned class/subject results | Selected child's published results |
| Forms | Create and track own requests | Review class-scoped requests where assigned | Create for linked child and track |
| Events and clubs | Browse and register | Browse school activities | View child status and required consent |
| Notifications | Personal | Teaching and homeroom notifications | Notifications for selected child |
| AI assistant | Read-only own data | Read-only assigned teaching data | Read-only selected-child data |

## Teacher rules

### Subject teacher

- View the teaching schedule generated from active timetable assignments.
- View rosters only for assigned class, subject, and academic term combinations.
- Create or update regular, midterm, and final assessments only for those
  assignments and only while the grade book is open.
- See missing, make-up, and absent assessment states.
- Publish results to students and parents. After a grade book is locked, submit
  a correction request instead of directly overwriting the value.
- Every grade change records actor, old value, new value, reason, and timestamp.

### Homeroom teacher

- View the complete homeroom roster and consolidated academic overview.
- Record or consolidate conduct and learning comments where applicable.
- Confirm grade correction requests but cannot edit another teacher's subject
  grade directly.
- Send class-scoped announcements and contact requests to linked parents.

## Parent rules

- A parent can be linked to multiple students; a student can have multiple
  guardians. Links have relationship type, verification status, effective
  period, and contact priority.
- The parent selects a child once, and every subsequent API query validates that
  active link on the server.
- Parents can view the selected child's timetable, published grades and details,
  school notifications, forms, event registrations, and club status.
- Parents can submit and track permitted forms for a linked child and acknowledge
  notices or consent requests.
- Parents cannot edit grades, student registrations, or another child's data.
  Free-form parent-teacher chat is outside the first release;
  structured contact requests are auditable and safer.

## Identity and authorization model

- Backend roles become `STUDENT`, `TEACHER`, `PARENT`, and `ADMIN`.
- A user can hold more than one role because a teacher may also be a parent.
  `user_roles` replaces the single-role assumption without changing the separate
  Admin cookie login flow.
- Mobile login returns available roles. A single-role account enters directly;
  a multi-role account chooses a context. The access and refresh tokens are bound
  to the selected active role.
- Mobile API namespaces are explicit: `/api/v1/student/**`,
  `/api/v1/teacher/**`, and `/api/v1/parent/**`.
- Teacher and parent services derive the profile from the authenticated
  principal. Client-provided student, class, subject, or child identifiers are
  always checked against an active assignment or guardian link.

## Required data model

- `teacher_profiles`, `parent_profiles`, `user_roles`
- `teacher_subject_assignments`, `homeroom_assignments`
- `parent_student_links`
- Foreign-key teacher assignment on timetable entries instead of teacher name
  as the authorization source
- Grade-book publication/lock state and `grade_change_requests`
- Existing audit log extended for teacher grade mutations and form decisions

## Flutter navigation

- Student: Home, Timetable, Results, Activities, Profile.
- Teacher: Overview, Teaching schedule, Classes, Grade book, Profile.
- Parent: Overview, Child timetable, Results, Forms, Profile.
- Shared theme, session, network, error, notification, and AI popup components
  remain common. Role shells and repositories are separate so a screen cannot
  accidentally call an API for another role.
- Parent screens include a child selector. Multi-role accounts include a role
  switcher in Profile; switching creates a new role-bound session.

## Delivery phases and acceptance

| Phase | Scope | Playwright acceptance |
| --- | --- | --- |
| R1 | Role schema, profiles, role-bound JWT/refresh, role switcher | Student regression remains green; teacher/parent cannot access each other's APIs |
| R2 | Teacher assignments, teaching timetable, class rosters | Teacher sees only assigned lessons and students |
| R3 | Teacher grade book, publish/lock/correction audit | Assigned teacher records a score; parent/student see it only after publication; unauthorized teacher gets 403 |
| R4 | Guardian links, child selector, parent timetable and grades | Parent switches between linked children and cannot query an unlinked child |
| R5 | Parent forms, homeroom workflow, consent and notifications | Parent submits an allowed form; the assigned reviewer processes it; notice and consent status update end to end |
| R6 | Role-scoped AI, Admin management screens, hardening | AI uses only active role scope; Admin manages assignments/links; full three-role E2E and production smoke pass |

Each phase includes Flyway migration, Spring integration tests against
PostgreSQL, Flutter unit/widget tests, negative authorization tests, and a
Playwright E2E spec. No phase is complete while an earlier Student/Admin
regression fails.

## Explicitly deferred

- Attendance and roll-call tracking
- Online tuition payment and accounting reconciliation
- Unmoderated parent-teacher chat
- Payroll, teacher leave, and HR workflows
- LMS assignment submission and online examinations

These areas require separate financial, moderation, retention, or academic
integrity requirements and should not be hidden inside the first three-role
release.
