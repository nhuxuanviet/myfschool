# Education-domain decisions

## Vietnamese secondary-school timetable

Phase 4 follows the Ministry of Education and Training's consolidated General
Education Programme: a secondary-school lesson is **45 minutes**; a session has
at most **five lessons**; breaks occur between lessons; and upper-secondary
schools may organize two sessions a day when conditions permit.

Source: [Chương trình giáo dục phổ thông — Bộ GDĐT](https://moet.gov.vn/content/vanban/Lists/VBPQ/Attachments/1483/vbhn-chuong-trinh-tong-the.pdf)
(accessed 2026-07-11).

The programme specifies duration and capacity, not a nationwide bell schedule.
Therefore the application models the following as academic-term/school
configuration rather than hard-coding them in Flutter or Spring services:

- start and end time of every period;
- morning versus afternoon session;
- gaps/breaks between periods;
- recurring class timetable entries;
- date-specific cancellation, replacement, and make-up lessons.

The backend enforces the 45-minute duration and the period range 1–5 for each
session. The UI always renders the configured times returned by the API.

## Vietnamese secondary-school semester grades

Phase 5 follows [Circular 22/2021/TT-BGDĐT](https://vanban.chinhphu.vn/?docid=203926&pageid=27160),
which governs assessment of lower- and upper-secondary students (effective
2021-09-05). For a subject assessed with scores, each semester selects:

- two regular assessments for a subject with 35 annual lessons;
- three for more than 35 through 70 annual lessons;
- four for more than 70 annual lessons;
- one midterm and one final assessment.

The semester average is `(sum regular + 2 × midterm + 3 × final) / (regular
count + 5)`. The system uses decimal arithmetic and rounds the resulting value
to one decimal place with `HALF_UP`, never binary floating point. Oral,
15-minute, 45-minute, presentation, practical, experiment, product, and
project labels describe the assessment method; only the `REGULAR`, `MIDTERM`,
and `FINAL` category determines its coefficient.

For subjects assessed by remarks, the app requires two regular assessments, a
midterm, and a final. The term result is `ACHIEVED` only when all finalized
outcomes are achieved, `NOT_ACHIEVED` when a finalized outcome is not achieved,
and `PENDING` while required results are incomplete. Make-up and excused
workflow states deliberately do not produce a calculated grade until a final
result is recorded.

## School-event access and registration

Phase 6 keeps announcements as a separate home-feed capability and models
student events independently. An event is visible only when its audience is
`ALL` or its configured grade matches the authenticated student's grade. The
same visibility rule applies to list, detail, registration, and cancellation
so an event for another grade is not disclosed by its identifier.

Registration capacity is not a cached counter. The backend locks the event row,
counts active `REGISTERED` rows, and then creates or reactivates a registration
inside one transaction. This prevents concurrent requests from oversubscribing
an event. A cancelled registration does not consume capacity and may be
reactivated only while registration remains open.

Registration requires an enabled future event and a non-expired registration
deadline. Cancellation requires an active registration and the explicit
cancellation deadline, or (when no deadline is configured) a time before the
event starts. Event timestamps remain instants in the API; the UI uses the
returned `Asia/Ho_Chi_Minh` time-zone identifier for local presentation.
