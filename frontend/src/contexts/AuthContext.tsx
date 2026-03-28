'use client';

import React, {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';
import type { LoginRequest, UserDto } from '@/types';
import { authApi, usersApi } from '@/lib/api';

interface AuthContextType {
  user: UserDto | null;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  hasPermission: (permission: string) => boolean;
  hasRole: (role: string) => boolean;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Restore session from localStorage on mount
  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    const stored = localStorage.getItem('currentUser');
    if (token && stored) {
      try {
        setUser(JSON.parse(stored) as UserDto);
      } catch {
        localStorage.removeItem('currentUser');
      }
    }
    setIsLoading(false);
  }, []);

  const login = useCallback(async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials);
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);

    // Fetch fresh user profile after login
    const profile = await usersApi.getByUsername(response.user.username);
    localStorage.setItem('currentUser', JSON.stringify(profile));
    setUser(profile);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // Ignore logout errors — clear client state regardless
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('currentUser');
      setUser(null);
    }
  }, []);

  const hasPermission = useCallback(
    (_permission: string) =>
      // UserDto only carries role names; ADMIN role has all permissions
      user?.roles.includes('ADMIN') ?? false,
    [user],
  );

  const hasRole = useCallback(
    (role: string) => user?.roles.includes(role) ?? false,
    [user],
  );

  const value = useMemo(
    () => ({ user, isLoading, login, logout, hasPermission, hasRole }),
    [user, isLoading, login, logout, hasPermission, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
