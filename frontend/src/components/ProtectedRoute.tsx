'use client';

import { useAuth } from '@/hooks/useAuth';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { Spin } from 'antd';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredPermissions?: string[];
  requiredRoles?: string[];
}

export function ProtectedRoute({
  children,
  requiredPermissions,
  requiredRoles,
}: ProtectedRouteProps) {
  const { user, isLoading, hasPermission, hasRole } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace('/login');
    }
  }, [user, isLoading, router]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Spin size="large" />
      </div>
    );
  }

  if (!user) return null;

  const permitted =
    (!requiredPermissions || requiredPermissions.some(hasPermission)) &&
    (!requiredRoles || requiredRoles.some(hasRole));

  if (!permitted) {
    return (
      <div className="flex items-center justify-center min-h-screen text-red-500 text-lg">
        Access Denied — insufficient permissions.
      </div>
    );
  }

  return <>{children}</>;
}
