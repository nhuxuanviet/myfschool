# Club membership decisions

Phase 8 supports six student-facing club categories: academic, sports, arts,
skills, community, and media. Each club is either open to all grades or one
specific grade from 6 through 12. List and detail visibility derive the grade
from the authenticated student's JWT-backed profile; a hidden club is
indistinguishable from an unknown club.

The membership lifecycle is `PENDING`, `ACTIVE`, `REJECTED`, or `WITHDRAWN`.
Students create a pending application, may withdraw it while pending, and may
reapply after withdrawal or rejection. Active and pending memberships reject
duplicate applications. School-side approval remains outside this student app
phase.

Capacity represents active members, so pending applications do not reserve a
place. Applying locks the club row before checking the current deadline,
accepting-applications flag, and active-member count. This gives concurrent
requests one authoritative decision without exceeding capacity. Flutter never
derives action availability itself: it renders the API's `canApply` and
`canWithdraw` flags.

The Home active-club counter includes only `ACTIVE` memberships. Local profiles
seed open, active, pending, full-capacity, and grade-restricted clubs so the API,
Flutter widgets, and Playwright flow exercise every important state.
