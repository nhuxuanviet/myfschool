import { parseResponse } from './adminAuthApi';

const baseUrl = '/api/v1/admin/operations';

function headers(accessToken: string): HeadersInit {
  return { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' };
}

async function request<T>(
  accessToken: string,
  path: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: { ...headers(accessToken), ...init?.headers },
  });
  return parseResponse<T>(response);
}

export interface Period {
  id: string;
  academicTermId: string;
  session: 'MORNING' | 'AFTERNOON';
  periodNumber: number;
  startTime: string;
  endTime: string;
  version: number;
}

export interface TimetableLesson {
  id: string;
  academicTermId: string;
  schoolClassId: string;
  dayOfWeek: number;
  session: 'MORNING' | 'AFTERNOON';
  periodNumber: number;
  subjectId: string;
  subjectName: string;
  teacherName: string | null;
  room: string | null;
  version: number;
}

export interface TimetableOverride {
  id: string;
  academicTermId: string;
  schoolClassId: string;
  lessonDate: string;
  session: 'MORNING' | 'AFTERNOON';
  periodNumber: number;
  overrideType: 'CANCELLED' | 'REPLACED' | 'ADDED';
  subjectId: string | null;
  subjectName: string | null;
  teacherName: string | null;
  room: string | null;
  note: string | null;
  version: number;
}

export interface TimetableData {
  periods: Period[];
  lessons: TimetableLesson[];
  overrides: TimetableOverride[];
}

export interface StudentSubject {
  id: string;
  studentId: string;
  academicTermId: string;
  subjectId: string;
  subjectName: string;
  assessmentMode: 'NUMERIC' | 'REMARK';
  annualLessonCount: number | null;
  displayOrder: number;
  version: number;
}

export interface Assessment {
  id: string;
  studentTermSubjectId: string;
  assessmentKind: 'REGULAR' | 'MIDTERM' | 'FINAL';
  assessmentForm: string;
  displayLabel: string;
  durationMinutes: number | null;
  status: string;
  score: number | null;
  outcome: string | null;
  assessedOn: string | null;
  displayOrder: number;
  version: number;
}

export interface GradesData {
  subjects: StudentSubject[];
  assessments: Assessment[];
}

export interface StudentForm {
  id: string;
  studentId: string;
  studentCode: string;
  studentName: string;
  formType: string;
  reason: string;
  startsOn: string | null;
  endsOn: string | null;
  status: string;
  submittedAt: string;
  updatedAt: string;
  version: number;
}

export interface Announcement {
  id: string;
  title: string;
  body: string;
  audience: 'ALL' | 'GRADE';
  audienceGradeLevel: number | null;
  publishedAt: string;
  visibleFrom: string;
  visibleUntil: string | null;
  version: number;
}

export interface SchoolEvent {
  id: string;
  category: string;
  title: string;
  description: string;
  location: string;
  startsAt: string;
  endsAt: string;
  audience: 'ALL' | 'GRADE';
  audienceGradeLevel: number | null;
  capacity: number | null;
  registrationDeadline: string | null;
  cancellationDeadline: string | null;
  registrationEnabled: boolean;
  enabled: boolean;
  version: number;
  registeredCount: number;
}

export interface EventRegistration {
  id: string;
  eventId: string;
  studentId: string;
  studentCode: string;
  studentName: string;
  status: string;
  registeredAt: string;
  version: number;
}

export interface SchoolClub {
  id: string;
  category: string;
  name: string;
  description: string;
  advisorName: string;
  meetingSchedule: string;
  location: string;
  audience: 'ALL' | 'GRADE';
  audienceGradeLevel: number | null;
  capacity: number | null;
  applicationDeadline: string | null;
  acceptingApplications: boolean;
  enabled: boolean;
  version: number;
  activeCount: number;
}

export interface ClubMembership {
  id: string;
  clubId: string;
  studentId: string;
  studentCode: string;
  studentName: string;
  status: string;
  appliedAt: string;
  version: number;
}

export interface ActivitiesData {
  events: SchoolEvent[];
  eventRegistrations: EventRegistration[];
  clubs: SchoolClub[];
  clubMemberships: ClubMembership[];
}

export interface AuditEvent {
  id: string;
  actorUserId: string;
  actorName: string;
  action: string;
  entityType: string;
  entityId: string;
  changedFields: string;
  occurredAt: string;
}

