import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from './AuthContext';
import { useAuth } from './authState';
import * as adminAuthApi from '../api/adminAuthApi';

vi.mock('../api/adminAuthApi', async (importOriginal) => {
  const original = await importOriginal<typeof adminAuthApi>();
  return { ...original, readCsrfCookie: vi.fn(), refresh: vi.fn(), login: vi.fn() };
});

function Probe() {
  const auth = useAuth();
  return (
    <div>
      <span>{auth.status}</span>
      <span>{auth.account?.fullName ?? 'none'}</span>
      <button type="button" onClick={() => void auth.login('0900000000', 'Admin@123')}>login</button>
    </div>
  );
}

describe('AuthProvider', () => {
  beforeEach(() => vi.clearAllMocks());

  it('does not let a stale bootstrap refresh clear a newer login', async () => {
    let rejectRefresh!: (reason: Error) => void;
    vi.mocked(adminAuthApi.readCsrfCookie).mockReturnValue('stale-csrf');
    vi.mocked(adminAuthApi.refresh).mockReturnValue(new Promise((_, reject) => {
      rejectRefresh = reject;
    }));
    vi.mocked(adminAuthApi.login).mockResolvedValue({
      accessToken: 'new-token',
      expiresIn: 600,
      csrfToken: 'new-csrf',
      account: { id: 'admin-id', fullName: 'Quản trị FSchool', role: 'ADMIN' },
    });

    render(<AuthProvider><Probe /></AuthProvider>);
    await userEvent.click(screen.getByRole('button', { name: 'login' }));
    expect(await screen.findByText('authenticated')).toBeVisible();

    await act(async () => rejectRefresh(new Error('stale refresh failed')));

    expect(screen.getByText('authenticated')).toBeVisible();
    expect(screen.getByText('Quản trị FSchool')).toBeVisible();
  });
});
