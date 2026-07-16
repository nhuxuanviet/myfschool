# Student-form workflow decisions

Phase 7 provides a focused student workflow rather than a generic document
builder. A student can submit:

- a leave-of-absence request;
- a student-status confirmation request;
- a transcript request;
- a student-card reissue request.

Only leave requests contain `startsOn` and `endsOn`; PostgreSQL and the service
both require an inclusive, non-reversed date range. Other request types reject
dates so invalid combinations cannot be stored or interpreted differently by
the API and Flutter app.

New forms always start as `SUBMITTED`. School-side processing may move them to
`IN_REVIEW`, `APPROVED`, or `REJECTED`; seeded local examples demonstrate these
states until an administrative workflow is added. A student may cancel only a
`SUBMITTED` or `IN_REVIEW` form. Cancellation locks the owned form row, checks
the latest status, updates it to `CANCELLED`, and appends a timeline entry in
one transaction.

List, detail, creation, and cancellation derive the student from the JWT
subject. No client-provided student identifier is trusted. A form owned by a
different student returns the same `FORM_NOT_FOUND` response as an unknown ID.
The Home pending counter includes `SUBMITTED` and `IN_REVIEW` forms only.
