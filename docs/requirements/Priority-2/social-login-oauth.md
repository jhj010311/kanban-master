# Priority 2 요구사항 – Social Login (OAuth 2.0) 통합

## 목적

칸반 보드 서비스의 사용자 인증 시스템을 확장하여 **Google, GitHub 등 소셜 로그인(OAuth 2.0)을 지원**한다. 이를 통해 사용자는 번거로운 회원가입 절차 없이 기존 소셜 계정으로 빠르게 서비스에 접근할 수 있으며, 비밀번호 관리 부담을 줄이고 보안성을 향상시킬 수 있다.

## 배경

### 현재 인증 시스템

- **Email/Password 기반 전통적 인증**: 이메일과 비밀번호를 입력하여 회원가입 및 로그인
- **JWT 기반 토큰 인증**: Access Token (15분) + Refresh Token (14일) Dual Token 전략
- **Spring Security + BCrypt**: 비밀번호 해싱 및 보안 처리
- **자동 워크스페이스 생성**: 신규 가입 시 개인 워크스페이스 자동 생성

### 개선 필요성

1. **사용자 편의성**: 복잡한 비밀번호 생성 및 관리 부담 제거
2. **가입 전환율 향상**: 원클릭 로그인으로 가입 장벽 낮춤
3. **보안 강화**: OAuth 2.0 프로바이더의 보안 인프라 활용
4. **글로벌 표준 준수**: 대부분의 현대 웹 서비스가 채택한 인증 방식

## 범위

### 포함

- Google OAuth 2.0 로그인 구현 (최우선)
- 확장 가능한 Multi-Provider 아키텍처 (GitHub, Kakao, Naver 등 향후 추가 가능)
- 신규 사용자 OAuth 가입 플로우
- 기존 Email/Password 인증 시스템과의 공존
- 같은 이메일로 여러 Provider 연결 지원
- OAuth 사용자에 대한 JWT 발급 통합
- 자동 워크스페이스 생성 (기존 로직 유지)

### 제외

- SAML 기반 엔터프라이즈 SSO (Priority 3 이상으로 미룸)
- 계정 연결/해제 UI (Phase 2로 미룸, 백엔드 인프라는 구축)
- 다중 계정 동시 로그인 (현재 단일 세션만 지원)
- OAuth Token을 이용한 외부 API 호출 (예: Google Drive 연동)

## 이해관계자

### 사용자 페르소나

1. **신규 사용자 (김신규)**
   - 역할: 처음 칸반 보드 서비스를 이용하는 사용자
   - 니즈: 복잡한 회원가입 없이 빠르게 서비스를 시작하고 싶음
   - 시나리오: Google 계정으로 원클릭 가입 후 즉시 워크스페이스 생성

2. **기존 사용자 (박기존)**
   - 역할: 이미 이메일/비밀번호로 가입한 사용자
   - 니즈: 기존 계정을 유지하면서 소셜 로그인을 추가로 사용하고 싶음
   - 시나리오: 이메일 로그인 사용 중이지만, 나중에 Google 계정 연결 가능

3. **보안 담당자 (이보안)**
   - 역할: 서비스 보안을 관리하는 개발자
   - 니즈: OAuth 2.0 표준을 준수하고, CSRF 및 오픈 리다이렉션 등의 취약점 방지
   - 시나리오: 보안 감사 시 OAuth 구현이 업계 표준을 따르는지 확인

## 기능 요구사항

| ID | 제목 | 설명 | 우선순위 |
|----|------|------|----------|
| FR-06a | Google OAuth 로그인 | Google 계정으로 원클릭 로그인 및 회원가입 | Must |
| FR-06b | Multi-Provider 아키텍처 | 향후 GitHub, Kakao 등 추가 Provider를 쉽게 확장할 수 있는 구조 | Must |
| FR-06c | UserIdentity 테이블 | 외부 프로바이더 연동 정보를 저장하는 별도 테이블 생성 | Must |
| FR-06d | 계정 병합 정책 | 같은 이메일로 Local + OAuth 가입 시 동일 User로 병합 | Must |
| FR-06e | 기존 인증 시스템 유지 | 이메일/비밀번호 로그인 기능 유지 (LOCAL provider) | Must |
| FR-06f | JWT 발급 통합 | OAuth 로그인 성공 시에도 기존과 동일한 JWT 발급 | Must |
| FR-06g | 자동 워크스페이스 생성 | OAuth 신규 가입자에게도 기본 워크스페이스 자동 생성 | Must |
| FR-06h | 로그인 페이지 UI | 기존 로그인 폼 아래에 "Google로 로그인" 버튼 추가 | Must |
| FR-06i | OAuth 콜백 처리 | `/oauth2/callback/{provider}` 엔드포인트에서 토큰 발급 및 리다이렉트 | Must |
| FR-06j | 에러 처리 | OAuth 실패 시 사용자 친화적 에러 메시지 표시 | Should |
| FR-06k | 프로필 페이지 확장 | 연결된 계정 목록 표시 (Phase 2) | Could |
| FR-06l | 계정 연결/해제 | 기존 사용자가 소셜 계정을 추가/제거하는 기능 (Phase 2) | Could |

