import apiClient from '@/lib/apiClient';
import type {
  AuthResponse,
  LoginRequest,
  UserDto,
  CreateUserRequest,
  UpdateUserRequest,
  RoleDto,
  CreateRoleRequest,
  PermissionDto,
  UserSessionDto,
  AuditLog,
  Page,
} from '@/types';

// ===== Auth =====
export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<AuthResponse>('/auth/login', data).then((r) => r.data),

  logout: () => apiClient.post<void>('/auth/logout').then((r) => r.data),

  refreshToken: (refreshToken: string) =>
    apiClient
      .post<AuthResponse>('/auth/refresh', null, { params: { refreshToken } })
      .then((r) => r.data),
};

// ===== Users =====
export const usersApi = {
  getAll: (page = 0, size = 20) =>
    apiClient
      .get<Page<UserDto>>('/users', { params: { page, size } })
      .then((r) => r.data),

  getById: (id: number) =>
    apiClient.get<UserDto>(`/users/${id}`).then((r) => r.data),

  getByUsername: (username: string) =>
    apiClient.get<UserDto>(`/users/username/${username}`).then((r) => r.data),

  create: (data: CreateUserRequest) =>
    apiClient.post<UserDto>('/users', data).then((r) => r.data),

  update: (id: number, data: UpdateUserRequest) =>
    apiClient.put<UserDto>(`/users/${id}`, data).then((r) => r.data),

  delete: (id: number) =>
    apiClient.delete<void>(`/users/${id}`).then((r) => r.data),

  enable: (id: number) =>
    apiClient.post<void>(`/users/${id}/enable`).then((r) => r.data),

  disable: (id: number) =>
    apiClient.post<void>(`/users/${id}/disable`).then((r) => r.data),

  unlock: (id: number) =>
    apiClient.post<void>(`/users/${id}/unlock`).then((r) => r.data),
};

// ===== Roles =====
export const rolesApi = {
  getAll: (page = 0, size = 20) =>
    apiClient
      .get<Page<RoleDto>>('/roles', { params: { page, size } })
      .then((r) => r.data),

  getActive: () =>
    apiClient.get<RoleDto[]>('/roles/active').then((r) => r.data),

  getById: (id: number) =>
    apiClient.get<RoleDto>(`/roles/${id}`).then((r) => r.data),

  create: (data: CreateRoleRequest) =>
    apiClient.post<RoleDto>('/roles', data).then((r) => r.data),

  update: (id: number, data: CreateRoleRequest) =>
    apiClient.put<RoleDto>(`/roles/${id}`, data).then((r) => r.data),

  delete: (id: number) =>
    apiClient.delete<void>(`/roles/${id}`).then((r) => r.data),

  activate: (id: number) =>
    apiClient.post<void>(`/roles/${id}/activate`).then((r) => r.data),

  deactivate: (id: number) =>
    apiClient.post<void>(`/roles/${id}/deactivate`).then((r) => r.data),
};

// ===== Permissions =====
export const permissionsApi = {
  getAll: (page = 0, size = 50) =>
    apiClient
      .get<Page<PermissionDto>>('/permissions', { params: { page, size } })
      .then((r) => r.data),

  getAllList: () =>
    apiClient.get<PermissionDto[]>('/permissions/all').then((r) => r.data),

  getById: (id: number) =>
    apiClient.get<PermissionDto>(`/permissions/${id}`).then((r) => r.data),

  create: (params: {
    name: string;
    description?: string;
    resource?: string;
    action?: string;
  }) =>
    apiClient
      .post<PermissionDto>('/permissions', null, { params })
      .then((r) => r.data),

  delete: (id: number) =>
    apiClient.delete<void>(`/permissions/${id}`).then((r) => r.data),
};

// ===== Sessions =====
export const sessionsApi = {
  getMySessions: () =>
    apiClient.get<UserSessionDto[]>('/sessions/my').then((r) => r.data),

  invalidateMySession: (sessionId: string) =>
    apiClient.delete<void>(`/sessions/my/${sessionId}`).then((r) => r.data),

  invalidateAllMySessions: () =>
    apiClient.delete<void>('/sessions/my/all').then((r) => r.data),

  getUserSessions: (username: string) =>
    apiClient
      .get<UserSessionDto[]>(`/sessions/user/${username}`)
      .then((r) => r.data),

  invalidateUserSession: (username: string, sessionId: string) =>
    apiClient
      .delete<void>(`/sessions/user/${username}/${sessionId}`)
      .then((r) => r.data),

  invalidateAllUserSessions: (username: string) =>
    apiClient
      .delete<void>(`/sessions/user/${username}/all`)
      .then((r) => r.data),
};

// ===== Audit =====
export const auditApi = {
  getAll: (page = 0, size = 20) =>
    apiClient
      .get<Page<AuditLog>>('/audit', { params: { page, size } })
      .then((r) => r.data),

  getByUser: (userId: number, page = 0, size = 20) =>
    apiClient
      .get<Page<AuditLog>>(`/audit/user/${userId}`, { params: { page, size } })
      .then((r) => r.data),

  getByAction: (action: string, page = 0, size = 20) =>
    apiClient
      .get<Page<AuditLog>>(`/audit/action/${action}`, {
        params: { page, size },
      })
      .then((r) => r.data),

  getByDateRange: (
    startDate: string,
    endDate: string,
    page = 0,
    size = 20,
  ) =>
    apiClient
      .get<Page<AuditLog>>('/audit/date-range', {
        params: { startDate, endDate, page, size },
      })
      .then((r) => r.data),

  getFailed: (page = 0, size = 20) =>
    apiClient
      .get<Page<AuditLog>>('/audit/failed', { params: { page, size } })
      .then((r) => r.data),
};

// ===== Security =====
export const securityApi = {
  changePassword: (oldPassword: string, newPassword: string) =>
    apiClient
      .post<{ message: string }>('/security/change-password', { oldPassword, newPassword })
      .then((r) => r.data),

  validatePassword: (password: string) =>
    apiClient
      .post<{ valid: boolean; errors: string[]; strength: string }>('/security/validate-password', { password })
      .then((r) => r.data),

  getPasswordPolicy: () =>
    apiClient
      .get<{ minLength: number; requireUppercase: boolean; requireLowercase: boolean; requireNumbers: boolean; requireSpecialChars: boolean }>('/security/password-policy')
      .then((r) => r.data),
};
