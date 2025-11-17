# spec-enhance-user-authentication-with-social-login — Social Login 도입 (OAuth2 인증)

## 1. 개요

기존 이메일/비밀번호 기반 인증 시스템에 Google OAuth2 로그인을 추가하고, 향후 Kakao, Naver 등 다양한 소셜 로그인 프로바이더로 확장 가능한 아키텍처를 구축한다.

- Google OAuth2 2.0 표준을 준수하며 Spring Security OAuth2 Client를 사용한다.
- UserIdentity 엔티티를 추가하여 외부 프로바이더의 사용자 ID를 저장한다.
- 이메일이 일치하는 기존 계정에 소셜 계정을 자동 연동하거나 신규 User를 생성한다.
- 사용자는 프로필 설정에서 연동된 소셜 계정을 관리할 수 있다.

## 2. 연계 요구사항

- FR-06a Google OAuth2 로그인
- FR-06b UserIdentity 엔티티
- FR-06c 신규 사용자 자동 생성
- FR-06d 기존 계정 연동
- FR-06e 소셜 계정 연동 해제
- FR-06f 다중 프로바이더 지원
- FR-06g OAuth2 콜백 처리
- FR-06h 보안 토큰 발급
- FR-06i 프로바이더별 사용자 정보 매핑
- NFR-06a 성능 (콜백 처리 < 1초, 토큰 발급 < 500ms)
- NFR-06b 보안 (CSRF 방지, HTTPS 필수, 토큰 탈취 방지)
- NFR-06c 확장성 (프로바이더 추가 시 최소 코드 수정)

## 3. 주요 사용자 시나리오

### 시나리오 1: 처음 Google 로그인하는 신규 사용자

1. 사용자가 로그인 페이지에서 "Google로 로그인" 버튼을 클릭한다.
2. `/api/v1/auth/oauth2/authorization/google`로 리다이렉션된다.
3. Google 로그인 페이지가 열리고 사용자가 Google 계정으로 로그인한다.
4. Google이 콜백 URL(`/api/v1/auth/oauth2/callback/google?code=...&state=...`)로 인증 코드를 반환한다.
5. 백엔드가 인증 코드로 Google Access Token을 요청하고 사용자 정보를 조회한다.
6. 이메일로 기존 User를 검색하고, 없으면 신규 User를 생성한다.
7. UserIdentity 레코드를 생성하여 Google 프로바이더 ID를 저장한다.
8. JWT AccessToken과 RefreshToken을 발급하고 프론트엔드로 리다이렉션한다.
9. 프론트엔드가 토큰을 저장하고 대시보드로 이동한다.

### 시나리오 2: 기존 이메일/비밀번호 계정에 Google 계정 연동

1. 사용자가 이메일/비밀번호로 이미 가입한 상태에서 Google 로그인을 시도한다.
2. Google 인증이 완료되고 백엔드가 사용자 정보를 조회한다.
3. 이메일이 기존 User 레코드와 일치함을 확인한다.
4. UserIdentity 테이블에 Google 프로바이더 정보를 추가한다.
5. 토큰을 발급하고 로그인을 완료한다.
6. 프론트엔드에서 "Google 계정이 연동되었습니다" 토스트 메시지를 표시한다.

### 시나리오 3: 이미 Google 계정이 연동된 사용자의 재로그인

1. 사용자가 로그인 페이지에서 "Google로 로그인" 버튼을 클릭한다.
2. Google 인증이 완료되고 콜백이 호출된다.
3. 백엔드가 Google 프로바이더 ID로 UserIdentity를 조회한다.
4. 연결된 User를 찾고 JWT 토큰을 발급한다.
5. 프론트엔드가 대시보드로 이동한다.

### 시나리오 4: 소셜 계정 연동 해제

1. 사용자가 프로필 설정 페이지에서 "계정 연동 관리" 섹션으로 이동한다.
2. 연동된 Google 계정 옆에 "연동 해제" 버튼이 표시된다.
3. 사용자가 "연동 해제" 버튼을 클릭한다.
4. 확인 모달이 표시된다: "Google 계정 연동을 해제하시겠습니까?"
5. "확인" 클릭 시 `DELETE /api/v1/auth/oauth2/identities/{identityId}` 호출된다.
6. 백엔드가 UserIdentity 레코드를 삭제한다.
7. "Google 계정 연동이 해제되었습니다" 토스트 메시지가 표시된다.

### 시나리오 5: OAuth2 인증 실패

