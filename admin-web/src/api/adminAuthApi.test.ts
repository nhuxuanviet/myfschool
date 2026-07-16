import { afterEach, describe, expect, it, vi } from 'vitest';
import { login, readCsrfCookie } from './adminAuthApi';

describe('adminAuthApi', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.cookie = 'MYSCHOOL_ADMIN_CSRF=; Max-Age=0; Path=/';
  });

  it('sends login credentials through a same-origin credentialed request', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      accessToken: 'access',
      expiresIn: 600,
      csrfToken: 'csrf',
      account: { id: 'admin-id', fullName: 'Quản trị FSchool', role: 'ADMIN' },
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }));
    vi.stubGlobal('fetch', fetchMock);

    await login('0900000000', 'Admin@123');

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/auth/login', expect.objectContaining({
      method: 'POST',
      credentials: 'include',
      body: JSON.stringify({ phoneNumber: '0900000000', password: 'Admin@123' }),
    }));
  });

  it('maps problem details into a typed API error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
      code: 'INVALID_CREDENTIALS',
      detail: 'Invalid credentials',
    }), { status: 401, headers: { 'Content-Type': 'application/problem+json' } })));

    await expect(login('0900000000', 'wrong')).rejects.toEqual(expect.objectContaining({
      status: 401,
      code: 'INVALID_CREDENTIALS',
    }));
  });

  it('reads the non-HttpOnly CSRF cookie', () => {
    document.cookie = 'MYSCHOOL_ADMIN_CSRF=token%20value; Path=/';
    expect(readCsrfCookie()).toBe('token value');
  });
});
