import { expect, type APIRequestContext, type APIResponse } from '@playwright/test';

export const seededStudent = {
  phoneNumber: '0912345678',
  password: 'Student@123',
  replacementPassword: 'NewStudent@123',
  otp: '123456',
} as const;

export const seededPeerStudent = {
  phoneNumber: '0977777777',
  password: 'Student@123',
  gradeLevel: 11,
  className: '11A1',
  studentCode: 'SE1913011',
} as const;

export interface LoginCredentials {
  phoneNumber: string;
  password: string;
}

export interface StudentSummary {
  id: string;
  studentCode: string;
  fullName: string;
  gradeLevel: number;
  className: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  student: StudentSummary;
}

export interface HomeAcademicTerm {
  academicYear: string;
  code: string;
  name: string;
  startsOn: string;
  endsOn: string;
}

export interface HomeDashboardResponse {
  student: Omit<StudentSummary, 'id'>;
  academicTerm: HomeAcademicTerm | null;
  summary: {
    lessons: { today: number };
    events: { upcoming: number };
    forms: { pending: number };
    clubs: { active: number };
  };
  announcements: Array<{
    id: string;
    title: string;
    body: string;
    publishedAt: string;
  }>;
}

interface ResetChallengeResponse {
  challengeId: string;
  expiresIn: number;
}

interface ResetVerificationResponse {
  resetToken: string;
}

const authPath = '/api/v1/auth';

export class AuthApi {
  constructor(
    private readonly request: APIRequestContext,
    private readonly baseUrl: string,
  ) {}

  login(password: string): Promise<APIResponse> {
    return this.loginWithCredentials({
      phoneNumber: seededStudent.phoneNumber,
      password,
    });
  }

  loginWithCredentials({
    phoneNumber,
    password,
  }: LoginCredentials): Promise<APIResponse> {
    return this.post('/login', { phoneNumber, password });
  }

  refresh(refreshToken: string): Promise<APIResponse> {
    return this.post('/refresh', { refreshToken });
  }

  logout(refreshToken: string): Promise<APIResponse> {
    return this.post('/logout', { refreshToken });
  }

  requestPasswordReset(): Promise<APIResponse> {
    return this.post('/password-reset/request', {
      phoneNumber: seededStudent.phoneNumber,
    });
  }

  verifyPasswordReset(challengeId: string): Promise<APIResponse> {
    return this.post('/password-reset/verify', {
      challengeId,
      otp: seededStudent.otp,
    });
  }

  completePasswordReset(
    resetToken: string,
    newPassword: string,
  ): Promise<APIResponse> {
    return this.post('/password-reset/complete', { resetToken, newPassword });
  }

  getHome(accessToken: string): Promise<APIResponse> {
    return this.request.get(`${this.baseUrl}/api/v1/home`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  async resetPassword(newPassword: string): Promise<void> {
    const challengeResponse = await this.requestPasswordReset();
    expect(challengeResponse.status()).toBe(202);
    const challenge = (await challengeResponse.json()) as ResetChallengeResponse;
    expect(challenge.challengeId).toEqual(expect.any(String));
    expect(challenge.expiresIn).toBeGreaterThan(0);

    const verificationResponse = await this.verifyPasswordReset(
      challenge.challengeId,
    );
    expect(verificationResponse.status()).toBe(200);
    const verification =
      (await verificationResponse.json()) as ResetVerificationResponse;
    expect(verification.resetToken).toEqual(expect.any(String));

    const completionResponse = await this.completePasswordReset(
      verification.resetToken,
      newPassword,
    );
    expect(completionResponse.status()).toBe(204);
  }

  private post(path: string, data: object): Promise<APIResponse> {
    return this.request.post(`${this.baseUrl}${authPath}${path}`, { data });
  }
}

export async function expectTokenResponse(
  response: APIResponse,
): Promise<TokenResponse> {
  expect(response.status()).toBe(200);
  expect(response.headers()['content-type']).toContain('application/json');
  const body = (await response.json()) as TokenResponse;
  expect(body).toEqual(
    expect.objectContaining({
      accessToken: expect.any(String),
      refreshToken: expect.any(String),
      expiresIn: expect.any(Number),
      student: expect.objectContaining({
        id: expect.any(String),
        studentCode: expect.any(String),
        fullName: expect.any(String),
        gradeLevel: expect.any(Number),
        className: expect.any(String),
      }),
    }),
  );
  expect(body.accessToken).not.toBe('');
  expect(body.refreshToken).not.toBe('');
  expect(body.expiresIn).toBeGreaterThan(0);
  return body;
}

export async function expectUnauthorizedProblem(
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
      code: 'INVALID_REFRESH_TOKEN',
      instance: '/api/v1/auth/refresh',
      timestamp: expect.any(String),
    }),
  );
}

export async function expectHomeDashboardResponse(
  response: APIResponse,
): Promise<HomeDashboardResponse> {
  expect(response.status()).toBe(200);
  expect(response.headers()['content-type']).toContain('application/json');
  const body = (await response.json()) as HomeDashboardResponse;
  expect(body).toEqual(
    expect.objectContaining({
      student: expect.objectContaining({
        studentCode: expect.any(String),
        fullName: expect.any(String),
        gradeLevel: expect.any(Number),
        className: expect.any(String),
      }),
      summary: {
        lessons: { today: expect.any(Number) },
        events: { upcoming: expect.any(Number) },
        forms: { pending: expect.any(Number) },
        clubs: { active: expect.any(Number) },
      },
      announcements: expect.any(Array),
    }),
  );
  expect(body.student).not.toHaveProperty('id');
  expect(body.student.gradeLevel).toBeGreaterThanOrEqual(6);
  expect(body.student.gradeLevel).toBeLessThanOrEqual(12);
  for (const value of [
    body.summary.lessons.today,
    body.summary.events.upcoming,
    body.summary.forms.pending,
    body.summary.clubs.active,
  ]) {
    expect(value).toBeGreaterThanOrEqual(0);
  }
  if (body.academicTerm !== null) {
    expect(body.academicTerm).toEqual(
      expect.objectContaining({
        academicYear: expect.stringMatching(/^\d{4}-\d{4}$/),
        code: expect.any(String),
        name: expect.any(String),
        startsOn: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
        endsOn: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
      }),
    );
  }
  expect(body.announcements.length).toBeGreaterThan(0);
  return body;
}
