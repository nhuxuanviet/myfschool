import {
  expect,
  type APIRequestContext,
  type APIResponse,
} from '@playwright/test';

import type { HomeAcademicTerm } from './auth-api.js';

const ISO_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
const LOCAL_TIME_PATTERN = /^([01]\d|2[0-3]):([0-5]\d)$/;
const dayOfWeekValues = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const;
const lessonStatusValues = [
  'SCHEDULED',
  'CANCELLED',
  'REPLACED',
  'ADDED',
] as const;

export type TimetableDayOfWeek = (typeof dayOfWeekValues)[number];
export type TimetableLessonStatus = (typeof lessonStatusValues)[number];

export interface TimetableSubject {
  code: string;
  name: string;
}

export interface TimetableLesson {
  session: 'MORNING' | 'AFTERNOON';
  periodNumber: number;
  startTime: string;
  endTime: string;
  subject: TimetableSubject;
  teacherName: string | null;
  room: string | null;
  status: TimetableLessonStatus;
  note: string | null;
}

export interface TimetableDay {
  date: string;
  dayOfWeek: TimetableDayOfWeek;
  lessons: TimetableLesson[];
}

export interface TimetableResponse {
  timeZone: string;
  weekStart: string;
  weekEnd: string;
  academicTerms: HomeAcademicTerm[];
  days: TimetableDay[];
}

export interface TimetableLessonOccurrence extends TimetableLesson {
  date: string;
  dayOfWeek: TimetableDayOfWeek;
}

export class TimetableApi {
  constructor(
    private readonly request: APIRequestContext,
    private readonly baseUrl: string,
  ) {}

  get(accessToken: string, weekStart?: string): Promise<APIResponse> {
    const url = new URL('/api/v1/timetable', this.baseUrl);
    if (weekStart !== undefined) {
      url.searchParams.set('weekStart', weekStart);
    }

    return this.request.get(url.toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  getUnauthenticated(weekStart?: string): Promise<APIResponse> {
    const url = new URL('/api/v1/timetable', this.baseUrl);
    if (weekStart !== undefined) {
      url.searchParams.set('weekStart', weekStart);
    }
    return this.request.get(url.toString());
  }
}

export async function expectTimetableUnauthorized(
  response: APIResponse,
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
      instance: '/api/v1/timetable',
      timestamp: expect.any(String),
    }),
  );
}

export async function expectTimetableResponse(
  response: APIResponse,
): Promise<TimetableResponse> {
  expect(response.status()).toBe(200);
  expect(response.headers()['content-type']).toContain('application/json');

  const body = (await response.json()) as TimetableResponse;
  expect(body).toEqual(
    expect.objectContaining({
      timeZone: 'Asia/Ho_Chi_Minh',
      weekStart: expect.stringMatching(ISO_DATE_PATTERN),
      weekEnd: expect.stringMatching(ISO_DATE_PATTERN),
      academicTerms: expect.any(Array),
      days: expect.any(Array),
    }),
  );
  expect(body).not.toHaveProperty('academicTerm');
  expect(body.days).toHaveLength(7);
  expectIsoDate(body.weekStart, 'weekStart');
  expectIsoDate(body.weekEnd, 'weekEnd');
  expect(weekdayFor(body.weekStart)).toBe('MONDAY');
  expect(body.weekEnd).toBe(addDays(body.weekStart, 6));
  expectAcademicTerms(body);

  let lessonCount = 0;
  for (const [index, day] of body.days.entries()) {
    expect(day).toEqual(
      expect.objectContaining({
        date: expect.stringMatching(ISO_DATE_PATTERN),
        dayOfWeek: dayOfWeekValues[index],
        lessons: expect.any(Array),
      }),
    );
    expectIsoDate(day.date, `days[${index}].date`);
    expect(day.date).toBe(addDays(body.weekStart, index));

    let previousStartTime = -1;
    const periodNumbersBySession = new Map<string, Set<number>>();
    for (const [lessonIndex, lesson] of day.lessons.entries()) {
      assertLesson(lesson, `days[${index}].lessons[${lessonIndex}]`);
      expect(
        body.academicTerms.some((term) => termCoversDate(term, day.date)),
        `days[${index}].lessons[${lessonIndex}] must fall within a returned academic term`,
      ).toBe(true);
      const periodNumbers = periodNumbersBySession.get(lesson.session) ?? new Set();
      expect(
        periodNumbers.has(lesson.periodNumber),
        `days[${index}] has duplicate period ${lesson.periodNumber} in ${lesson.session}`,
      ).toBe(false);
      periodNumbers.add(lesson.periodNumber);
      periodNumbersBySession.set(lesson.session, periodNumbers);
      const startTime = minutesSinceMidnight(lesson.startTime);
      expect(startTime).toBeGreaterThanOrEqual(previousStartTime);
      expect(minutesSinceMidnight(lesson.endTime) - startTime).toBe(45);
      previousStartTime = startTime;
      lessonCount += 1;
    }

    for (const [session, periodNumbers] of periodNumbersBySession) {
      expect(
        periodNumbers.size,
        `days[${index}] has more than five ${session.toLowerCase()} lessons`,
      ).toBeLessThanOrEqual(5);
    }
  }

  expect(lessonCount).toBeGreaterThan(0);
  return body;
}

export function findTimetableException(
  timetable: TimetableResponse,
): TimetableLessonOccurrence {
  for (const day of timetable.days) {
    for (const lesson of day.lessons) {
      if (lesson.status !== 'SCHEDULED') {
        expect(lesson.note).toEqual(expect.any(String));
        expect(lesson.note?.trim()).not.toBe('');
        return { ...lesson, date: day.date, dayOfWeek: day.dayOfWeek };
      }
    }
  }

  throw new Error('Expected the seeded timetable to include an exception lesson.');
}

