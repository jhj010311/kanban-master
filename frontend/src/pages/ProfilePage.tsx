import React, { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { ProfilePhotoUpload } from '@/components/ProfilePhotoUpload';
import { authService, UserIdentity } from '@/services/authService';

const ProfilePage: React.FC = () => {
  const { user, updateAvatar, removeAvatar } = useAuth();
  const [identities, setIdentities] = useState<UserIdentity[]>([]);
  const [loadingIdentities, setLoadingIdentities] = useState(true);
  const [unlinkingId, setUnlinkingId] = useState<number | null>(null);

  useEffect(() => {
    loadUserIdentities();
  }, []);

  const loadUserIdentities = async () => {
    try {
      setLoadingIdentities(true);
      const data = await authService.getUserIdentities();
      setIdentities(data);
    } catch (error) {
      console.error('Failed to load user identities:', error);
    } finally {
      setLoadingIdentities(false);
    }
  };

  const handleUnlinkIdentity = async (identityId: number, provider: string) => {
    if (!confirm(`${provider} 계정 연동을 해제하시겠습니까?`)) {
      return;
    }

    try {
      setUnlinkingId(identityId);
      await authService.unlinkIdentity(identityId);
      // 목록에서 제거
      setIdentities(prev => prev.filter(identity => identity.id !== identityId));
      alert('계정 연동이 해제되었습니다.');
    } catch (error) {
      console.error('Failed to unlink identity:', error);
      alert('계정 연동 해제에 실패했습니다.');
    } finally {
      setUnlinkingId(null);
    }
  };

  const getProviderDisplayName = (provider: string): string => {
    const names: Record<string, string> = {
      GOOGLE: 'Google',
      KAKAO: 'Kakao',
      NAVER: 'Naver',
    };
    return names[provider] || provider;
  };

  const getProviderColor = (provider: string): string => {
    const colors: Record<string, string> = {
      GOOGLE: 'text-red-600',
      KAKAO: 'text-yellow-600',
      NAVER: 'text-green-600',
    };
    return colors[provider] || 'text-gray-600';
  };

  if (!user) {
    return null;
  }

  return (
    <div className="h-full bg-gradient-pastel flex flex-col overflow-hidden">
      <main className="flex-1 overflow-auto">
        <div className="w-full max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* 페이지 헤더 */}
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-pastel-blue-900">프로필 설정</h1>
            <p className="text-sm text-pastel-blue-600 mt-2">
              프로필 사진과 개인 정보를 관리하세요
            </p>
          </div>

          {/* 프로필 사진 섹션 */}
          <section className="glass-light rounded-2xl p-8 mb-6">
            <h2 className="text-xl font-semibold text-pastel-blue-900 mb-6">프로필 사진</h2>
            <ProfilePhotoUpload
              currentAvatarUrl={user.avatarUrl}
              userName={user.name}
              onUploadSuccess={updateAvatar}
              onDeleteSuccess={removeAvatar}
            />
          </section>

          {/* 기본 정보 섹션 */}
          <section className="glass-light rounded-2xl p-8 mb-6">
            <h2 className="text-xl font-semibold text-pastel-blue-900 mb-6">기본 정보</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-pastel-blue-700 mb-2">
                  이름
                </label>
                <div className="px-4 py-3 bg-pastel-blue-50 border border-pastel-blue-200 rounded-lg text-pastel-blue-900">
                  {user.name}
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-pastel-blue-700 mb-2">
                  이메일
                </label>
                <div className="px-4 py-3 bg-pastel-blue-50 border border-pastel-blue-200 rounded-lg text-pastel-blue-900">
                  {user.email}
                </div>
              </div>
            </div>
          </section>

          {/* 소셜 계정 연동 섹션 */}
          <section className="glass-light rounded-2xl p-8">
            <h2 className="text-xl font-semibold text-pastel-blue-900 mb-2">소셜 계정 연동</h2>
            <p className="text-sm text-pastel-blue-600 mb-6">
              소셜 계정을 연동하여 간편하게 로그인하세요
            </p>

            {loadingIdentities ? (
              <div className="flex justify-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-pastel-blue-500"></div>
              </div>
            ) : identities.length === 0 ? (
              <div className="text-center py-8 px-4 bg-pastel-blue-50 border border-pastel-blue-200 rounded-lg">
                <p className="text-pastel-blue-600">연동된 소셜 계정이 없습니다.</p>
                <p className="text-sm text-pastel-blue-500 mt-2">
                  로그인 페이지에서 Google 계정으로 로그인하면 자동으로 연동됩니다.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {identities.map((identity) => (
                  <div
                    key={identity.id}
                    className="flex items-center justify-between p-4 bg-white/60 border border-pastel-blue-200 rounded-lg"
                  >
                    <div className="flex items-center gap-4">
                      {identity.profileImageUrl && (
                        <img
                          src={identity.profileImageUrl}
                          alt={identity.name}
                          className="w-10 h-10 rounded-full"
                        />
                      )}
                      <div>
                        <div className="flex items-center gap-2">
                          <span className={`font-semibold ${getProviderColor(identity.provider)}`}>
                            {getProviderDisplayName(identity.provider)}
                          </span>
                          <span className="text-pastel-blue-900">
                            {identity.name}
                          </span>
                        </div>
                        <p className="text-sm text-pastel-blue-600">
                          {identity.email}
                        </p>
                        <p className="text-xs text-pastel-blue-500 mt-1">
                          연동일: {new Date(identity.createdAt).toLocaleDateString('ko-KR')}
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => handleUnlinkIdentity(identity.id, getProviderDisplayName(identity.provider))}
                      disabled={unlinkingId === identity.id}
                      className="px-4 py-2 text-sm font-medium text-pastel-pink-700 bg-pastel-pink-50 border border-pastel-pink-200 rounded-lg hover:bg-pastel-pink-100 transition disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {unlinkingId === identity.id ? '해제 중...' : '연동 해제'}
                    </button>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
};

export default ProfilePage;
