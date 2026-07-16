import { afterEach, describe, expect, it, vi } from 'vitest';
import { getStudents } from './adminAcademicsApi';

describe('adminAcademicsApi', () => {
  afterEach(() => vi.restoreAllMocks());

  it('builds bounded student filters and sends the admin token', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({
      items: [], page: 1, size: 20, totalElements: 0, totalPages: 0,
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }));

    await getStudents('admin-token', {
      query: 'SE19', gradeLevel: 10, enabled: true, page: 1, size: 20,
    });

    const [url, options] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('query=SE19');
    expect(String(url)).toContain('gradeLevel=10');
    expect(String(url)).toContain('page=1');
    expect(options?.headers).toEqual({ Authorization: 'Bearer admin-token' });
  });
});
