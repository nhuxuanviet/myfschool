import { parseResponse } from './adminAuthApi';

export interface AcademicYear {
  id: string;
  code: string;
  startsOn: string;
  endsOn: string;
  version: number;
}

export interface AcademicTerm {
  id: string;
  academicYearId: string;
  code: string;
  name: string;
  startsOn: string;
  endsOn: string;
  version: number;
}

export interface AcademicSubject {
  id: string;
  code: string;
  name: string;
  enabled: boolean;
  version: number;
}

export interface SchoolClass {
  id: string;
  academicYearId: string;
  code: string;
  name: string;
  gradeLevel: number;
  enabled: boolean;
  version: number;
  studentCount: number;
}

export interface AcademicCatalog {
  academicYears: AcademicYear[];
  terms: AcademicTerm[];
  subjects: AcademicSubject[];
  classes: SchoolClass[];
}

export interface AdminStudent {
  id: string;
  studentCode: string;
  fullName: string;
  phoneNumber: string;
  gradeLevel: number;
  classId: string | null;
  classCode: string;
  enabled: boolean;
  version: number;
  updatedAt: string;
}

export interface AdminStudentPage {
  items: AdminStudent[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface StudentFilters {
  query?: string;
  gradeLevel?: number;
  classId?: string;
  enabled?: boolean;
  page: number;
  size: number;
  sort?: string;
}

export interface StudentInput {
  phoneNumber: string;
  studentCode: string;
  fullName: string;
  classId: string;
  enabled: boolean;
  version: number;
  initialPassword?: string;
}

export interface SchoolClassInput {
  academicYearId: string;
  code: string;
  name: string;
  gradeLevel: number;
  enabled: boolean;
  version: number;
}

function authHeaders(accessToken: string): HeadersInit {
  return {
    Authorization: `Bearer ${accessToken}`,
    'Content-Type': 'application/json',
  };
}

export async function getAcademicCatalog(accessToken: string): Promise<AcademicCatalog> {
  const response = await fetch('/api/v1/admin/academics', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return parseResponse<AcademicCatalog>(response);
}

export async function getStudents(
  accessToken: string,
  filters: StudentFilters,
): Promise<AdminStudentPage> {
  const parameters = new URLSearchParams({
    page: String(filters.page),
    size: String(filters.size),
    sort: filters.sort ?? 'fullName,asc',
  });
  if (filters.query) parameters.set('query', filters.query);
  if (filters.gradeLevel) parameters.set('gradeLevel', String(filters.gradeLevel));
  if (filters.classId) parameters.set('classId', filters.classId);
  if (filters.enabled !== undefined) parameters.set('enabled', String(filters.enabled));
  const response = await fetch(`/api/v1/admin/students?${parameters}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return parseResponse<AdminStudentPage>(response);
}

export async function createStudent(accessToken: string, input: StudentInput): Promise<{ id: string }> {
  const response = await fetch('/api/v1/admin/students', {
    method: 'POST',
    headers: authHeaders(accessToken),
    body: JSON.stringify(input),
  });
  return parseResponse<{ id: string }>(response);
}

export async function updateStudent(
  accessToken: string,
  studentId: string,
  input: StudentInput,
): Promise<void> {
  const response = await fetch(`/api/v1/admin/students/${studentId}`, {
    method: 'PUT',
    headers: authHeaders(accessToken),
    body: JSON.stringify(input),
  });
  return parseResponse<void>(response);
}

export async function createSchoolClass(
  accessToken: string,
  input: SchoolClassInput,
): Promise<{ id: string }> {
  const response = await fetch('/api/v1/admin/academics/classes', {
    method: 'POST',
    headers: authHeaders(accessToken),
    body: JSON.stringify(input),
  });
  return parseResponse<{ id: string }>(response);
}

export async function updateSchoolClass(
  accessToken: string,
  classId: string,
  input: SchoolClassInput,
): Promise<void> {
  const response = await fetch(`/api/v1/admin/academics/classes/${classId}`, {
    method: 'PUT',
    headers: authHeaders(accessToken),
    body: JSON.stringify(input),
  });
  return parseResponse<void>(response);
}

export async function createAcademicItem(
  accessToken: string,
  resource: 'years' | 'terms' | 'subjects',
  input: Record<string, unknown>,
): Promise<{ id: string }> {
  const response = await fetch(`/api/v1/admin/academics/${resource}`, {
    method: 'POST',
    headers: authHeaders(accessToken),
    body: JSON.stringify(input),
  });
  return parseResponse<{ id: string }>(response);
}

export async function updateAcademicItem(
  accessToken: string,
  resource: 'years' | 'terms' | 'subjects',
  id: string,
  input: Record<string, unknown>,
): Promise<void> {
  const response = await fetch(`/api/v1/admin/academics/${resource}/${id}`, {
    method: 'PUT',
    headers: authHeaders(accessToken),
    body: JSON.stringify(input),
  });
  return parseResponse<void>(response);
}

export async function deleteAcademicItem(
  accessToken: string,
  resource: 'years' | 'terms' | 'subjects' | 'classes',
  id: string,
  version: number,
): Promise<void> {
  const response = await fetch(`/api/v1/admin/academics/${resource}/${id}?version=${version}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return parseResponse<void>(response);
}
