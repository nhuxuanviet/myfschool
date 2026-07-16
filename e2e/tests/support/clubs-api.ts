import { expect, type APIRequestContext, type APIResponse } from '@playwright/test';

export type ClubCategory = 'ACADEMIC' | 'SPORTS' | 'ARTS' | 'SKILLS' | 'COMMUNITY' | 'MEDIA';
export type ClubMembershipStatus = 'NOT_APPLIED' | 'PENDING' | 'ACTIVE' | 'REJECTED' | 'WITHDRAWN';

export interface SchoolClub {
  id: string;
  category: ClubCategory;
  name: string;
  description: string;
  advisorName: string;
  meetingSchedule: string;
  location: string;
  audienceGradeLevel: number | null;
  capacity: number | null;
  activeMemberCount: number;
  applicationDeadline: string | null;
  membershipStatus: ClubMembershipStatus;
  canApply: boolean;
  canWithdraw: boolean;
}

export class ClubsApi {
  constructor(private readonly request: APIRequestContext, private readonly baseUrl: string) {}
  list(token: string, category?: ClubCategory): Promise<APIResponse> {
    const url = new URL('/api/v1/clubs', this.baseUrl);
    if (category !== undefined) url.searchParams.set('category', category);
    return this.request.get(url.toString(), { headers: { Authorization: `Bearer ${token}` } });
  }
  listWithStudentId(token: string, studentId: string): Promise<APIResponse> {
    const url = new URL('/api/v1/clubs', this.baseUrl);
    url.searchParams.set('studentId', studentId);
    return this.request.get(url.toString(), { headers: { Authorization: `Bearer ${token}` } });
  }
  get(token: string, id: string): Promise<APIResponse> {
    return this.request.get(this.clubUrl(id).toString(), { headers: { Authorization: `Bearer ${token}` } });
  }
  apply(token: string, id: string): Promise<APIResponse> {
    return this.request.post(`${this.clubUrl(id)}/applications`, { headers: { Authorization: `Bearer ${token}` } });
  }
  withdraw(token: string, id: string): Promise<APIResponse> {
    return this.request.delete(`${this.clubUrl(id)}/applications`, { headers: { Authorization: `Bearer ${token}` } });
  }
  private clubUrl(id: string): URL {
    return new URL(`/api/v1/clubs/${encodeURIComponent(id)}`, this.baseUrl);
  }
}

export async function expectClubs(response: APIResponse): Promise<{ clubs: SchoolClub[] }> {
  expect(response.status()).toBe(200);
  const body = (await response.json()) as { clubs: SchoolClub[] };
  expect(Object.keys(body)).toEqual(['clubs']);
  for (const club of body.clubs) expectClubShape(club);
  return body;
}

export async function expectClub(response: APIResponse, status: 200 | 201 = 200): Promise<SchoolClub> {
  expect(response.status()).toBe(status);
  const club = (await response.json()) as SchoolClub;
  expectClubShape(club);
  return club;
}

function expectClubShape(club: SchoolClub): void {
  expect(club.id).toMatch(/^[0-9a-f-]{36}$/i);
  expect(club.name.trim()).not.toBe('');
  expect(club.description.trim()).not.toBe('');
  expect(club.advisorName.trim()).not.toBe('');
  expect(club.meetingSchedule.trim()).not.toBe('');
  expect(club.location.trim()).not.toBe('');
  expect(club.activeMemberCount).toBeGreaterThanOrEqual(0);
  if (club.capacity !== null) expect(club.activeMemberCount).toBeLessThanOrEqual(club.capacity);
  expect(club.canWithdraw).toBe(club.membershipStatus === 'PENDING');
  if (club.membershipStatus === 'PENDING' || club.membershipStatus === 'ACTIVE') {
    expect(club.canApply).toBe(false);
  }
}
