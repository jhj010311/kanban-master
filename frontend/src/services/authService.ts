import axiosInstance from '@/utils/axios';
import type { AuthResponse, LoginRequest, TokenRefreshResponse, UserProfile } from '@/types/auth';

class AuthService {
  async signup(payload: { email: string; password: string; name: string }): Promise<AuthResponse> {
    console.log('📝 [AuthService] Posting signup request', payload.email);
    const response = await axiosInstance.post<AuthResponse>('/auth/signup', payload);
    console.log('✅ [AuthService] Signup response received:', {
      hasAccessToken: !!response.data.accessToken,
      tokenLength: response.data.accessToken?.length,
      hasUser: !!response.data.user,
      userEmail: response.data.user?.email,
    });
    return response.data;
  }

  async login(payload: LoginRequest): Promise<AuthResponse> {
    console.log('🔐 [AuthService] Posting login request', payload.email);
    const response = await axiosInstance.post<AuthResponse>('/auth/login', payload);
    console.log('✅ [AuthService] Login response received:', {
      hasAccessToken: !!response.data.accessToken,
      tokenLength: response.data.accessToken?.length,
      hasUser: !!response.data.user,
      userEmail: response.data.user?.email,
    });
    return response.data;
  }

  async logout(): Promise<void> {
    await axiosInstance.post('/auth/logout');
  }

  async fetchProfile(): Promise<UserProfile> {
    const response = await axiosInstance.get<UserProfile>('/auth/me');
    return response.data;
  }

  async refreshToken(): Promise<TokenRefreshResponse> {
    const response = await axiosInstance.post<TokenRefreshResponse>('/auth/refresh');
    return response.data;
  }

  /**
   * Get all linked OAuth2 identities for the current user
   */
  async getUserIdentities(): Promise<UserIdentity[]> {
    const response = await axiosInstance.get<UserIdentity[]>('/auth/me/identities');
    return response.data;
  }

  /**
   * Unlink an OAuth2 identity from the current user
   * @param identityId the identity ID to unlink
   */
  async unlinkIdentity(identityId: number): Promise<void> {
    await axiosInstance.delete(`/auth/oauth2/identities/${identityId}`);
  }
}

export const authService = new AuthService();

/**
 * OAuth2 user identity interface
 */
export interface UserIdentity {
  id: number;
  provider: 'GOOGLE' | 'KAKAO' | 'NAVER';
  providerUserId: string;
  email: string;
  name: string;
  profileImageUrl: string | null;
  createdAt: string;
}
