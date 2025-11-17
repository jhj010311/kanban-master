import React from 'react';

interface GoogleLoginButtonProps {
  text?: string;
}

/**
 * Google OAuth 로그인 버튼 컴포넌트
 */
const GoogleLoginButton: React.FC<GoogleLoginButtonProps> = ({
  text = 'Google로 로그인'
}) => {
  const handleGoogleLogin = () => {
    // OAuth 인증 시작 엔드포인트로 이동
    window.location.href = '/api/v1/auth/oauth2/authorization/google';
  };

  return (
    <button
      type="button"
      onClick={handleGoogleLogin}
      className="w-full flex items-center justify-center gap-3 px-4 py-2.5 bg-white border border-gray-300 rounded-lg font-semibold text-gray-700 hover:bg-gray-50 hover:shadow-md transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
      aria-label="Google 계정으로 로그인"
    >
      <img
        src="/icons/google.svg"
        alt="Google"
        className="w-5 h-5"
      />
      <span>{text}</span>
    </button>
  );
};

export default GoogleLoginButton;
