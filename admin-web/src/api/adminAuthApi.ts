export interface AdminAccount {
  id: string;
  fullName: string;
  role: 'ADMIN';
}

export interface AdminSession {
  accessToken: string;
  expiresIn: number;
  csrfToken: string;
  account: AdminAccount;
}

export class ApiProblem extends Error {
  readonly status: number;
  readonly code: string;

  constructor(
    status: number,
    code: string,
    detail: string,
  ) {
    super(detail);
    this.name = 'ApiProblem';
    this.status = status;
    this.code = code;
  }
}

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export async function parseResponse<T>(response: Response): Promise<T> {
  if (response.ok) {
    return response.status === 204 ? (undefined as T) : response.json() as Promise<T>;
  }

  const fallback = {
    code: 'REQUEST_FAILED',
    detail: 'Không thể kết nối đến hệ thống. Vui lòng thử lại.',
  };
  const problem = await response.json().catch(() => fallback) as {
    code?: string;
    detail?: string;
  };
  throw new ApiProblem(
    response.status,
    problem.code ?? fallback.code,
    problem.detail ?? fallback.detail,
  );
}

export async function login(phoneNumber: string, password: string): Promise<AdminSession> {
  const response = await fetch('/api/v1/admin/auth/login', {
    method: 'POST',
    credentials: 'include',
    headers: JSON_HEADERS,
    body: JSON.stringify({ phoneNumber, password }),
  });
  return parseResponse<AdminSession>(response);
}

export async function refresh(csrfToken: string): Promise<AdminSession> {
  const response = await fetch('/api/v1/admin/auth/refresh', {
    method: 'POST',
    credentials: 'include',
    headers: { 'X-CSRF-Token': csrfToken },
  });
  return parseResponse<AdminSession>(response);
}

export async function logout(csrfToken: string): Promise<void> {
  const response = await fetch('/api/v1/admin/auth/logout', {
    method: 'POST',
    credentials: 'include',
    headers: { 'X-CSRF-Token': csrfToken },
  });
  return parseResponse<void>(response);
}

export function readCsrfCookie(): string | null {
  const prefix = 'MYSCHOOL_ADMIN_CSRF=';
  const value = document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix));
  return value ? decodeURIComponent(value.slice(prefix.length)) : null;
}