## 기술 요구사항

### 백엔드 (Spring Boot)

#### 1. 새로운 엔티티 및 Enum

**AuthProvider Enum**
```
LOCAL       - 이메일/비밀번호 가입
GOOGLE      - Google OAuth
GITHUB      - GitHub OAuth (Phase 2)
KAKAO       - Kakao OAuth (Phase 3)
NAVER       - Naver OAuth (Phase 3)
```

**UserIdentity 엔티티**
| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long | PK | 고유 식별자 |
| user_id | Long | FK to User, NOT NULL | 연결된 사용자 |
| provider | AuthProvider | NOT NULL | 프로바이더 종류 |
| provider_id | String(255) | NOT NULL | 외부 프로바이더의 사용자 고유 ID (Google UUID, GitHub user ID) |
| email | String(150) | NULLABLE | 프로바이더에서 받은 이메일 (User.email과 다를 수 있음) |
| linked_at | LocalDateTime | NOT NULL | 연결 시각 |

**제약 조건:**
- UNIQUE(user_id, provider): 한 사용자가 같은 프로바이더 중복 연결 방지
- UNIQUE(provider, provider_id): 같은 외부 계정이 여러 User에 연결되는 것 방지

#### 2. User 엔티티 수정

| 변경 사항 | Before | After |
|----------|--------|-------|
| password 필드 | `@Column(nullable = false)` | `@Column(nullable = true)` |
| 새 필드 추가 | N/A | `primary_provider: AuthProvider` (최초 가입 경로) |

**마이그레이션 전략:**
- 기존 사용자는 `primary_provider = LOCAL`, `password` 유지
- UserIdentity 테이블에 기존 사용자의 LOCAL identity 생성 (provider_id = user.email)

#### 3. Spring Security OAuth2 설정

**필요한 Gradle 의존성:**
```kotlin
implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
```

**application.yml 설정:**
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
              - email
              - profile
            redirect-uri: "{baseUrl}/api/v1/auth/oauth2/callback/google"
            authorization-grant-type: authorization_code
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
            user-name-attribute: sub
