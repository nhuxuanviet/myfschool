# Typography audit — 2026-07-13

## Verdict

The app has a consistent 22 px centered page header, but several content titles used 21–24 px and competed with it. Metrics such as grades and calendar dates are valid exceptions; ordinary section and card titles are not. The target scale is: page header 22, section title 18–20, card title 15–16, body 14–16, metadata 13, badge 11–12, key metric 24–30.

## Captured steps

1. Home (`implementation-01-home.png`) — Healthy with intentional hero-name and date-metric exceptions.
2. Schedule (`implementation-02-schedule.png`) — Healthy; lesson titles remain below the page header.
3. Semester results (`implementation-03-grades.png`) — Needs correction; the 24 px term heading exceeded the 22 px page header.
4. Grade details (`implementation-feedback-grade-detail.png`) — Needs correction; component values did not share the summary's right-aligned scan axis.
5. Events (`implementation-04-events-list.png`) — Healthy after compact-card work; title 16, metadata 13.
6. Event details (`implementation-05-event-detail.png`) — Needs correction; the 22 px event title competed with the page header.
7. Clubs (`implementation-06-clubs-list.png`) — Healthy after compact-card work.
8. Club details (`implementation-07-club-detail.png`) — Needs correction; the 23 px club title exceeded the page header.
9. Forms list/create/details (`implementation-08-forms-list.png`, `implementation-09-form-create.png`, `implementation-10-form-detail.png`) — Mostly healthy; the 21 px form-detail title needed reduction.
10. Profile and notifications (`implementation-feedback-profile.png`, `implementation-feedback-notifications.png`) — Healthy; the profile name is an intentional identity-card exception.
11. Password reset (`implementation-feedback-reset-phone.png`) — Needs correction; the 22 px step heading competed with the page header.
12. Assistant (`implementation-feedback-assistant-popup.png`) — Healthy; message text stays at body scale.

## Accessibility limits

The screenshots support hierarchy and clipping review, not full accessibility compliance. Semantics and navigation are covered by widget and Playwright tests. Large-text scaling and screen-reader reading order require separate device-level checks.

## Resolution

All identified hierarchy issues were corrected. Post-change captures were reviewed in `design-qa/`; ordinary headings now stay below the 22 px page header, metrics remain intentional exceptions, and component scores align to the right. Final implementation gate: passed.
