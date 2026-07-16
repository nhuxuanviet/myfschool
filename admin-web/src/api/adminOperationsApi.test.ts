import { afterEach, describe, expect, it, vi } from 'vitest';
import { createLesson, getAdminTimetable, getAudit } from './adminOperationsApi';

describe('adminOperationsApi', () => {
  afterEach(() => vi.restoreAllMocks());

  it('scopes timetable queries and sends the in-memory admin token', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({
      periods: [], lessons: [], overrides: [],
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }));

    await getAdminTimetable('admin-token', 'term-id', 'class-id');

    const [url, options] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('academicTermId=term-id');
    expect(String(url)).toContain('schoolClassId=class-id');
    expect(options?.headers).toMatchObject({ Authorization: 'Bearer admin-token' });
  });

  it('serializes privileged mutations and bounded audit pagination', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 'lesson-id' }), {
        status: 201, headers: { 'Content-Type': 'application/json' },
      }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        items: [], page: 2, size: 50, totalElements: 0, totalPages: 0,
      }), { status: 200, headers: { 'Content-Type': 'application/json' } }));

    await createLesson('admin-token', { academicTermId: 'term-id', periodNumber: 1 });
    await getAudit('admin-token', 'LESSON', 2, 50);

    expect(fetchMock.mock.calls[0][1]?.method).toBe('POST');
    expect(fetchMock.mock.calls[0][1]?.body).toContain('term-id');
    expect(String(fetchMock.mock.calls[1][0])).toContain('query=LESSON');
    expect(String(fetchMock.mock.calls[1][0])).toContain('page=2');
    expect(String(fetchMock.mock.calls[1][0])).toContain('size=50');
  });
});
