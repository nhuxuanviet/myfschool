import {
  expect,
  type APIRequestContext,
  type APIResponse,
} from '@playwright/test';

const ISO_INSTANT_PATTERN =
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/;
const eventCategoryValues = [
  'ACADEMIC',
  'CULTURAL',
  'SPORTS',
  'CLUB',
  'CAREER',
] as const;
const registrationStatusValues = [
  'NOT_REGISTERED',
  'REGISTERED',
  'CANCELLED',
] as const;
const eventResponseFields = [
  'id',
  'category',
  'title',
  'description',
  'location',
  'startsAt',
  'endsAt',
  'audienceGradeLevel',
  'capacity',
  'registeredCount',
  'registrationDeadline',
  'cancellationDeadline',
  'registrationStatus',
  'canRegister',
  'canCancel',
] as const;

export type EventCategory = (typeof eventCategoryValues)[number];
export type EventRegistrationStatus =
  (typeof registrationStatusValues)[number];

export interface SchoolEvent {
  id: string;
  category: EventCategory;
  title: string;
  description: string;
  location: string;
  startsAt: string;
  endsAt: string;
  audienceGradeLevel: number | null;
  capacity: number | null;
  registeredCount: number;
  registrationDeadline: string | null;
  cancellationDeadline: string | null;
  registrationStatus: EventRegistrationStatus;
  canRegister: boolean;
  canCancel: boolean;
}

export interface EventsResponse {
  timeZone: string;
  events: SchoolEvent[];
}

export interface EventsQuery {
  category?: EventCategory;
  includePast?: boolean;
}

export class EventsApi {
  constructor(
    private readonly request: APIRequestContext,
    private readonly baseUrl: string,
  ) {}

  list(
    accessToken: string,
    query: EventsQuery = {},
  ): Promise<APIResponse> {
    return this.listWithParameters(accessToken, query);
  }

  listUnauthenticated(query: EventsQuery = {}): Promise<APIResponse> {
    return this.request.get(this.eventsUrl(query).toString());
  }

  listWithStudentId(
    accessToken: string,
    studentId: string,
    query: EventsQuery = {},
  ): Promise<APIResponse> {
    return this.listWithParameters(accessToken, { ...query, studentId });
  }