1. 사용자가 Google 로그인을 시도하지만 Google 인증 페이지에서 "취소" 버튼을 클릭한다.
2. Google이 콜백 URL에 `error=access_denied` 파라미터를 전달한다.
3. 백엔드가 에러를 감지하고 프론트엔드로 에러 메시지와 함께 리다이렉션한다.
4. 프론트엔드가 "Google 로그인이 취소되었습니다" 토스트를 표시한다.
5. 사용자는 다시 이메일/비밀번호 로그인을 시도할 수 있다.

### 시나리오 6: 여러 소셜 계정 연동 (향후 확장)

1. 사용자가 이미 Google 계정이 연동된 상태에서 Kakao 로그인을 시도한다.
2. Kakao 인증이 완료되고 백엔드가 사용자 정보를 조회한다.
3. 이메일이 기존 User와 일치하면 Kakao UserIdentity 레코드를 추가한다.
4. 프로필 설정 페이지에서 Google과 Kakao 두 개의 연동된 계정이 표시된다.

## 4. 디자인 가이드라인

### 로그인 페이지 레이아웃

```
┌─────────────────────────────────────────────────────┐
│                  Modern Kanban Service              │
│                                                       │
│  ┌───────────────────────────────────────────────┐  │
│  │ 로그인                                         │  │
│  ├───────────────────────────────────────────────┤  │
│  │ 이메일                                         │  │
│  │ [___________________________________]          │  │
│  │                                                │  │
│  │ 비밀번호                                       │  │
│  │ [___________________________________]          │  │
│  │                                                │  │
│  │        [로그인]                                │  │
│  │                                                │  │
│  │ ─────────── 또는 ───────────                  │  │
│  │                                                │  │
│  │  [🔵 Google로 로그인]                         │  │
│  │                                                │  │
│  │  (향후: Kakao, Naver 버튼 추가)               │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### Google 로그인 버튼 스타일

**Google 브랜드 가이드라인 준수:**
- 버튼 배경: `#4285F4` (Google Blue)
- 텍스트: `#FFFFFF` (흰색)
- 아이콘: Google "G" 로고 (SVG)
- 폰트: `Roboto` 또는 시스템 기본 폰트
- 높이: `44px` (터치 최적화)
- 패딩: `12px 16px`
- Border Radius: `4px`
- Hover: `#357AE8` (약간 어두운 파란색)

**버튼 HTML 구조:**
```
┌────────────────────────────────────┐
│ [G 아이콘]  Google로 로그인       │
└────────────────────────────────────┘
```

### 프로필 설정 - 계정 연동 관리 섹션

```
┌────────────────────────────────────────────────────┐
│ 프로필 설정                                        │
├────────────────────────────────────────────────────┤
│ 계정 연동 관리                                     │
├────────────────────────────────────────────────────┤
│                                                      │
│ 연동된 계정:                                       │
│                                                      │
│ ┌──────────────────────────────────────────────┐ │
│ │ [G] Google                                   │ │
│ │ example@gmail.com                            │ │
│ │                               [연동 해제]    │ │
│ └──────────────────────────────────────────────┘ │
│                                                      │
│ ┌──────────────────────────────────────────────┐ │
│ │ [K] Kakao (연동되지 않음)                    │ │
│ │                               [연동하기]     │ │
│ └──────────────────────────────────────────────┘ │
│                                                      │
└────────────────────────────────────────────────────┘
```

### 계정 연동 확인 모달

```
┌────────────────────────────────────┐
│ Google 계정 연동 확인              │
├────────────────────────────────────┤
│                                      │
│ Google 계정이 성공적으로           │
│ 연동되었습니다.                    │
│                                      │
│ 이메일: example@gmail.com          │
│                                      │
│          [확인]                     │
│                                      │
└────────────────────────────────────┘
```

### 연동 해제 확인 모달

```
┌────────────────────────────────────┐
│ 계정 연동 해제 확인                │
├────────────────────────────────────┤
│                                      │
│ Google 계정 연동을                 │
│ 해제하시겠습니까?                  │
│                                      │
│ example@gmail.com                  │
│                                      │
│    [취소]        [확인]            │
│                                      │
└────────────────────────────────────┘
```

### 에러 메시지 스타일

**OAuth2 인증 실패:**
```
┌─────────────────────────────────────┐
│ ⚠️ Google 로그인이 취소되었습니다 │ ← 토스트 (3초 후 사라짐)
└─────────────────────────────────────┘
```