```

#### 4. 새로운 API 엔드포인트

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|----------|
| GET | `/api/v1/auth/oauth2/authorization/{provider}` | OAuth 로그인 시작 (Google 로그인 페이지로 리다이렉트) | No |
| GET | `/api/v1/auth/oauth2/callback/{provider}` | OAuth 콜백 처리 (Authorization Code → Access Token → JWT 발급) | No |
| POST | `/api/v1/auth/oauth2/link/{provider}` | 기존 계정에 소셜 계정 연결 (Phase 2) | Yes |
| DELETE | `/api/v1/auth/oauth2/unlink/{provider}` | 소셜 계정 연결 해제 (Phase 2) | Yes |
| GET | `/api/v1/auth/identities` | 현재 사용자의 연결된 계정 목록 조회 (Phase 2) | Yes |

#### 5. OAuth 처리 로직

**성공 플로우:**
1. 사용자가 "Google로 로그인" 버튼 클릭
2. `/oauth2/authorization/google`로 리다이렉트
3. Google 로그인 페이지에서 인증 및 동의
4. Google이 Authorization Code와 함께 `/oauth2/callback/google`로 콜백
5. Spring Security가 Authorization Code를 Google Access Token으로 교환
6. Google의 UserInfo API 호출하여 사용자 정보 획득 (email, name, sub)
7. **계정 병합 로직 실행**:
   - `provider_id`로 UserIdentity 조회
   - 존재하면: 해당 User 반환
   - 존재하지 않으면: `email`로 User 조회
     - User 존재: 기존 User에 새로운 UserIdentity 추가 (계정 병합)
     - User 없음: 신규 User 생성 + UserIdentity 생성 + 워크스페이스 생성
8. JWT Access Token + Refresh Token 발급
9. 프론트엔드로 리다이렉트: `/oauth2/redirect?token={accessToken}`

### 프론트엔드 (React + TypeScript)

#### 1. 로그인 페이지 수정

**LoginPage.tsx 변경:**
- 기존 이메일/비밀번호 폼 유지
- 폼 아래에 구분선 추가: "또는 소셜 계정으로 로그인"
- Google 로그인 버튼 추가 (Google 로고 + "Google로 로그인")
- 버튼 클릭 시: `window.location.href = '/api/v1/auth/oauth2/authorization/google'`

#### 2. OAuth 콜백 페이지

**신규 파일: `OAuthRedirectPage.tsx`**
- Route: `/oauth2/redirect`
- URL에서 `token` 파라미터 추출
- `authStorage.setAccessToken(token)`
- AuthContext의 `refreshProfile()` 호출
- Dashboard로 리다이렉트
- 에러 처리: `error` 파라미터가 있으면 로그인 페이지로 돌아가며 에러 메시지 표시

#### 3. 타입 정의 추가

**types/auth.ts 확장:**
```typescript
export enum AuthProvider {
  LOCAL = 'LOCAL',
  GOOGLE = 'GOOGLE',
  GITHUB = 'GITHUB',
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
  primaryProvider: AuthProvider;  // 신규 필드
  workspaces: WorkspaceMembership[];
  identities?: UserIdentity[];  // Phase 2에서 사용
}
```

## 보안 요구사항

### 1. CSRF 보호

- **State 파라미터**: OAuth 요청 시 랜덤 state 생성 및 세션 저장, 콜백에서 검증
- Spring Security OAuth2 Client가 자동으로 state 검증 수행

### 2. Redirect URI 검증

- **화이트리스트**: `application.yml`에 명시된 redirect-uri만 허용
- **오픈 리다이렉션 방지**: 사용자 입력 기반 리다이렉트 금지
- 프론트엔드 리다이렉트 URL도 화이트리스트 검증

### 3. 토큰 관리

- **Access Token**: 기존과 동일하게 localStorage 저장
- **Refresh Token**: HttpOnly 쿠키로 관리 (XSS 방지)
- **OAuth Access Token**: 백엔드에서만 사용, 프론트엔드 노출 금지

### 4. Scope 최소화

- **Google**: `email`, `profile`만 요청 (Drive, Calendar 등 불필요한 권한 요청 안 함)
- **GitHub**: `user:email`만 요청

### 5. 이메일 검증

- **Google**: `email_verified` 필드 확인 (true인 경우만 허용)
- **GitHub**: Primary email이면서 verified인 것만 사용

### 6. Rate Limiting

- OAuth 콜백 엔드포인트에 Rate Limiting 적용 (1분에 10회)
- 무차별 대입 공격 방지

## 데이터 모델 변경사항

### ERD (확장)

```
[User] 1 ──< N [UserIdentity]
  ↓
[AuthToken]
  ↓
[WorkspaceMembership]
```

**UserIdentity 테이블 DDL (PostgreSQL):**
```sql
CREATE TABLE user_identities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    email VARCHAR(150),
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_provider UNIQUE (user_id, provider),
    CONSTRAINT unique_provider_id UNIQUE (provider, provider_id)
);

