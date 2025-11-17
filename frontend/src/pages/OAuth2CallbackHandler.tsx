import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';

/**
 * OAuth2 콜백 핸들러 컴포넌트
 *
 * OAuth2 인증 성공 후 백엔드가 리다이렉트한 페이지에서
 * JWT 토큰을 추출하여 AuthContext에 저장하고 대시보드로 이동합니다.
 *
 * 백엔드 OAuth2AuthenticationSuccessHandler에서 다음 형식으로 리다이렉트:
 * http://localhost:3000/oauth2/callback?token=<access_token>
 * 또는 에러 시:
 * http://localhost:3000/oauth2/callback?error=<error_message>
 */
const OAuth2CallbackHandler = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { setToken } = useAuth();

  useEffect(() => {
    const token = searchParams.get('token');
    const error = searchParams.get('error');

    const handleCallback = async () => {
      if (error) {
        console.error('❌ [OAuth2Callback] OAuth2 authentication failed:', error);
        // 에러 메시지와 함께 로그인 페이지로 리다이렉트
        navigate(`/login?error=${encodeURIComponent(error)}`, { replace: true });
        return;
      }

      if (token) {
        console.log('✅ [OAuth2Callback] OAuth2 authentication successful, setting token');

        try {
          // JWT 토큰을 localStorage에 저장하고 프로필 가져오기 (AuthContext의 setToken이 처리)
          await setToken(token);

          console.log('🎉 [OAuth2Callback] Token saved and profile fetched, redirecting to dashboard');

          // 대시보드로 리다이렉트
          navigate('/', { replace: true });
        } catch (err) {
          console.error('❌ [OAuth2Callback] Failed to set token:', err);
          navigate('/login?error=로그인 처리 중 오류가 발생했습니다', { replace: true });
        }
      } else {
        console.warn('⚠️ [OAuth2Callback] No token or error found in URL parameters');
        // 토큰도 에러도 없으면 로그인 페이지로
        navigate('/login', { replace: true });
      }
    };

    handleCallback();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);  // 빈 의존성 배열: 컴포넌트 마운트 시 한 번만 실행

  return (
    <div className="min-h-screen bg-gradient-pastel flex items-center justify-center px-4">
      <div className="glass rounded-3xl p-10 shadow-glass max-w-md w-full text-center">
        <div className="flex justify-center mb-4">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-pastel-blue-500"></div>
        </div>
        <h2 className="text-xl font-semibold text-pastel-blue-900 mb-2">
          로그인 처리 중...
        </h2>
        <p className="text-pastel-blue-600">
          잠시만 기다려주세요.
        </p>
      </div>
    </div>
  );
};

export default OAuth2CallbackHandler;
