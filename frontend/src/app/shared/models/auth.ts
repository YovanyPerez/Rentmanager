export type AppRole = 'ADMIN' | 'OWNER' | 'TENANT';

export interface AuthResponse {
  token: string;
  expiresAt: string;
  userId: number;
  email: string;
  fullName: string;
  role: AppRole;
}

export interface AuthUser {
  userId: number;
  email: string;
  fullName: string;
  role: AppRole;
  expiresAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}