CREATE INDEX idx_user_identities_user_id ON user_identities(user_id);
CREATE INDEX idx_user_identities_provider_id ON user_identities(provider, provider_id);
```

**User 테이블 ALTER (PostgreSQL):**
```sql
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
ALTER TABLE users ADD COLUMN primary_provider VARCHAR(20) DEFAULT 'LOCAL';
UPDATE users SET primary_provider = 'LOCAL' WHERE primary_provider IS NULL;
ALTER TABLE users ALTER COLUMN primary_provider SET NOT NULL;
```

**마이그레이션 스크립트 (기존 사용자 Identity 생성):**
```sql
INSERT INTO user_identities (user_id, provider, provider_id, email, linked_at)
SELECT id, 'LOCAL', email, email, created_at FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM user_identities ui
    WHERE ui.user_id = users.id AND ui.provider = 'LOCAL'
);
```

## UX·인터랙션 요구사항

### 로그인 페이지 UI

**레이아웃:**
```
┌─────────────────────────────────┐
│     칸반 보드 로그인            │
├─────────────────────────────────┤
│  [이메일 입력]                  │
│  [비밀번호 입력]                │
│  [로그인 버튼]                  │
│                                 │
│  ───── 또는 ─────               │
│                                 │
│  [G  Google로 로그인]           │
│  [GitHub으로 로그인] (Phase 2)  │
│                                 │
│  아직 계정이 없으신가요? 회원가입│
└─────────────────────────────────┘
```

**Google 버튼 스타일:**
- 배경: 흰색 (`bg-white`)
- 테두리: 연한 회색 (`border border-gray-300`)
- 아이콘: Google 공식 로고 (SVG)
- 텍스트: "Google로 로그인" (14px, font-semibold)
- Hover: 살짝 그림자 효과 (`hover:shadow-md`)

### OAuth 플로우 사용자 경험

1. **로그인 버튼 클릭**:
   - 즉시 Google 로그인 페이지로 이동 (새 탭 아님, 같은 탭)
   - 로딩 인디케이터는 표시하지 않음 (빠른 리다이렉트)

2. **Google 로그인 페이지**:
   - Google이 제공하는 표준 UI
   - 사용자가 계정 선택 및 동의

3. **콜백 처리 중**:
   - 로딩 스피너 표시: "로그인 중..."
   - 최대 5초 이내 완료 (백엔드 처리 시간)

4. **성공 시**:
   - Dashboard로 자동 이동
   - 환영 메시지: "환영합니다, {사용자명}님!" (Toast 알림)

5. **실패 시**:
   - 로그인 페이지로 복귀
   - 에러 메시지 표시:
     - "Google 로그인에 실패했습니다. 다시 시도해주세요."
     - "이미 다른 계정으로 사용 중인 이메일입니다." (이메일 충돌 시)

### 접근성 고려사항

- **키보드 네비게이션**: Tab 키로 Google 버튼 포커스 가능
- **스크린 리더**: `aria-label="Google 계정으로 로그인"`
- **명확한 버튼 텍스트**: "Google로 로그인" (아이콘만으로는 불충분)

## 비기능 요구사항

| 항목 | 기준 | 설명 |
|------|------|------|
| 성능 | 5초 이하 | OAuth 콜백부터 JWT 발급까지 5초 이내 완료 |
| 가용성 | 99.9% | Google OAuth 장애 시에도 Local 로그인은 정상 작동 |
| 확장성 | N개 Provider | 새로운 OAuth Provider 추가 시 코드 수정 최소화 (설정 파일만 수정) |
| 호환성 | OAuth 2.0 표준 | RFC 6749 준수 |
| 보안 | OWASP Top 10 | CSRF, 오픈 리다이렉션, 토큰 노출 방지 |
| 테스트 커버리지 | 80% 이상 | OAuth 핵심 로직 (계정 병합, JWT 발급) 단위 테스트 |

## 제약사항

### 기술 제약

- **Spring Boot 3.2**: OAuth2 Client 라이브러리 사용
- **PostgreSQL**: UserIdentity 테이블 저장
- **React 19**: 프론트엔드 구현
- **JWT 토큰 구조 유지**: 기존 토큰 검증 로직 호환

### 비즈니스 제약

- **Google OAuth 할당량**: 무료 티어에서 초당 10 요청 제한 (이후 유료 확장 고려)
- **이메일 필수**: OAuth 프로바이더가 이메일을 제공하지 않으면 가입 불가
- **중복 이메일 허용 안 함**: 같은 이메일은 하나의 User 계정으로만 존재

### 운영 제약

- **환경 변수 관리**: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`은 배포 환경마다 별도 설정
- **로컬 개발 환경**: Google OAuth 콜백이 localhost를 허용하도록 설정 필요
- **HTTPS 필수**: 프로덕션에서는 HTTPS만 허용 (OAuth 보안 요구사항)

## 구현 로드맵

### Phase 1: 데이터 모델 및 인프라 (1주)

