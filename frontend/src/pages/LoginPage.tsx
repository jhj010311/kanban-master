import { FormEvent, useState } from 'react';
import { isAxiosError } from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';

const LoginPage = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  // 로그인 후 항상 대시보드로 리다이렉트 (보안: 이전 사용자의 URL 접근 방지)
  const redirectTo = '/';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      console.log('📧 [LoginPage] Attempting login with email:', email);
      await login({ email, password });
      console.log('🎉 [LoginPage] Login successful, redirecting to:', redirectTo);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      console.error('❌ [LoginPage] Login failed:', err);
      let message = '로그인에 실패했습니다.';
      if (isAxiosError(err)) {
        const data = err.response?.data as { message?: string };
        message = data?.message ?? err.message ?? message;
        console.error('❌ [LoginPage] Axios error response:', err.response?.status, data);
      } else if (err instanceof Error) {
        message = err.message;
      }
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-pastel flex items-center justify-center px-4">
      <div className="max-w-4xl w-full grid md:grid-cols-2 gap-8">
        <div className="glass rounded-3xl p-10 shadow-glass hidden md:flex flex-col justify-between">
          <div>
            <p className="text-sm uppercase tracking-widest text-pastel-blue-500 mb-2">Modern Kanban</p>
            <h1 className="text-4xl font-bold text-pastel-blue-900 leading-tight">
              한눈에 보이는
              <br /> 팀의 모든 일
            </h1>
            <p className="text-pastel-blue-600 mt-6">
              글래스모피즘 UI로 제공되는 최신 칸반 보드에서 프로젝트를 시작해 보세요.
            </p>
          </div>
          <div className="glass rounded-2xl p-5 text-sm text-pastel-blue-800 shadow-glass-sm border border-white/40">
            <p className="font-semibold text-pastel-blue-900">디자인 가이드</p>
            <p className="mt-1 text-pastel-blue-600">
              파스텔 톤, 부드러운 그림자, 뚜렷한 포커스 링으로 브랜드 무드를 유지하세요.
            </p>
          </div>
        </div>

        <div className="glass-light rounded-3xl p-10 shadow-glass">
          <p className="text-sm text-pastel-blue-500 mb-3">팀 계정으로 로그인</p>
          <h2 className="text-2xl font-semibold text-pastel-blue-900 mb-8">Modern Kanban Service</h2>

          <form className="space-y-6" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-pastel-blue-800 mb-1">
                이메일
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                className="w-full rounded-2xl border border-pastel-blue-200/60 bg-white/70 px-4 py-3 text-pastel-blue-900 focus:outline-none focus:ring-2 focus:ring-pastel-blue-400"
                placeholder="team@company.com"
                required
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-pastel-blue-800 mb-1">
                비밀번호
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                className="w-full rounded-2xl border border-pastel-blue-200/60 bg-white/70 px-4 py-3 text-pastel-blue-900 focus:outline-none focus:ring-2 focus:ring-pastel-blue-400"
                placeholder="비밀번호를 입력하세요"
                required
              />
            </div>

            {error && (
              <div className="rounded-2xl px-4 py-3 text-sm border border-pastel-pink-200 bg-pastel-pink-50 text-pastel-pink-700 shadow-glass-sm">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded-2xl bg-gradient-to-r from-pastel-blue-500 to-pastel-purple-400 text-white font-semibold py-3 shadow-glass-lg transition hover:opacity-90 disabled:opacity-70"
            >
              {submitting ? '로그인 중...' : '로그인'}
            </button>
          </form>

          <div className="mt-6">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-pastel-blue-200"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-white/70 text-pastel-blue-600">또는</span>
              </div>
            </div>

            <div className="mt-6">
              <a
                href={`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/oauth2/authorization/google`}
                className="w-full flex items-center justify-center gap-3 rounded-2xl border border-pastel-blue-200/60 bg-white/90 px-4 py-3 text-pastel-blue-900 font-medium shadow-glass-sm transition hover:bg-white hover:shadow-glass"
              >
                <svg className="h-5 w-5" viewBox="0 0 24 24">
                  <path
                    fill="#4285F4"
                    d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                  />
                  <path
                    fill="#34A853"
                    d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                  />
                  <path
                    fill="#FBBC05"
                    d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                  />
                  <path
                    fill="#EA4335"
                    d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                  />
                </svg>
                Google로 계속하기
              </a>
            </div>
          </div>

          <div className="mt-6 text-center">
            <p className="text-pastel-blue-600">
              계정이 없으신가요?{' '}
              <Link to="/signup" className="text-pastel-blue-900 font-semibold hover:underline">
                회원가입
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
