import { afterEach, describe, expect, it, vi } from 'vitest';
import { getAdminDashboard } from './adminDashboardApi';

describe('adminDashboardApi', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('requests dashboard data with the administrator bearer token', async () => {
    const payload = {
      metrics: {
        totalStudents: 2,
        activeClasses: 2,
        pendingForms: 1,
        upcomingEvents: 4,
        pendingClubApplications: 1,
        recentlyUpdatedGrades: 3,
      },
      recentActivities: [],
      generatedAt: '2026-07-14T12:00:00Z',
    };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(payload), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(getAdminDashboard('admin-access-token')).resolves.toEqual(payload);
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/dashboard', {
      headers: { Authorization: 'Bearer admin-access-token' },
    });
  });
});
