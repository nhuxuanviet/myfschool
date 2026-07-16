import { parseResponse } from './adminAuthApi';

export interface AdminDashboardMetrics {
  totalStudents: number;
  activeClasses: number;
  pendingForms: number;
  upcomingEvents: number;
  pendingClubApplications: number;
  recentlyUpdatedGrades: number;
}

export interface AdminActivity {
  id: string;
  eventType: string;
  actorName: string;
  occurredAt: string;
}

export interface AdminDashboardData {
  metrics: AdminDashboardMetrics;
  recentActivities: AdminActivity[];
  generatedAt: string;
}

export async function getAdminDashboard(accessToken: string): Promise<AdminDashboardData> {
  const response = await fetch('/api/v1/admin/dashboard', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return parseResponse<AdminDashboardData>(response);
}
