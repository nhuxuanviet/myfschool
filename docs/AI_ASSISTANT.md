# Student assistant security and provider decisions

Phase 9 exposes one read-only chat endpoint for authenticated students. The
assistant can answer from six existing application services: timetable,
semester grades, upcoming events, student-owned forms, grade-visible clubs, and
grade-visible announcements. It does not expose mutation tools.

Authorization is structural rather than prompt-based. A tool object is created
for each request with the authenticated JWT subject already bound to it. Tool
methods accept no student or user identifier, so neither prompt injection nor a
client-supplied UUID can select another student's data. Existing feature
services perform the final profile, ownership, and grade-scope checks.

The `local` provider is the default for development and automated tests. It
routes Vietnamese keywords to the same authorized data service and gives stable
answers without network access or an API key. The `openai` provider uses Spring
AI 2.0 `ChatClient` and the same bound read-only tools. The system instruction
treats tool output as data, refuses cross-student disclosure, and limits the
assistant to school information.

OpenAI is enabled in production with three environment variables:

- `ASSISTANT_PROVIDER=openai`
- `AI_CHAT_MODEL=openai`
- `OPENAI_API_KEY=<secret>`

The provider and credential remain deployment-owned. Model, temperature,
maximum completion tokens, and memory depth are changed from the Admin
“Cấu hình AI” page. These safe runtime values are stored in PostgreSQL with
optimistic locking; updates are written to the Admin audit log. The API key is
never stored in PostgreSQL and never sent to React or Flutter.

Conversation messages are persisted per authenticated user and conversation ID
and trimmed to the configured memory depth. This retains follow-up context after
a restart and across backend replicas. A PostgreSQL-backed per-student rate
limit protects provider cost and abuse; defaults are 20 requests per minute and
can be configured with `ASSISTANT_RATE_LIMIT_WINDOW` and
`ASSISTANT_MAX_REQUESTS_PER_WINDOW`.

Provider errors are normalized to HTTP 503 and never return upstream response
bodies or credentials. Rate-limit rejection is HTTP 429. Model responses are
streamed as NDJSON plain-text deltas; Markdown is sanitized from the student UI.

The Spring AI 2.0 OpenAI adapter uses `ChatClient` with request-scoped options,
read-only function tools, `store=false`, and parallel tool calls disabled. The
default runtime model is `gpt-5.6-luna`; administrators can change it without a
backend restart when the configured provider supports the target model.