**네트워크 오류:**
```
┌─────────────────────────────────────┐
│ ⚠️ 네트워크 오류가 발생했습니다   │
│    다시 시도해주세요                │
└─────────────────────────────────────┘
```

**이메일 정보 없음:**
```
┌─────────────────────────────────────┐
│ ⚠️ Google 계정에서 이메일 정보를  │
│    가져올 수 없습니다               │
└─────────────────────────────────────┘
```

## 5. 프론트엔드 규격

### 페이지/컴포넌트 구조

#### 1. LoginPage.tsx (수정)

**변경점:**
- Google 로그인 버튼 추가
- OAuth2 콜백 처리 로직 추가

**추가 상태:**
```typescript
const [oauthLoading, setOauthLoading] = useState(false);
const [oauthError, setOauthError] = useState<string | null>(null);
```

**Google 로그인 버튼:**
```typescript
<button
  onClick={handleGoogleLogin}
  disabled={oauthLoading}
  className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-[#4285F4] text-white rounded-md hover:bg-[#357AE8] transition-colors disabled:opacity-50"
>
  <GoogleIcon className="w-5 h-5" />
  <span>Google로 로그인</span>
</button>
```

**Google 로그인 핸들러:**
```typescript
const handleGoogleLogin = () => {
  setOauthLoading(true);
  // Spring Security OAuth2 Client의 authorization endpoint로 리다이렉션
  window.location.href = `${API_BASE_URL}/api/v1/auth/oauth2/authorization/google`;
};
```

**OAuth2 콜백 처리:**
```typescript
useEffect(() => {
  // URL 파라미터에서 토큰 추출 (콜백 후 리다이렉션된 경우)
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('token');
  const error = urlParams.get('error');

  if (token) {
    // 토큰 저장
    localStorage.setItem('accessToken', token);
    // 사용자 정보 조회
    fetchCurrentUser();
    // 대시보드로 이동
    navigate('/dashboard');
  } else if (error) {
    setOauthError(decodeURIComponent(error));
    showToast('로그인에 실패했습니다', 'error');
  }
}, []);
```

#### 2. ProfileSettingsPage.tsx (신규 생성)

**역할:** 사용자 프로필 및 연동된 소셜 계정 관리

**Props:**
```typescript
interface ProfileSettingsPageProps {
  // 없음 (독립 페이지)
}
```

**상태:**
```typescript
const [userIdentities, setUserIdentities] = useState<UserIdentity[]>([]);
const [loading, setLoading] = useState(false);
const [unlinkingIdentityId, setUnlinkingIdentityId] = useState<number | null>(null);
```

**UserIdentity 인터페이스:**
```typescript
interface UserIdentity {
  id: number;
  provider: 'GOOGLE' | 'KAKAO' | 'NAVER';
  providerUserId: string;
  email: string;
  name?: string;
  createdAt: string;
}
```

**연동된 계정 목록 조회:**
```typescript
useEffect(() => {
  fetchUserIdentities();
}, []);

const fetchUserIdentities = async () => {
  setLoading(true);
  try {
    const response = await authService.getUserIdentities();
    setUserIdentities(response.data);
  } catch (error) {
    showToast('계정 정보를 불러오는데 실패했습니다', 'error');
  } finally {
    setLoading(false);
  }
};
```

**연동 해제 핸들러:**
```typescript
const handleUnlinkIdentity = async (identityId: number) => {
  if (!confirm('이 소셜 계정 연동을 해제하시겠습니까?')) {
    return;
  }

  setUnlinkingIdentityId(identityId);
  try {
    await authService.unlinkIdentity(identityId);
    setUserIdentities(prev => prev.filter(identity => identity.id !== identityId));
    showToast('계정 연동이 해제되었습니다', 'success');
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || '연동 해제에 실패했습니다';
    showToast(errorMessage, 'error');
  } finally {
    setUnlinkingIdentityId(null);
  }
};
```

**소셜 계정 연동 핸들러:**
```typescript
const handleLinkProvider = (provider: string) => {
  // OAuth2 인증 시작 (기존 로그인과 동일)
  window.location.href = `${API_BASE_URL}/api/v1/auth/oauth2/authorization/${provider.toLowerCase()}`;
};
```

#### 3. authService.ts (수정)

**추가 API 메서드:**

```typescript
// 사용자 연동 계정 목록 조회
getUserIdentities: async (): Promise<AxiosResponse<UserIdentity[]>> => {
  return apiClient.get('/api/v1/auth/me/identities');
},

// 소셜 계정 연동 해제
unlinkIdentity: async (identityId: number): Promise<AxiosResponse<void>> => {
  return apiClient.delete(`/api/v1/auth/oauth2/identities/${identityId}`);
},
```