export interface AuditPageData {
  items: AuditEvent[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export function getAdminTimetable(
  token: string,
  academicTermId: string,
  schoolClassId: string,
): Promise<TimetableData> {
  const query = new URLSearchParams({ academicTermId, schoolClassId });
  return request(token, `/timetable?${query}`);
}

export function createLesson(token: string, input: Record<string, unknown>): Promise<{ id: string }> {
  return request(token, '/timetable/lessons', { method: 'POST', body: JSON.stringify(input) });
}

export function updateLesson(token: string, id: string, input: Record<string, unknown>): Promise<void> {
  return request(token, `/timetable/lessons/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function deleteLesson(token: string, id: string, version: number): Promise<void> {
  return request(token, `/timetable/lessons/${id}?version=${version}`, { method: 'DELETE' });
}

export function createTimetableOverride(token: string, input: Record<string, unknown>): Promise<{ id: string }> {
  return request(token, '/timetable/overrides', { method: 'POST', body: JSON.stringify(input) });
}

export function deleteTimetableOverride(token: string, id: string, version: number): Promise<void> {
  return request(token, `/timetable/overrides/${id}?version=${version}`, { method: 'DELETE' });
}

export function getAdminGrades(token: string, studentId: string, termId: string): Promise<GradesData> {
  const query = new URLSearchParams({ studentId, academicTermId: termId });
  return request(token, `/grades?${query}`);
}

export function assignGradeSubject(token: string, input: Record<string, unknown>): Promise<{ id: string }> {
  return request(token, '/grades/subjects', { method: 'POST', body: JSON.stringify(input) });
}

export function createGradeAssessment(token: string, input: Record<string, unknown>): Promise<{ id: string }> {
  return request(token, '/grades/assessments', { method: 'POST', body: JSON.stringify(input) });
}

export function updateGradeAssessment(token: string, id: string, input: Record<string, unknown>): Promise<void> {
  return request(token, `/grades/assessments/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function getAdminForms(token: string, status?: string): Promise<StudentForm[]> {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return request(token, `/forms${query}`);
}

export function updateAdminFormStatus(
  token: string,
  id: string,
  status: string,
  version: number,
  note?: string,
): Promise<void> {
  return request(token, `/forms/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status, version, note }),
  });
}

export function getAnnouncements(token: string): Promise<Announcement[]> {
  return request(token, '/announcements');
}

export function createAnnouncement(token: string, input: Record<string, unknown>): Promise<{ id: string }> {
  return request(token, '/announcements', { method: 'POST', body: JSON.stringify(input) });
}

export function updateAnnouncement(token: string, id: string, input: Record<string, unknown>): Promise<void> {
  return request(token, `/announcements/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function deleteAnnouncement(token: string, id: string, version: number): Promise<void> {
  return request(token, `/announcements/${id}?version=${version}`, { method: 'DELETE' });
}

export function getActivities(token: string): Promise<ActivitiesData> {
  return request(token, '/activities');
}

export function createEvent(token: string, input: Record<string, unknown>): Promise<{ id: string }> {
  return request(token, '/activities/events', { method: 'POST', body: JSON.stringify(input) });
}

export function updateEvent(token: string, id: string, input: Record<string, unknown>): Promise<void> {
  return request(token, `/activities/events/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function createClub(token: string, input: Record<string, unknown>): Promise<{ id: string }> {
  return request(token, '/activities/clubs', { method: 'POST', body: JSON.stringify(input) });
}

export function updateClub(token: string, id: string, input: Record<string, unknown>): Promise<void> {
  return request(token, `/activities/clubs/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function updateActivityStatus(
  token: string,
  resource: 'event-registrations' | 'club-memberships',
  id: string,
  status: string,
  version: number,
): Promise<void> {
  return request(token, `/activities/${resource}/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ status, version }),
  });
}

export function getAudit(token: string, query: string, page: number, size: number): Promise<AuditPageData> {
  const parameters = new URLSearchParams({ query, page: String(page), size: String(size) });
  return request(token, `/audit?${parameters}`);
}

export async function downloadAuditCsv(token: string, query: string): Promise<Blob> {
  const parameters = new URLSearchParams({ query });
  const response = await fetch(`${baseUrl}/audit/export?${parameters}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await parseResponse(response);
  return response.blob();
}