1. **백엔드**:
   - AuthProvider enum 생성
   - UserIdentity 엔티티 생성
   - User 엔티티 수정 (password nullable, primary_provider 추가)
   - UserIdentityRepository 생성
   - DB 마이그레이션 스크립트 작성 및 실행

2. **테스트**:
   - UserIdentity CRUD 단위 테스트
   - 기존 User 테스트 케이스가 여전히 통과하는지 확인

### Phase 2: OAuth 백엔드 구현 (1주)

1. **의존성 추가**:
   - `spring-boot-starter-oauth2-client` 추가
   - Gradle 빌드 확인

2. **설정 파일**:
   - `application.yml`에 Google OAuth 설정 추가
   - `application-local.yml` 생성 (로컬 개발용)

3. **OAuth 서비스 로직**:
   - `OAuth2Service` 생성:
     - `processOAuthLogin(provider, oauthUser)` 메서드
     - 계정 병합 로직 (UserIdentity 조회 → User 조회 → 생성/연결)
     - 워크스페이스 자동 생성 (신규 사용자)
   - `OAuth2SuccessHandler` 생성:
     - JWT 발급
     - 프론트엔드로 리다이렉트 (`/oauth2/redirect?token=xxx`)
   - `OAuth2FailureHandler` 생성:
     - 에러 메시지와 함께 로그인 페이지로 리다이렉트

4. **Security 설정**:
   - `SecurityConfig`에 OAuth2 설정 추가
   - `/api/v1/auth/oauth2/**` 경로 허용

5. **테스트**:
   - OAuth 로그인 통합 테스트 (MockMvc + OAuth2 모킹)
   - 계정 병합 시나리오 테스트
   - JWT 발급 검증

### Phase 3: 프론트엔드 구현 (3일)

1. **UI 컴포넌트**:
   - Google 로고 SVG 추가 (`public/icons/google.svg`)
   - `GoogleLoginButton.tsx` 컴포넌트 생성
   - `LoginPage.tsx` 수정 (Google 버튼 추가)

2. **OAuth 콜백 처리**:
   - `OAuthRedirectPage.tsx` 생성
   - 토큰 추출 및 저장
   - AuthContext 갱신

3. **라우팅**:
   - `/oauth2/redirect` 라우트 추가

4. **타입 정의**:
   - `types/auth.ts`에 AuthProvider enum 추가
   - UserProfile에 primaryProvider 필드 추가

5. **테스트**:
   - Google 버튼 렌더링 테스트
   - OAuth 콜백 페이지 동작 테스트

### Phase 4: 테스트 및 배포 (2일)

1. **통합 테스트**:
   - 로컬 환경에서 Google OAuth 전체 플로우 테스트
   - 신규 가입 시나리오
   - 기존 이메일로 가입 시도 (계정 병합)
   - 에러 케이스 (OAuth 거부, 네트워크 오류)

2. **보안 점검**:
   - CSRF 검증 확인
   - Redirect URI 화이트리스트 확인
   - 토큰 저장 방식 점검

3. **문서화**:
   - Google OAuth 설정 가이드 작성
   - 환경 변수 설정 문서 업데이트
   - API 명세 업데이트

4. **배포**:
   - 스테이징 환경 배포 및 테스트
   - 프로덕션 배포
   - 모니터링 설정 (OAuth 성공/실패율)

## 확장 고려사항

### 추가 OAuth Provider (Phase 2)

1. **GitHub OAuth**:
   - `application.yml`에 GitHub 설정 추가
   - GitHub 로그인 버튼 추가
   - UserInfo 매핑 로직 확장 (GitHub API 응답 구조 다름)

2. **Kakao/Naver OAuth** (한국 사용자 대상):
   - 한국 사용자 비율이 높으면 추가 고려
   - Kakao/Naver API 문서 확인

### 계정 연결/해제 기능 (Phase 2)

1. **프로필 페이지 확장**:
   - "연결된 계정" 섹션 추가
   - 각 Provider별 연결 상태 표시
   - "Google 계정 연결" 버튼 (미연결 시)
   - "연결 해제" 버튼 (연결 시)

2. **백엔드 API**:
   - `POST /api/v1/auth/oauth2/link/google`: 현재 로그인한 사용자에게 Google 계정 추가
   - `DELETE /api/v1/auth/oauth2/unlink/google`: Google 계정 연결 해제
   - 단, **최소 1개의 Identity는 유지** (모든 로그인 수단을 제거할 수 없음)