### 패키지 설치

**추가 패키지 없음** (기존 React Router, Axios 사용)

### 라우팅 추가

**App.tsx:**
```typescript
<Route path="/profile/settings" element={<ProtectedRoute><ProfileSettingsPage /></ProtectedRoute>} />
<Route path="/auth/callback" element={<OAuth2CallbackHandler />} />
```

**OAuth2CallbackHandler.tsx (신규 생성):**

콜백 URL에서 토큰을 처리하는 전용 컴포넌트

```typescript
const OAuth2CallbackHandler: React.FC = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    const error = urlParams.get('error');

    if (token) {
      localStorage.setItem('accessToken', token);
      showToast('로그인에 성공했습니다', 'success');
      navigate('/dashboard');
    } else if (error) {
      showToast(decodeURIComponent(error), 'error');
      navigate('/login');
    } else {
      navigate('/login');
    }
  }, [navigate]);

  return (
    <div className="flex items-center justify-center h-screen">
      <p>로그인 처리 중...</p>
    </div>
  );
};
```

### 상태 관리

**현재 상태 관리 방식 유지:**
- 로컬 state로 userIdentities 관리
- JWT 토큰은 localStorage에 저장
- 변경사항 없음

### 에러 처리

**OAuth2 인증 실패:**
```typescript
// 콜백 URL에 error 파라미터가 있는 경우
const error = urlParams.get('error');
if (error === 'access_denied') {
  showToast('Google 로그인이 취소되었습니다', 'error');
} else if (error === 'email_not_provided') {
  showToast('Google 계정에서 이메일 정보를 가져올 수 없습니다', 'error');
} else {
  showToast('로그인에 실패했습니다. 다시 시도해주세요', 'error');
}
```

**연동 해제 실패:**
```typescript
try {
  await authService.unlinkIdentity(identityId);
} catch (error: any) {
  if (error.response?.status === 400) {
    showToast('최소 하나의 로그인 방법을 유지해야 합니다', 'error');
  } else {
    showToast('연동 해제에 실패했습니다', 'error');
  }
}
```

## 6. 백엔드 규격

### 데이터 모델

#### UserIdentity 엔티티

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| user_id | Long | User FK |
| provider | Enum(`GOOGLE`, `KAKAO`, `NAVER`) | OAuth2 프로바이더 |
| provider_user_id | String(255) | 프로바이더의 사용자 고유 ID |
| email | String(150) | 프로바이더에서 제공한 이메일 |
| name | String(100) | 프로바이더에서 제공한 이름 |
| created_at / updated_at | DateTime | |

**제약조건:**
- `UNIQUE(provider, provider_user_id)`: 프로바이더당 사용자 ID는 고유해야 함
- `INDEX(user_id)`: User 조회 성능 최적화
- `INDEX(provider, provider_user_id)`: 소셜 로그인 조회 성능 최적화

**관계:**
```
User 1 ──── * UserIdentity
```

#### User 엔티티 변경

**수정사항:**
- `password_hash` 필드를 nullable로 변경 (소셜 로그인 전용 사용자 지원)

```sql
ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL;
```

### API 엔드포인트

#### 1. OAuth2 인증 시작

```
GET /api/v1/auth/oauth2/authorization/{provider}
```

**파라미터:**
- `provider`: `google`, `kakao`, `naver` 등

**동작:**
- Spring Security OAuth2 Client가 자동으로 처리
- OAuth2 프로바이더의 인증 페이지로 리다이렉션
- `state` 파라미터로 CSRF 방지

**리다이렉션 URL:**
```
https://accounts.google.com/o/oauth2/v2/auth?
  client_id={CLIENT_ID}&
  redirect_uri={REDIRECT_URI}&
  response_type=code&
  scope=openid email profile&
  state={CSRF_TOKEN}
```

#### 2. OAuth2 콜백 처리

```
GET /api/v1/auth/oauth2/callback/{provider}
```

**파라미터:**
- `provider`: `google`, `kakao`, `naver` 등
- `code`: OAuth2 인증 코드 (query parameter)
- `state`: CSRF 토큰 (query parameter)
- `error`: 인증 실패 시 에러 코드 (optional)

**응답 (성공):**
- 프론트엔드 콜백 페이지로 리다이렉션
- URL: `/auth/callback?token={JWT_ACCESS_TOKEN}`

