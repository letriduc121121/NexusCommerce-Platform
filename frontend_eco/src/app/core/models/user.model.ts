export type UserRole = 'ADMIN' | 'USER';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  phone?: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  tokenType?: string;
  userId: string;
  email: string;
  role: string;
}

export interface UserProfileResponse {
  id: string;
  email: string;
  fullName?: string;
  phone?: string;
  address?: string;
  avatarUrl?: string;
  status: string; // 'ACTIVE' | 'LOCKED'
  role: string;   // 'ADMIN' | 'USER'
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  fullName?: string;
  phone?: string;
  status?: string;
  role: string;
}

export interface UpdateUserRequest {
  fullName?: string;
  phone?: string;
  status?: string;
  role?: string;
}

export interface UpdateProfileRequest {
  fullName?: string;
  phone?: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

// Aliases for legacy compatibility
export interface User extends UserProfileResponse {}
