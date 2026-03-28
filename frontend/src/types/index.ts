// ===== Auth =====
export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserDto;
}

// ===== User =====
export interface UserDto {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName?: string;
  enabled: boolean;
  accountNonExpired: boolean;
  accountNonLocked: boolean;
  credentialsNonExpired: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
  lastLoginAt?: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  roles?: string[];
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  roles?: string[];
}

// ===== Role =====
export interface RoleDto {
  id: number;
  name: string;
  description?: string;
  active: boolean;
  permissions: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateRoleRequest {
  name: string;
  description?: string;
  permissions?: string[];
}

// ===== Permission =====
export interface PermissionDto {
  id: number;
  name: string;
  description?: string;
  resource?: string;
  action?: string;
  createdAt: string;
}

// ===== Session =====
export interface UserSessionDto {
  sessionId: string;
  username: string;
  ipAddress: string;
  userAgent: string;
  createdAt: string;
  lastAccessedAt: string;
  expiresAt: string;
}

// ===== Audit =====
export interface AuditLog {
  id: number;
  userId?: number;
  username?: string;
  action: string;
  resource: string;
  details?: string;
  ipAddress?: string;
  userAgent?: string;
  success: boolean;
  timestamp: string;
}

// ===== Pagination =====
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// ===== API Error =====
export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
  path?: string;
}
