import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  createParent,
  getGuardianLinks,
  getTeachers,
  linkGuardian,
  unlinkGuardian,
  updateTeacher,
} from './adminIdentityApi';

function jsonResponse(payload: unknown, status = 200): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function stubFetch(response: Response) {
  const fetchMock = vi.fn().mockResolvedValue(response);
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function requestedUrl(fetchMock: ReturnType<typeof stubFetch>): URL {
  return new URL(fetchMock.mock.calls[0][0] as string, 'http://localhost');
}

describe('adminIdentityApi', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('sends only the filters that were set', async () => {
    const fetchMock = stubFetch(
      jsonResponse({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
    );

    await getTeachers('token', { page: 0, size: 20, query: 'Lan', hasAccount: false });

    const url = requestedUrl(fetchMock);
    expect(url.pathname).toBe('/api/v1/admin/teachers');
    expect(url.searchParams.get('query')).toBe('Lan');
    expect(url.searchParams.get('hasAccount')).toBe('false');
    // enabled was not set, so it must not narrow the result server-side.
    expect(url.searchParams.has('enabled')).toBe(false);
  });

  it('keeps hasAccount=false rather than dropping it as falsy', async () => {
    const fetchMock = stubFetch(
      jsonResponse({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
    );

    await getTeachers('token', { page: 0, size: 20, enabled: false, hasAccount: false });

    const url = requestedUrl(fetchMock);
    expect(url.searchParams.get('enabled')).toBe('false');
    expect(url.searchParams.get('hasAccount')).toBe('false');
  });

  it('sends the version when updating a teacher so a stale write is refused', async () => {
    const fetchMock = stubFetch(jsonResponse({ id: 'teacher-1' }));

    await updateTeacher('token', 'teacher-1', {
      teacherCode: 'GV001',
      fullName: 'Nguyễn Thị Lan',
      enabled: true,
      version: 3,
    });

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/teachers/teacher-1', {
      method: 'PUT',
      headers: { Authorization: 'Bearer token', 'Content-Type': 'application/json' },
      body: JSON.stringify({
        teacherCode: 'GV001',
        fullName: 'Nguyễn Thị Lan',
        enabled: true,
        version: 3,
      }),
    });
  });

  it('creates a guardian without an account when no phone number is given', async () => {
    const fetchMock = stubFetch(jsonResponse({ id: 'parent-1' }, 201));

    await createParent('token', { fullName: 'Trần Thị Mẹ' });

    expect(fetchMock.mock.calls[0][1]).toMatchObject({
      method: 'POST',
      body: JSON.stringify({ fullName: 'Trần Thị Mẹ' }),
    });
  });

  it('asks for links in force by default', async () => {
    const fetchMock = stubFetch(jsonResponse([]));

    await getGuardianLinks('token', { parentId: 'parent-1' });

    const url = requestedUrl(fetchMock);
    expect(url.searchParams.get('inForceOnly')).toBe('true');
    expect(url.searchParams.get('parentId')).toBe('parent-1');
  });

  it('can ask for ended links too', async () => {
    const fetchMock = stubFetch(jsonResponse([]));

    await getGuardianLinks('token', { parentId: 'parent-1', inForceOnly: false });

    expect(requestedUrl(fetchMock).searchParams.get('inForceOnly')).toBe('false');
  });

  it('links a guardian to a student', async () => {
    const fetchMock = stubFetch(jsonResponse({ id: 'link-1' }, 201));

    await linkGuardian('token', 'parent-1', {
      studentId: 'student-1',
      relationship: 'FATHER',
      contactOrder: 1,
    });

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/parents/parent-1/links', {
      method: 'POST',
      headers: { Authorization: 'Bearer token', 'Content-Type': 'application/json' },
      body: JSON.stringify({ studentId: 'student-1', relationship: 'FATHER', contactOrder: 1 }),
    });
  });

  it('resolves when ending a link returns no content', async () => {
    stubFetch(new Response(null, { status: 204 }));

    await expect(unlinkGuardian('token', 'link-1')).resolves.toBeUndefined();
  });

  it('surfaces the server problem code when a link already exists', async () => {
    stubFetch(
      jsonResponse({ code: 'GUARDIAN_LINK_EXISTS', detail: 'Đã liên kết' }, 409),
    );

    await expect(
      linkGuardian('token', 'parent-1', {
        studentId: 'student-1',
        relationship: 'FATHER',
        contactOrder: 1,
      }),
    ).rejects.toMatchObject({ status: 409, code: 'GUARDIAN_LINK_EXISTS' });
  });
});
