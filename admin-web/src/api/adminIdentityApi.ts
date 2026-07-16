import { parseResponse } from './adminAuthApi';

export interface AdminTeacher {
  id: string;
  teacherCode: string;
  fullName: string;
  email: string | null;
  phoneNumber: string | null;
  enabled: boolean;
  hasAccount: boolean;
  version: number;
}

export interface AdminTeacherPage {
  items: AdminTeacher[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TeacherFilters {
  query?: string;
  enabled?: boolean;
  hasAccount?: boolean;
  page: number;
  size: number;
  sort?: string;
}

export interface TeacherInput {
  teacherCode: string;
  fullName: string;
  email?: string;
  enabled: boolean;
  version: number;
}

export interface AdminParent {
  id: string;
  fullName: string;
  email: string | null;
  phoneNumber: string | null;
  enabled: boolean;
  hasAccount: boolean;
  linkedStudents: number;
  version: number;
}

export interface AdminParentPage {
  items: AdminParent[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ParentFilters {
  query?: string;
  enabled?: boolean;
  page: number;
  size: number;
  sort?: string;
}

/** Supplying a phone number also issues a sign-in account; leaving it out records the guardian only. */
export interface ParentInput {
  fullName: string;
  email?: string;
  phoneNumber?: string;
  initialPassword?: string;
}

export type Relationship = 'FATHER' | 'MOTHER' | 'GUARDIAN';

export interface GuardianLink {
  id: string;
  parentId: string;
  parentFullName: string;
  studentId: string;
  studentFullName: string;
  studentCode: string;
  relationship: Relationship;
  contactOrder: number;
  effectiveFrom: string;
  effectiveTo: string | null;
  inForce: boolean;
}

function authHeaders(accessToken: string): HeadersInit {
  return {
    Authorization: `Bearer ${accessToken}`,
    'Content-Type': 'application/json',
  };
}

function pageParameters(page: number, size: number, sort: string, query?: string): URLSearchParams {
  const parameters = new URLSearchParams({ page: String(page), size: String(size), sort });
  if (query) {
    parameters.set('query', query);
  }
  return parameters;
}

export async function getTeachers(
  accessToken: string,
  filters: TeacherFilters,
): Promise<AdminTeacherPage> {
  const parameters = pageParameters(
    filters.page,
    filters.size,
    filters.sort ?? 'fullName,asc',
    filters.query,
  );
  if (filters.enabled !== undefined) {
    parameters.set('enabled', String(filters.enabled));
  }
  if (filters.hasAccount !== undefined) {
    parameters.set('hasAccount', String(filters.hasAccount));
  }
  const response = await fetch(`/api/v1/admin/teachers?${parameters.toString()}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return parseResponse<AdminTeacherPage>(response);
}

export async function createTeacher(
  accessToken: string,
  input: Pick<TeacherInput, 'teacherCode' | 'fullName' | 'email'>,
): Promise<{ id: string }> {
  const response = await fetch('/api/v1/admin/teachers', {
    method: 'POST',
    headers: authHeaders(accessToken),
    body: JSON.stringify(input),
  });
  return parseResponse<{ id: string }>(response);
}

export async function updateTeacher(
  accessToken: string,
  teacherId: string,
  input: TeacherInput,
): Promise<{ id: string }> {
  const response = await fetch(`/api/v1/admin/teachers/${teacherId}`, {
    method: 'PUT',
    headers: authHeaders(accessToken),
    body: JSON.stringify(input),
  });
  return parseResponse<{ id: string }>(response);
}

export async function getParents(
  accessToken: string,
  filters: ParentFilters,
): Promise<AdminParentPage> {
  const parameters = pageParameters(
    filters.page,
    filters.size,
    filters.sort ?? 'fullName,asc',
    filters.query,
  );
  if (filters.enabled !== undefined) {
    parameters.set('enabled', String(filters.enabled));
  }
  const response = await fetch(`/api/v1/admin/parents?${parameters.toString()}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return parseResponse<AdminParentPage>(response);
}

export async function createParent(
  accessToken: string,
  input: ParentInput,
): Promise<{ id: string }> {
  const response = await fetch('/api/v1/admin/parents', {
    method: 'POST',
    headers: authHeaders(accessToken),
    body: JSON.stringify(input),
  });
  return parseResponse<{ id: string }>(response);
}

export async function updateParent(
  accessToken: string,
  parentId: string,
  input: { fullName: string; email?: string; enabled: boolean; version: number },
): Promise<{ id: string }> {
  const response = await fetch(`/api/v1/admin/parents/${parentId}`, {
    method: 'PUT',
    headers: authHeaders(accessToken),
    body: JSON.stringify(input),
  });
  return parseResponse<{ id: string }>(response);
}

export async function getGuardianLinks(
  accessToken: string,
  filters: { parentId?: string; studentId?: string; inForceOnly?: boolean },
): Promise<GuardianLink[]> {
  const parameters = new URLSearchParams({
    inForceOnly: String(filters.inForceOnly ?? true),
  });
  if (filters.parentId) {
    parameters.set('parentId', filters.parentId);
  }
  if (filters.studentId) {
    parameters.set('studentId', filters.studentId);
  }
  const response = await fetch(`/api/v1/admin/parents/links?${parameters.toString()}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return parseResponse<GuardianLink[]>(response);
}

export async function linkGuardian(
  accessToken: string,
  parentId: string,
  input: { studentId: string; relationship: Relationship; contactOrder: number },
): Promise<{ id: string }> {
  const response = await fetch(`/api/v1/admin/parents/${parentId}/links`, {
    method: 'POST',
    headers: authHeaders(accessToken),
    body: JSON.stringify(input),
  });
  return parseResponse<{ id: string }>(response);
}

/** Ends the link. The record survives so that past access stays answerable. */
export async function unlinkGuardian(accessToken: string, linkId: string): Promise<void> {
  const response = await fetch(`/api/v1/admin/parents/links/${linkId}`, {
    method: 'DELETE',
    headers: authHeaders(accessToken),
  });
  await parseResponse<void>(response);
}
