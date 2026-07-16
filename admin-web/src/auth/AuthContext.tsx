import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import {
  login as loginRequest,
  logout as logoutRequest,
  readCsrfCookie,
  refresh as refreshRequest,
  type AdminAccount,
} from '../api/adminAuthApi';
import { AuthContext, type AuthContextValue, type AuthStatus } from './authState';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('checking');
  const [account, setAccount] = useState<AdminAccount | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [csrfToken, setCsrfToken] = useState<string | null>(null);
  const bootstrapStarted = useRef(false);
  const sessionOperation = useRef(0);

  const clearSession = useCallback(() => {
    setAccount(null);
    setAccessToken(null);
    setCsrfToken(null);
    setStatus('anonymous');
  }, []);

  const applySession = useCallback((session: Awaited<ReturnType<typeof loginRequest>>) => {
    setAccount(session.account);
    setAccessToken(session.accessToken);
    setCsrfToken(session.csrfToken);
    setStatus('authenticated');
  }, []);

  useEffect(() => {
    if (bootstrapStarted.current) {
      return;
    }
    bootstrapStarted.current = true;

    const cookieToken = readCsrfCookie();
    if (!cookieToken) {
      clearSession();
      return;
    }

    const operation = ++sessionOperation.current;
    refreshRequest(cookieToken)
      .then((session) => {
        if (sessionOperation.current === operation) applySession(session);
      })
      .catch(() => {
        if (sessionOperation.current === operation) clearSession();
      });
  }, [applySession, clearSession]);

  const login = useCallback(async (phoneNumber: string, password: string) => {
    const operation = ++sessionOperation.current;
    const session = await loginRequest(phoneNumber, password);
    if (sessionOperation.current === operation) applySession(session);
  }, [applySession]);

  const logout = useCallback(async () => {
    ++sessionOperation.current;
    const token = csrfToken ?? readCsrfCookie();
    try {
      if (token) {
        await logoutRequest(token);
      }
    } finally {
      clearSession();
    }
  }, [clearSession, csrfToken]);

  const value = useMemo<AuthContextValue>(() => ({
    status,
    account,
    accessToken,
    login,
    logout,
  }), [accessToken, account, login, logout, status]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
