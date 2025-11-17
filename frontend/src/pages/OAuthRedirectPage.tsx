import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { authStorage } from '@/services/auth';

/**
 * OAuth 2.0 콜백 처리 페이지
 * 백엔드에서 발급한 JWT 토큰을 받아서 저장하고 프로필을 로드
 */
const OAuthRedirectPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { refreshProfile } = useAuth();

  useEffect(() => {
    const handleOAuthCallback = async () => {
      const token = searchParams.get('token');
      const error = searchParams.get('error');
      const errorMessage = searchParams.get('message');

      // 에러가 있으면 로그인 페이지로 리다이렉트
      if (error) {
        console.error('❌ [OAuthRedirect] OAuth 로그인 실패:', error, errorMessage);
        navigate(`/login?error=${error}&message=${encodeURIComponent(errorMessage || '')}`, {
          replace: true,
        });
        return;
      }

      // 토큰이 없으면 로그인 페이지로
      if (!token) {
        console.error('❌ [OAuthRedirect] 토큰이 제공되지 않음');
        navigate('/login?error=oauth_failed&message=인증 토큰을 받지 못했습니다.', {
          replace: true,
        });
        return;
      }

      try {
        console.log('🔐 [OAuthRedirect] Access Token 저장 중...');

        // Access Token 저장
        authStorage.setAccessToken(token);

        // 프로필 로드
        console.log('👤 [OAuthRedirect] 사용자 프로필 로드 중...');
        await refreshProfile();

        console.log('🎉 [OAuthRedirect] OAuth 로그인 성공, 대시보드로 이동');
        navigate('/', { replace: true });
      } catch (error) {
        console.error('❌ [OAuthRedirect] 프로필 로드 실패:', error);
        authStorage.clearAccessToken();
        navigate('/login?error=oauth_failed&message=사용자 정보를 불러오는데 실패했습니다.', {
          replace: true,
        });
      }
    };

    handleOAuthCallback();
  }, [navigate, searchParams, refreshProfile]);

  return (
    <div className="min-h-screen bg-gradient-pastel flex items-center justify-center">
      <div className="glass rounded-3xl p-10 shadow-glass max-w-md w-full text-center">
        <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-pastel-blue-500 mx-auto mb-6"></div>
        <h2 className="text-xl font-semibold text-pastel-blue-900 mb-2">로그인 중...</h2>
        <p className="text-pastel-blue-600">잠시만 기다려주세요.</p>
      </div>
    </div>
  );
};

export default OAuthRedirectPage;