**응답 (실패):**
- 프론트엔드 로그인 페이지로 리다이렉션
- URL: `/login?error={ERROR_MESSAGE}`

**서버 내부 처리 플로우:**
1. `code`로 OAuth2 Access Token 요청
2. Access Token으로 사용자 정보 조회 (이메일, 이름, 프로필 이미지)
3. `provider_user_id`로 UserIdentity 조회
4. UserIdentity 존재 → 연결된 User로 로그인
5. UserIdentity 없음 → 이메일로 User 검색
   - User 존재 → UserIdentity 생성 후 연동
   - User 없음 → 신규 User 생성 후 UserIdentity 생성
6. JWT AccessToken + RefreshToken 발급
7. 프론트엔드로 리다이렉션

#### 3. 사용자 연동 계정 목록 조회

```
GET /api/v1/auth/me/identities
```

**인증:** JWT 필수

**응답:**
```json
[
  {
    "id": 1,
    "provider": "GOOGLE",
    "providerUserId": "1234567890",
    "email": "user@gmail.com",
    "name": "홍길동",
    "createdAt": "2024-11-14T10:00:00"
  },
  {
    "id": 2,
    "provider": "KAKAO",
    "providerUserId": "9876543210",
    "email": "user@kakao.com",
    "name": "홍길동",
    "createdAt": "2024-11-15T14:30:00"
  }
]
```

#### 4. 소셜 계정 연동 해제

```
DELETE /api/v1/auth/oauth2/identities/{identityId}
```

**인증:** JWT 필수

**파라미터:**
- `identityId`: UserIdentity ID

**응답 (성공):**
```
204 No Content
```

**응답 (실패):**
```json
{
  "status": 400,
  "message": "최소 하나의 로그인 방법을 유지해야 합니다",
  "timestamp": "2024-11-14T10:00:00"
}
```

**검증 로직:**
- 사용자가 소유한 UserIdentity인지 확인
- 연동 해제 후 남은 인증 수단이 있는지 확인 (password 또는 다른 UserIdentity)

### DTO 정의

#### OAuth2UserInfo (내부 인터페이스)

```
provider: String (GOOGLE, KAKAO, NAVER)
providerId: String (프로바이더 사용자 고유 ID)
email: String
name: String
profileImageUrl: String (optional)
```

#### UserIdentityResponse

```
id: Long
provider: String (GOOGLE, KAKAO, NAVER)
providerUserId: String
email: String
name: String
createdAt: LocalDateTime
```

### 서비스 로직

#### OAuth2AuthenticationSuccessHandler

**역할:** OAuth2 인증 성공 후 처리

**플로우:**
1. OAuth2User에서 사용자 정보 추출
2. 프로바이더별로 정보 매핑 (`OAuth2UserInfoFactory`)
3. UserIdentity 조회 또는 생성
4. User 조회 또는 생성
5. JWT 토큰 발급
6. 프론트엔드로 리다이렉션 (`/auth/callback?token={JWT}`)

#### OAuth2UserInfoFactory

**역할:** 프로바이더별 사용자 정보 응답을 통일된 인터페이스로 변환

**Google 응답 예시:**
```json
{
  "sub": "1234567890",
  "email": "user@gmail.com",
  "name": "홍길동",
  "picture": "https://lh3.googleusercontent.com/..."
}
```

**매핑 로직:**
```
provider: GOOGLE
providerId: attributes.get("sub")
email: attributes.get("email")
name: attributes.get("name")
profileImageUrl: attributes.get("picture")
```

#### UserIdentityService

**주요 메서드:**

```
findByProviderAndProviderId(provider, providerId): Optional<UserIdentity>
findByUserId(userId): List<UserIdentity>
createUserIdentity(user, oauth2UserInfo): UserIdentity
deleteUserIdentity(identityId, currentUser): void
```

**연동 해제 검증 로직:**
```
1. UserIdentity 소유자 확인
2. 남은 인증 수단 확인:
   - User.passwordHash가 존재하면 OK
   - 다른 UserIdentity가 존재하면 OK
   - 둘 다 없으면 BadRequestException
3. UserIdentity 삭제
```

### Spring Security OAuth2 설정

#### application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - openid
              - email
              - profile
            redirect-uri: "{baseUrl}/api/v1/auth/oauth2/callback/google"
            authorization-grant-type: authorization_code
            client-name: Google

        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
            user-name-attribute: sub