### 비밀번호 설정 기능 (Phase 3)

- OAuth 전용 계정에 나중에 비밀번호 추가 허용
- "비밀번호 설정" 페이지 제공
- LOCAL Identity 생성

## 미해결 이슈

### 1. 계정 병합 정책 최종 결정

**현재 제안**: 같은 이메일이면 자동으로 동일 User 계정에 병합

**대안**:
- 수동 병합 요청 (사용자가 명시적으로 "이미 계정이 있습니다. 연결하시겠습니까?" 승인)
- 별도 계정 생성 (이메일 중복 허용, 하지만 현재 시스템 제약에 맞지 않음)

**결정 필요**: 자동 병합 시 보안 문제 (이메일만으로 동일 사용자 판단)

### 2. 이메일 변경 처리

- 사용자가 Google 계정의 이메일을 변경하면 어떻게 될까?
- UserIdentity.email vs User.email 불일치 발생 가능

**해결 방안**: OAuth 로그인 시마다 최신 이메일로 업데이트 (단, User.email은 변경하지 않음)

### 3. OAuth Access Token 저장 여부

- 현재는 Google Access Token을 저장하지 않음 (JWT만 사용)
- 향후 Google Drive 연동 등을 위해 필요할 수 있음

**해결 방안**: 필요 시 AuthToken 테이블에 OAUTH_ACCESS 타입 추가

### 4. 계정 삭제 시 UserIdentity 처리

- User 삭제 시 UserIdentity도 자동 삭제 (CASCADE)
- 복구 정책 필요 (soft delete 고려)

## 용어 정리

| 용어 | 정의 |
|------|------|
| **OAuth 2.0** | 사용자 인증 및 권한 부여를 위한 개방형 표준 프로토콜. 제3자 애플리케이션이 사용자의 자원에 접근할 수 있도록 허용. |
| **Social Login** | Google, GitHub 등 소셜 미디어 계정을 이용한 로그인 방식. OAuth 2.0을 기반으로 구현. |
| **Provider** | OAuth 인증을 제공하는 서비스 (Google, GitHub, Kakao 등). Authorization Server 역할. |
| **Client** | OAuth를 사용하는 애플리케이션 (우리 칸반 보드 서비스). |
| **Authorization Code** | OAuth 2.0 Authorization Code Flow에서 Provider가 발급하는 임시 코드. Access Token으로 교환됨. |
| **Access Token** | Provider의 API를 호출할 수 있는 토큰. 우리는 UserInfo API 호출 후 폐기. |
| **UserIdentity** | 외부 프로바이더와 우리 User의 연결 정보를 저장하는 엔티티. |
| **Provider ID** | 외부 프로바이더가 발급한 사용자 고유 ID (예: Google의 sub 필드, GitHub의 id 필드). |
| **계정 병합 (Account Linking)** | 같은 이메일로 가입한 여러 Provider의 계정을 하나의 User로 통합하는 과정. |
| **Primary Provider** | 사용자가 최초로 가입한 경로 (LOCAL, GOOGLE 등). |
| **Redirect URI** | OAuth 인증 완료 후 Provider가 사용자를 돌려보낼 URL (콜백 URL). |
| **Scope** | OAuth 요청 시 접근하려는 권한 범위 (email, profile 등). |
| **State** | CSRF 공격 방지를 위해 OAuth 요청 시 전달하는 랜덤 문자열. |

## 참고 자료

### 공식 문서

- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Google OAuth 2.0 Guide](https://developers.google.com/identity/protocols/oauth2)
- [RFC 6749 - OAuth 2.0 Framework](https://datatracker.ietf.org/doc/html/rfc6749)

### 기존 명세서

- `docs/specs/Priority-1/model-auth-000.md`: 인증 데이터 모델 (UserIdentity 확장 언급)
- `docs/specs/Priority-1/api-spec-000.md`: 기존 인증 API 명세

### 구현 참고

- `backend/src/main/java/com/kanban/auth/`: 기존 인증 로직
- `frontend/src/context/AuthContext.tsx`: 기존 인증 상태 관리

---

**작성일**: 2025-01-14
**작성자**: Claude Code
**버전**: 1.0
**상태**: Draft (구현 전 검토 필요)