  get(accessToken: string, eventId: string): Promise<APIResponse> {
    return this.request.get(this.eventUrl(eventId).toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  getWithStudentId(
    accessToken: string,
    eventId: string,
    studentId: string,
  ): Promise<APIResponse> {
    const url = this.eventUrl(eventId);
    url.searchParams.set('studentId', studentId);
    return this.request.get(url.toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  register(accessToken: string, eventId: string): Promise<APIResponse> {
    return this.request.post(this.registrationUrl(eventId).toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  cancel(accessToken: string, eventId: string): Promise<APIResponse> {
    return this.request.delete(this.registrationUrl(eventId).toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  private listWithParameters(
    accessToken: string,
    parameters: EventsQuery & { studentId?: string },
  ): Promise<APIResponse> {
    return this.request.get(this.eventsUrl(parameters).toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  private eventsUrl(
    parameters: EventsQuery & { studentId?: string },
  ): URL {
    const url = new URL('/api/v1/events', this.baseUrl);
    if (parameters.category !== undefined) {
      url.searchParams.set('category', parameters.category);
    }
    if (parameters.includePast !== undefined) {
      url.searchParams.set('includePast', String(parameters.includePast));
    }
    if (parameters.studentId !== undefined) {
      url.searchParams.set('studentId', parameters.studentId);
    }
    return url;
  }

  private eventUrl(eventId: string): URL {
    return new URL(`/api/v1/events/${encodeURIComponent(eventId)}`, this.baseUrl);
  }

  private registrationUrl(eventId: string): URL {
    return new URL(
      `/api/v1/events/${encodeURIComponent(eventId)}/registrations`,
      this.baseUrl,
    );
  }
}

export async function expectEventsUnauthorized(
  response: APIResponse,
): Promise<void> {
  await expectUnauthorizedEventProblem(response, '/api/v1/events');
}

export async function expectEventUnauthorized(
  response: APIResponse,
  instance: string,
): Promise<void> {
  await expectUnauthorizedEventProblem(response, instance);
}

export async function expectEventsResponse(
  response: APIResponse,
): Promise<EventsResponse> {
  expect(response.status()).toBe(200);
  expect(response.headers()['content-type']).toContain('application/json');

  const body = (await response.json()) as EventsResponse;
  expect(Object.keys(body).sort()).toEqual(['events', 'timeZone']);
  expect(body).toEqual(
    expect.objectContaining({
      timeZone: 'Asia/Ho_Chi_Minh',
      events: expect.any(Array),
    }),
  );

  const ids = new Set<string>();
  for (const [index, event] of body.events.entries()) {
    const path = `events[${index}]`;
    expectSchoolEvent(event, path);
    expect(ids.has(event.id), `${path}.id must be unique`).toBe(false);
    ids.add(event.id);
  }
  return body;
}

export async function expectEventResponse(
  response: APIResponse,
  expectedStatus: 200 | 201 = 200,
): Promise<SchoolEvent> {
  expect(response.status()).toBe(expectedStatus);
  expect(response.headers()['content-type']).toContain('application/json');

  const body = (await response.json()) as SchoolEvent;
  expectSchoolEvent(body, 'event');
  return body;
}

export async function expectEventNotFound(
  response: APIResponse,
  eventId: string,
): Promise<void> {
  expect(response.status()).toBe(404);
  expect(response.headers()['content-type']).toContain(
    'application/problem+json',
  );
  expect(await response.json()).toEqual(
    expect.objectContaining({
      status: 404,
      title: expect.any(String),
      detail: expect.any(String),
      code: 'EVENT_NOT_FOUND',
      instance: `/api/v1/events/${eventId}`,
      timestamp: expect.any(String),
    }),
  );
}

export async function expectEventRegistrationConflict(
  response: APIResponse,
  eventId: string,
  code:
    | 'EVENT_ALREADY_REGISTERED'
    | 'EVENT_REGISTRATION_CLOSED'
    | 'EVENT_CAPACITY_REACHED'
    | 'EVENT_CANCELLATION_CLOSED',
): Promise<void> {
  expect(response.status()).toBe(409);
  expect(response.headers()['content-type']).toContain(
    'application/problem+json',
  );
  expect(await response.json()).toEqual(
    expect.objectContaining({
      status: 409,
      title: expect.any(String),
      detail: expect.any(String),
      code,
      instance: `/api/v1/events/${eventId}/registrations`,
      timestamp: expect.any(String),
    }),
  );
}

export function findOpenCancellableEvent(events: SchoolEvent[]): SchoolEvent {
  const now = Date.now();
  const event = events.find(
    (candidate) =>
      candidate.canRegister &&
      candidate.registrationStatus !== 'REGISTERED' &&
      (candidate.cancellationDeadline === null ||
        instantMilliseconds(candidate.cancellationDeadline) > now),
  );
  if (event === undefined) {
    throw new Error(
      'Expected a seeded event that the student can register for and later cancel.',
    );
  }
  return event;
}

export function findFreshOpenCancellableEvent(
  events: SchoolEvent[],
): SchoolEvent {
  const now = Date.now();
  const event = events.find(
    (candidate) =>
      candidate.canRegister &&
      candidate.registrationStatus === 'NOT_REGISTERED' &&
      (candidate.cancellationDeadline === null ||
        instantMilliseconds(candidate.cancellationDeadline) > now),
  );
  if (event === undefined) {
    throw new Error(
      'Expected a seeded event with a new, not reactivated registration path.',
    );
  }
  return event;
}

export function findPastEvent(events: SchoolEvent[]): SchoolEvent {
  const now = Date.now();
  const event = events.find(
    (candidate) => instantMilliseconds(candidate.endsAt) < now,
  );
  if (event === undefined) {
    throw new Error('Expected a seeded event that has already ended.');
  }
  return event;
}

export function findCapacityBlockedEvent(events: SchoolEvent[]): SchoolEvent {
  const event = events.find(
    (candidate) =>
      candidate.capacity !== null &&
      candidate.registeredCount >= candidate.capacity &&
      candidate.registrationStatus !== 'REGISTERED',
  );
  if (event === undefined) {
    throw new Error('Expected a seeded event that has reached capacity.');
  }
  expect(event.canRegister).toBe(false);
  return event;
}

export function findRegistrationDeadlineBlockedEvent(
  events: SchoolEvent[],
): SchoolEvent {
  const now = Date.now();
  const event = events.find(
    (candidate) =>
      candidate.registrationDeadline !== null &&
      instantMilliseconds(candidate.registrationDeadline) <= now &&
      candidate.registrationStatus !== 'REGISTERED',
  );
  if (event === undefined) {
    throw new Error('Expected a seeded event with a closed registration deadline.');
  }
  expect(event.canRegister).toBe(false);
  return event;
}

export function findCancellationDeadlineBlockedEvent(
  events: SchoolEvent[],
): SchoolEvent {
  const now = Date.now();
  const event = events.find(
    (candidate) =>
      candidate.registrationStatus === 'REGISTERED' &&
      candidate.cancellationDeadline !== null &&
      instantMilliseconds(candidate.cancellationDeadline) <= now,
  );
  if (event === undefined) {
    throw new Error('Expected a seeded registration with a closed cancellation deadline.');
  }
  expect(event.canCancel).toBe(false);
  return event;
}

export function eventCategories(events: SchoolEvent[]): EventCategory[] {
  return [...new Set(events.map((event) => event.category))];
}

export function expectSameEvent(
  actual: SchoolEvent,
  expected: SchoolEvent,
): void {
  expect(actual).toEqual(expected);
}

async function expectUnauthorizedEventProblem(
  response: APIResponse,
  instance: string,
): Promise<void> {
  expect(response.status()).toBe(401);
  expect(response.headers()['content-type']).toContain(
    'application/problem+json',
  );
  expect(await response.json()).toEqual(
    expect.objectContaining({
      status: 401,
      title: expect.any(String),
      detail: expect.any(String),
      code: 'UNAUTHORIZED',
      instance,
      timestamp: expect.any(String),
    }),
  );
}

function expectSchoolEvent(event: SchoolEvent, path: string): void {
  expect(Object.keys(event).sort(), `${path} must expose only the event contract`).toEqual(
    [...eventResponseFields].sort(),
  );
  expect(event).toEqual(
    expect.objectContaining({
      id: expect.any(String),
      category: expect.stringMatching(/^(ACADEMIC|CULTURAL|SPORTS|CLUB|CAREER)$/),
      title: expect.any(String),
      description: expect.any(String),
      location: expect.any(String),
      startsAt: expect.any(String),
      endsAt: expect.any(String),
      registeredCount: expect.any(Number),
      registrationStatus: expect.stringMatching(
        /^(NOT_REGISTERED|REGISTERED|CANCELLED)$/,
      ),
      canRegister: expect.any(Boolean),
      canCancel: expect.any(Boolean),
    }),
  );
  expectUuid(event.id, `${path}.id`);
  expect(eventCategoryValues).toContain(event.category);
  expectNonBlank(event.title, `${path}.title`);
  expectNonBlank(event.description, `${path}.description`);
  expectNonBlank(event.location, `${path}.location`);
  const startsAt = instantMilliseconds(event.startsAt, `${path}.startsAt`);
  const endsAt = instantMilliseconds(event.endsAt, `${path}.endsAt`);
  expect(endsAt, `${path}.endsAt must be after startsAt`).toBeGreaterThan(
    startsAt,
  );
  expectNullableGradeLevel(event.audienceGradeLevel, `${path}.audienceGradeLevel`);
  expectNullablePositiveInteger(event.capacity, `${path}.capacity`);
  expectNonNegativeInteger(event.registeredCount, `${path}.registeredCount`);
  if (event.capacity !== null) {
    expect(
      event.registeredCount,
      `${path}.registeredCount must not exceed capacity`,
    ).toBeLessThanOrEqual(event.capacity);
  }
  expectNullableInstant(
    event.registrationDeadline,
    `${path}.registrationDeadline`,
  );
  expectNullableInstant(
    event.cancellationDeadline,
    `${path}.cancellationDeadline`,
  );
  expect(registrationStatusValues).toContain(event.registrationStatus);
  expect(typeof event.canRegister, `${path}.canRegister`).toBe('boolean');
  expect(typeof event.canCancel, `${path}.canCancel`).toBe('boolean');

  if (event.registrationStatus === 'REGISTERED') {
    expect(event.canRegister, `${path}.canRegister`).toBe(false);
  } else {
    expect(event.canCancel, `${path}.canCancel`).toBe(false);
  }
  if (event.canRegister && event.capacity !== null) {
    expect(event.registeredCount).toBeLessThan(event.capacity);
  }
}

function expectUuid(value: string, path: string): void {
  expect(
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
      value,
    ),
    path,
  ).toBe(true);
}

function expectNullableGradeLevel(value: number | null, path: string): void {
  expect(value === null || typeof value === 'number', path).toBe(true);
  if (value !== null) {
    expect(Number.isInteger(value), path).toBe(true);
    expect(value).toBeGreaterThanOrEqual(6);
    expect(value).toBeLessThanOrEqual(12);
  }
}

function expectNullablePositiveInteger(
  value: number | null,
  path: string,
): void {
  expect(value === null || typeof value === 'number', path).toBe(true);
  if (value !== null) {
    expect(Number.isInteger(value), path).toBe(true);
    expect(value).toBeGreaterThan(0);
  }
}

function expectNonNegativeInteger(value: number, path: string): void {
  expect(Number.isInteger(value), path).toBe(true);
  expect(value).toBeGreaterThanOrEqual(0);
}

function expectNullableInstant(value: string | null, path: string): void {
  expect(value === null || typeof value === 'string', path).toBe(true);
  if (value !== null) {
    instantMilliseconds(value, path);
  }
}

function instantMilliseconds(value: string, path = 'instant'): number {
  expect(ISO_INSTANT_PATTERN.test(value), path).toBe(true);
  const milliseconds = Date.parse(value);
  expect(Number.isNaN(milliseconds), path).toBe(false);
  return milliseconds;
}

function expectNonBlank(value: string, path: string): void {
  expect(value.trim(), path).not.toBe('');
}