export function findScheduledTimetableLesson(
  timetable: TimetableResponse,
): TimetableLessonOccurrence {
  for (const day of timetable.days) {
    for (const lesson of day.lessons) {
      if (lesson.status === 'SCHEDULED') {
        return { ...lesson, date: day.date, dayOfWeek: day.dayOfWeek };
      }
    }
  }

  throw new Error('Expected the seeded timetable to include a scheduled lesson.');
}

export function addDays(isoDate: string, days: number): string {
  const date = parseIsoDate(isoDate);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function assertLesson(lesson: TimetableLesson, path: string): void {
  expect(lesson).toEqual(
    expect.objectContaining({
      session: expect.stringMatching(/^(MORNING|AFTERNOON)$/),
      periodNumber: expect.any(Number),
      startTime: expect.stringMatching(LOCAL_TIME_PATTERN),
      endTime: expect.stringMatching(LOCAL_TIME_PATTERN),
      subject: expect.objectContaining({
        code: expect.any(String),
        name: expect.any(String),
      }),
      status: expect.stringMatching(
        /^(SCHEDULED|CANCELLED|REPLACED|ADDED)$/,
      ),
    }),
  );
  expect(Number.isInteger(lesson.periodNumber), `${path}.periodNumber`).toBe(
    true,
  );
  expect(lesson.periodNumber).toBeGreaterThanOrEqual(1);
  expect(lesson.periodNumber).toBeLessThanOrEqual(5);
  expect(lesson.subject.code.trim(), `${path}.subject.code`).not.toBe('');
  expect(lesson.subject.name.trim(), `${path}.subject.name`).not.toBe('');
  expectNullableNonBlankString(lesson.teacherName, `${path}.teacherName`);
  expectNullableNonBlankString(lesson.room, `${path}.room`);
  expectNullableNonBlankString(lesson.note, `${path}.note`);
}

function expectNullableNonBlankString(value: string | null, path: string): void {
  expect(value === null || typeof value === 'string', path).toBe(true);
  if (value !== null) {
    expect(value.trim(), path).not.toBe('');
  }
}

function expectAcademicTerms(timetable: TimetableResponse): void {
  let previousTerm: HomeAcademicTerm | undefined;

  for (const [index, term] of timetable.academicTerms.entries()) {
    const path = `academicTerms[${index}]`;
    expect(term, path).not.toBeNull();
    expect(term).toEqual(
      expect.objectContaining({
        academicYear: expect.stringMatching(/^\d{4}-\d{4}$/),
        code: expect.any(String),
        name: expect.any(String),
        startsOn: expect.stringMatching(ISO_DATE_PATTERN),
        endsOn: expect.stringMatching(ISO_DATE_PATTERN),
      }),
    );
    expect(term.code.trim(), `${path}.code`).not.toBe('');
    expect(term.name.trim(), `${path}.name`).not.toBe('');
    expectIsoDate(term.startsOn, `${path}.startsOn`);
    expectIsoDate(term.endsOn, `${path}.endsOn`);
    expect(
      compareIsoDates(term.startsOn, term.endsOn),
      `${path} must not end before it starts`,
    ).toBeLessThanOrEqual(0);
    expect(
      termsOverlap(timetable.weekStart, timetable.weekEnd, term),
      `${path} must overlap the requested week`,
    ).toBe(true);

    if (previousTerm !== undefined) {
      expect(
        compareAcademicTerms(previousTerm, term),
        'academicTerms must be ordered by start date, end date, then code',
      ).toBeLessThanOrEqual(0);
    }
    previousTerm = term;
  }
}

function compareAcademicTerms(
  left: HomeAcademicTerm,
  right: HomeAcademicTerm,
): number {
  return (
    compareIsoDates(left.startsOn, right.startsOn) ||
    compareIsoDates(left.endsOn, right.endsOn) ||
    compareText(left.code, right.code)
  );
}

function termsOverlap(
  weekStart: string,
  weekEnd: string,
  term: HomeAcademicTerm,
): boolean {
  return (
    compareIsoDates(term.startsOn, weekEnd) <= 0 &&
    compareIsoDates(term.endsOn, weekStart) >= 0
  );
}

function termCoversDate(term: HomeAcademicTerm, date: string): boolean {
  return (
    compareIsoDates(term.startsOn, date) <= 0 &&
    compareIsoDates(term.endsOn, date) >= 0
  );
}

function compareIsoDates(left: string, right: string): number {
  return compareText(left, right);
}

function compareText(left: string, right: string): number {
  if (left === right) return 0;
  return left < right ? -1 : 1;
}

function expectIsoDate(value: string, path: string): void {
  expect(ISO_DATE_PATTERN.test(value), path).toBe(true);
  expect(parseIsoDate(value).toISOString().slice(0, 10), path).toBe(value);
}

function weekdayFor(isoDate: string): TimetableDayOfWeek {
  const weekdays = [
    'SUNDAY',
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
  ] as const;
  return weekdays[parseIsoDate(isoDate).getUTCDay()];
}

function minutesSinceMidnight(value: string): number {
  const match = LOCAL_TIME_PATTERN.exec(value);
  if (match === null) {
    throw new Error(`Expected an HH:mm local time, received ${value}.`);
  }
  return Number(match[1]) * 60 + Number(match[2]);
}

function parseIsoDate(value: string): Date {
  if (!ISO_DATE_PATTERN.test(value)) {
    throw new Error(`Expected an ISO date, received ${value}.`);
  }

  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.toISOString().slice(0, 10) !== value) {
    throw new Error(`Expected a valid ISO date, received ${value}.`);
  }
  return date;
}
