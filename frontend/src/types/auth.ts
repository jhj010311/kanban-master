export interface WorkspaceMembership {
  workspaceId: number;
  workspaceName: string;
  slug: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'GUEST';
}

export type UserStatus = 'ACTIVE' | 'PENDING' | 'SUSPENDED';

export enum AuthProvider {
  LOCAL = 'LOCAL',
  GOOGLE = 'GOOGLE',
  GITHUB = 'GITHUB',
  KAKAO = 'KAKAO',
  NAVER = 'NAVER',
}

export interface UserIdentity {
  id: number;
  provider: AuthProvider;
  email: string;
  linkedAt: string;
}

export interface UserProfile {
  id: number;
  email: string;
  name: string;
  avatarUrl?: string | null;
  status: UserStatus;
  primaryProvider: AuthProvider;
  workspaces: WorkspaceMembership[];
  identities?: UserIdentity[];  // Phase 2에서 사용
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserProfile;
}

export interface TokenRefreshResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}
