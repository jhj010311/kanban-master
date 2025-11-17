import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authService } from '@/services/authService';
import type { LoginRequest, UserProfile } from '@/types/auth';
import { authStorage } from '@/utils/authStorage';

interface AuthContextValue {
  user: UserProfile | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (payload: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  updateAvatar: (avatarUrl: string) => void;
  removeAvatar: () => void;
  setToken: (token: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<UserProfile | null>(null);

  // 페이지 새로고침 시에만 사용자 프로필 로드 (lazy loading)
  useEffect(() => {
    const loadUserProfile = async () => {
      const token = authStorage.getToken();
      if (!token || user) {
        // 토큰이 없거나 이미 user가 로드된 경우 스킵
        return;
      }

      try {
        console.debug('[AuthContext] Loading user profile in background...');
        const profile = await authService.fetchProfile();
        console.debug('[AuthContext] Profile loaded successfully:', profile.email);
        setUser(profile);
      } catch (error) {
        // 401 에러는 axios interceptor에서 처리 (토큰 갱신 시도 → 실패 시 로그아웃)
        // 500 에러 등은 무시 (사용자 정보 없이도 앱 사용 가능, 다음 API 호출에서 재시도)
        console.error('[AuthContext] Failed to load user profile (continuing without user info):', error);
      }
    };

    void loadUserProfile();
  }, []); // 최초 마운트 시 1회만 실행

  // Storage 변화 감지 (다른 탭에서 로그인/로그아웃 시)
  useEffect(() => {
    const handleStorageChange = () => {
      const token = authStorage.getToken();
      console.debug('[AuthContext] Storage changed. Token exists:', !!token);

      if (!token) {
        // 토큰이 삭제되면 user도 초기화
        setUser(null);
      } else if (!user) {
        // 토큰이 새로 생성되었는데 user가 없으면 profile 로드
        authService.fetchProfile()
          .then(profile => setUser(profile))
          .catch(err => console.error('[AuthContext] Failed to load profile after storage change:', err));
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [user]);

  const login = useCallback(async (payload: LoginRequest) => {
    console.log('🔑 [AuthContext.login] Starting login...');
    const result = await authService.login(payload);
    console.log('💾 [AuthContext.login] Saving token to localStorage');
    authStorage.setToken(result.accessToken);
    console.log('✔️ [AuthContext.login] Token saved. Token in storage:', authStorage.getToken()?.substring(0, 20) + '...');
    setUser(result.user);
    console.log('✔️ [AuthContext.login] User set:', result.user.email);
  }, []);

  const logout = useCallback(async () => {
    console.log('🚪 [AuthContext.logout] Starting logout...');
    try {
      await authService.logout();
      console.log('✔️ [AuthContext.logout] Server logout successful');
    } catch (err) {
      console.error('⚠️ [AuthContext.logout] Server logout failed (continuing with local logout):', err);
    } finally {
      authStorage.clearToken();
      console.log('✔️ [AuthContext.logout] Token cleared. Token in storage:', authStorage.getToken());
      setUser(null);
      console.log('✔️ [AuthContext.logout] User cleared');
    }
  }, []);

  const refreshProfile = useCallback(async () => {
    const profile = await authService.fetchProfile();
    setUser(profile);
  }, []);

  const updateAvatar = useCallback((avatarUrl: string) => {
    setUser(prev => prev ? { ...prev, avatarUrl } : null);
  }, []);

  const removeAvatar = useCallback(() => {
    setUser(prev => prev ? { ...prev, avatarUrl: null } : null);
  }, []);

  const setToken = useCallback(async (token: string) => {
    console.log('🔑 [AuthContext.setToken] Setting token from OAuth2 callback');
    authStorage.setToken(token);
    console.log('✔️ [AuthContext.setToken] Token saved. Fetching user profile...');
    try {
      const profile = await authService.fetchProfile();
      console.log('✔️ [AuthContext.setToken] Profile fetched:', profile.email);
      setUser(profile);
    } catch (error) {
      console.error('❌ [AuthContext.setToken] Failed to fetch profile:', error);
      authStorage.clearToken();
      throw error;
    }
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    isAuthenticated: Boolean(authStorage.getToken()), // 토큰 존재 여부로 인증 판단
    loading: false, // Bootstrap 로딩 제거
    login,
    logout,
    refreshProfile,
    updateAvatar,
    removeAvatar,
    setToken,
  }), [user, login, logout, refreshProfile, updateAvatar, removeAvatar, setToken]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