```

**환경 변수:**
- `GOOGLE_CLIENT_ID`: Google Cloud Console에서 발급받은 Client ID
- `GOOGLE_CLIENT_SECRET`: Google Cloud Console에서 발급받은 Client Secret

#### SecurityConfig 수정

```
OAuth2 로그인 활성화:
- /api/v1/auth/oauth2/** 엔드포인트 허용
- successHandler: OAuth2AuthenticationSuccessHandler
- failureHandler: OAuth2AuthenticationFailureHandler
```

## 7. 보안 처리

### CSRF 방지

**OAuth2 state 파라미터:**
- Spring Security가 자동으로 `state` 파라미터 생성
- 콜백 시 state 검증하여 CSRF 공격 방지

### 토큰 보안

**JWT 발급:**
- OAuth2 인증 성공 시 기존과 동일한 JWT AccessToken + RefreshToken 발급
- 토큰 payload에 `sub`, `email`, `roles` 포함

**HTTPS 필수:**
- OAuth2 콜백 URL은 HTTPS만 허용
- HTTP로 콜백하면 보안 경고 발생

### 이메일 검증

**Google OAuth2 scope:**
- `openid`, `email`, `profile` 필수
- 이메일 정보가 없으면 인증 실패 처리

**이메일 중복 방지:**
- UserIdentity 생성 시 이메일로 기존 User 검색
- 이메일 일치 시 자동 연동, 불일치 시 신규 User 생성

### UserIdentity 무결성

**제약조건:**
- `UNIQUE(provider, provider_user_id)`: 동일 프로바이더에서 같은 사용자 ID 중복 방지
- User 삭제 시 연관된 UserIdentity도 CASCADE DELETE

### 소셜 계정 탈취 방지

**OAuth2 Access Token 저장 여부:**
- 현재는 JWT만 발급하고 OAuth2 Access Token은 저장하지 않음
- 향후 프로필 동기화 필요 시 암호화하여 DB 저장 고려

## 8. 수용 기준

1. 사용자가 로그인 페이지에서 "Google로 로그인" 버튼을 클릭하면 Google 인증 페이지로 이동한다.
2. Google 인증 완료 후 콜백이 정상 처리되고 JWT 토큰이 발급된다.
3. 처음 Google 로그인하는 사용자는 자동으로 User 레코드와 UserIdentity 레코드가 생성된다.
4. 이메일이 일치하는 기존 계정에 Google 계정이 자동으로 연동된다.
5. 프로필 설정 페이지에서 연동된 소셜 계정 목록을 확인할 수 있다.
6. 연동된 소셜 계정을 해제할 수 있다 (단, 최소 하나의 인증 수단은 유지).
7. Google 인증 실패 시 명확한 에러 메시지가 표시되고 로그인 페이지로 돌아간다.
8. OAuth2 state 파라미터로 CSRF 공격이 방지된다.
9. Google 외에도 Kakao, Naver 프로바이더 추가 시 최소한의 코드 수정으로 확장 가능하다.
10. 소셜 로그인 콜백 처리 시간이 1초 이내이다.

## 9. 구현 순서

### Phase 1: 데이터베이스 및 엔티티 설정 (1.5일)

- [ ] `UserIdentity` 엔티티 생성
- [ ] `User.password_hash` nullable 변경 마이그레이션
- [ ] `UserIdentityRepository` 생성
- [ ] 제약조건 및 인덱스 추가 (`UNIQUE(provider, provider_user_id)`)
- [ ] Unit Tests (엔티티 및 Repository)

### Phase 2: OAuth2 설정 및 Google 통합 (2일)

- [ ] Spring Security OAuth2 Client 의존성 추가
- [ ] `application.yml` OAuth2 설정 (Google)
- [ ] Google Cloud Console에서 OAuth2 Credentials 발급
- [ ] 환경 변수 설정 (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`)
- [ ] `SecurityConfig` OAuth2 로그인 활성화
- [ ] `/api/v1/auth/oauth2/authorization/google` 테스트
- [ ] `/api/v1/auth/oauth2/callback/google` 테스트

### Phase 3: OAuth2 인증 핸들러 구현 (2일)

- [ ] `OAuth2UserInfo` 인터페이스 정의
- [ ] `OAuth2UserInfoFactory` 생성 (Google 지원)
- [ ] `OAuth2AuthenticationSuccessHandler` 구현
  - [ ] UserIdentity 조회 또는 생성
  - [ ] User 조회 또는 생성
  - [ ] JWT 토큰 발급
  - [ ] 프론트엔드로 리다이렉션
- [ ] `OAuth2AuthenticationFailureHandler` 구현
- [ ] Unit Tests (핸들러 로직)
- [ ] Integration Tests (OAuth2 플로우)

### Phase 4: UserIdentity 서비스 및 API 구현 (1.5일)

- [ ] `UserIdentityService` 생성
- [ ] `getUserIdentities()` 메서드 구현
- [ ] `deleteUserIdentity()` 메서드 구현 (검증 로직 포함)
- [ ] `UserIdentityController` 생성
  - [ ] `GET /api/v1/auth/me/identities`
  - [ ] `DELETE /api/v1/auth/oauth2/identities/{identityId}`
- [ ] Unit Tests (서비스 로직)
- [ ] Integration Tests (API)

### Phase 5: 프론트엔드 로그인 페이지 수정 (1.5일)

- [ ] `LoginPage.tsx` Google 로그인 버튼 추가
- [ ] `handleGoogleLogin()` 핸들러 구현
- [ ] `OAuth2CallbackHandler.tsx` 생성
- [ ] 콜백 URL 처리 로직 구현
- [ ] 라우팅 설정 (`/auth/callback`)
- [ ] 에러 처리 및 토스트 메시지
- [ ] UI 테스트 (버튼 클릭, 리다이렉션)

### Phase 6: 프로필 설정 페이지 생성 (2일)

- [ ] `ProfileSettingsPage.tsx` 생성
- [ ] 연동된 계정 목록 조회 API 호출
- [ ] 계정 연동 해제 기능 구현
- [ ] 확인 모달 추가
- [ ] 에러 처리 및 토스트 메시지
- [ ] 반응형 디자인 적용
- [ ] UI 테스트

### Phase 7: 테스트 및 검증 (1.5일)

- [ ] E2E 테스트 (Playwright)
  - [ ] Google 로그인 플로우
  - [ ] 계정 연동
  - [ ] 연동 해제
  - [ ] 에러 시나리오
- [ ] 브라우저 호환성 테스트
- [ ] 보안 테스트 (CSRF, 토큰 탈취)
- [ ] 성능 테스트 (콜백 처리 시간)
- [ ] 버그 수정 및 최종 검증

**총 소요 시간: ~12일**

## 10. 위험 요소 및 완화 전략

| 위험 | 영향 | 완화 전략 |
|------|------|----------|
| OAuth2 프로바이더 서비스 장애 | 로그인 불가 | 이메일/비밀번호 로그인 병행 제공, 에러 메시지 명확화 |
| 이메일 정보 누락 | 계정 생성 실패 | OAuth2 scope에 email 필수 설정, 에러 처리 |
| CSRF 공격 | 계정 탈취 | OAuth2 state 파라미터 검증, HTTPS 필수 |
| 중복 계정 생성 | 데이터 무결성 저하 | 이메일 기반 자동 연동, UNIQUE 제약조건 |
| 프로바이더별 응답 형식 차이 | 매핑 오류 | OAuth2UserInfoFactory 패턴, 프로바이더별 구현 분리 |
| 소셜 계정 탈취 | 보안 취약점 | 2FA 도입 고려, 의심스러운 로그인 감지 |
| 최소 인증 수단 미확보 | 계정 잠금 | 연동 해제 시 검증 로직, 경고 메시지 |

## 11. 테스트 전략

### Unit Tests

**백엔드:**
- `OAuth2UserInfoFactory`:
  - Google 응답 매핑 테스트
  - 이메일 누락 시 예외 발생 확인
- `UserIdentityService`:
  - `createUserIdentity()` 정상 동작
  - `deleteUserIdentity()` 검증 로직 (최소 인증 수단 확인)
  - `findByProviderAndProviderId()` 조회 성공/실패
- `OAuth2AuthenticationSuccessHandler`:
  - UserIdentity 조회 또는 생성
  - User 조회 또는 생성
  - JWT 토큰 발급

**프론트엔드:**
- `OAuth2CallbackHandler`:
  - 토큰 파라미터 파싱
  - 에러 파라미터 처리
  - 리다이렉션 로직
- `ProfileSettingsPage`:
  - 연동 계정 목록 렌더링
  - 연동 해제 버튼 클릭

### Integration Tests

**백엔드:**
```
1. GET /api/v1/auth/oauth2/authorization/google
   - 302 리다이렉션 확인
   - Google 인증 URL 검증

2. GET /api/v1/auth/oauth2/callback/google?code=...&state=...
   - Mock OAuth2 서버로 테스트
   - UserIdentity 생성 확인
   - User 생성 확인
   - JWT 토큰 발급 확인
   - 프론트엔드 리다이렉션 URL 검증

3. GET /api/v1/auth/me/identities
   - 인증된 사용자의 UserIdentity 목록 반환
   - 권한 없는 사용자 401 반환

4. DELETE /api/v1/auth/oauth2/identities/{identityId}
   - 정상 삭제 → 204 No Content
   - 최소 인증 수단 위반 → 400 Bad Request
   - 권한 없음 → 403 Forbidden
```

**프론트엔드:**
```
1. 로그인 페이지 렌더링
   - Google 로그인 버튼 표시 확인
2. Google 로그인 버튼 클릭
   - OAuth2 인증 URL로 리다이렉션 확인
3. 콜백 처리
   - 토큰 저장 확인
   - 대시보드로 이동 확인
```

### E2E Tests (Playwright)

```
시나리오 1: 신규 사용자 Google 로그인
  1. 로그인 페이지 접속
  2. "Google로 로그인" 클릭
  3. Google Mock 인증 페이지에서 승인
  4. 콜백 처리 후 대시보드로 이동 확인
  5. 프로필 설정에서 Google 계정 연동 확인

시나리오 2: 기존 사용자 Google 계정 연동
  1. 이메일/비밀번호로 로그인
  2. 프로필 설정 페이지로 이동
  3. "Google 연동하기" 클릭
  4. Google 인증 완료
  5. 프로필 설정에서 Google 계정 추가 확인

시나리오 3: Google 계정 연동 해제
  1. 로그인 (Google + 이메일/비밀번호 모두 연동된 상태)
  2. 프로필 설정 페이지로 이동
  3. Google 계정 "연동 해제" 클릭
  4. 확인 모달에서 "확인" 클릭
  5. Google 계정 제거 확인
  6. 이메일/비밀번호로 재로그인 가능 확인

시나리오 4: OAuth2 인증 취소
  1. "Google로 로그인" 클릭
  2. Google 인증 페이지에서 "취소" 클릭
  3. 로그인 페이지로 돌아옴
  4. 에러 토스트 표시 확인

시나리오 5: 최소 인증 수단 위반
  1. 소셜 계정만으로 가입한 사용자로 로그인
  2. 프로필 설정에서 유일한 소셜 계정 연동 해제 시도
  3. 에러 메시지 표시: "최소 하나의 로그인 방법을 유지해야 합니다"
```

### 성능 테스트

- **OAuth2 콜백 처리**: < 1초
- **JWT 토큰 발급**: < 500ms
- **UserIdentity 조회**: < 100ms

### 보안 테스트

- **CSRF 공격**: state 파라미터 변조 시도 → 401 반환
- **토큰 탈취**: 타인의 identityId로 연동 해제 시도 → 403 반환
- **이메일 누락**: Google scope에서 email 제거 → 에러 처리

## 12. Notes

- **Google Cloud Console 설정**:
  - OAuth2 Credentials 생성 시 Redirect URI에 `http://localhost:8080/api/v1/auth/oauth2/callback/google` 추가 (개발)
  - 프로덕션 배포 시 HTTPS URL로 변경 필수

- **프로바이더 추가 시**:
  - `application.yml`에 새 프로바이더 설정 추가
  - `OAuth2UserInfoFactory`에 프로바이더별 매핑 로직 추가
  - 프론트엔드에 프로바이더별 로그인 버튼 추가
  - 최소 코드 수정으로 확장 가능

- **기존 데이터 마이그레이션**:
  - 기존 이메일/비밀번호 사용자는 영향 없음
  - `password_hash` nullable 변경만 필요

- **소셜 계정 프로필 동기화**:
  - 현재 단계에서는 제외
  - 향후 OAuth2 Access Token을 저장하여 주기적으로 프로필 정보 업데이트 고려

- **다중 소셜 계정 병합**:
  - 현재는 이메일 기반 자동 연동만 지원
  - 향후 사용자가 수동으로 계정 병합 기능 추가 고려

- **소셜 로그인 전용 사용자의 비밀번호 설정**:
  - Priority 3 이상에서 구현
  - 프로필 설정에서 "비밀번호 설정" 기능 추가

## 13. Related Documents

- `../../requirements/Priority-2/enhance-user-authentication-with-social-login.md` - 요구사항 정의
- `../Priority-1/model-auth-000.md` - 인증·인가 설계
- `../Priority-1/spec-auth-000.md` - 인증·세션 PRD
- `../Priority-1/api-spec.md` - API 명세 참조
- `CLAUDE.md` - 프로젝트 전체 가이드라인
