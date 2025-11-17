# OAuth 2.0 소셜 로그인 구현 요약

## 개요

Google OAuth 2.0을 이용한 소셜 로그인 기능을 성공적으로 구현했습니다. 사용자는 이제 기존 이메일/비밀번호 로그인 외에도 Google 계정으로 간편하게 로그인할 수 있습니다.

## 구현 내용

### Phase 1: 데이터 모델 및 인프라 구축

#### 백엔드 변경사항

1. **AuthProvider Enum 생성** ([backend/src/main/java/com/kanban/auth/AuthProvider.java](../../backend/src/main/java/com/kanban/auth/AuthProvider.java))
   - LOCAL, GOOGLE, GITHUB, KAKAO, NAVER 프로바이더 정의
   - 향후 다른 OAuth 프로바이더 확장 가능

2. **UserIdentity 엔티티 생성** ([backend/src/main/java/com/kanban/auth/UserIdentity.java](../../backend/src/main/java/com/kanban/auth/UserIdentity.java))
   - 외부 프로바이더 연동 정보 저장
   - User와 N:1 관계 (한 사용자가 여러 프로바이더 연동 가능)
   - 제약 조건:
     - `UNIQUE(user_id, provider)`: 한 사용자가 같은 프로바이더 중복 연결 방지
     - `UNIQUE(provider, provider_id)`: 같은 외부 계정이 여러 User에 연결 방지

3. **User 엔티티 수정** ([backend/src/main/java/com/kanban/user/User.java](../../backend/src/main/java/com/kanban/user/User.java))
   - `password` 필드를 nullable로 변경 (OAuth 사용자는 비밀번호 없음)
   - `primaryProvider` 필드 추가 (최초 가입 경로 추적)

4. **UserIdentityRepository 생성** ([backend/src/main/java/com/kanban/auth/UserIdentityRepository.java](../../backend/src/main/java/com/kanban/auth/UserIdentityRepository.java))
   - 프로바이더별 Identity 조회 메서드
   - 사용자별 연동 정보 조회 메서드

5. **DB 마이그레이션** ([backend/src/main/resources/db/migration/V4__Add_OAuth_Support.sql](../../backend/src/main/resources/db/migration/V4__Add_OAuth_Support.sql))
   - `user_identities` 테이블 생성
   - 기존 사용자에 대한 LOCAL identity 자동 생성

### Phase 2: OAuth 백엔드 구현

#### Gradle 의존성 추가

```kotlin
implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
```

#### Spring Security OAuth2 설정

1. **application.yml 설정** ([backend/src/main/resources/application.yml](../../backend/src/main/resources/application.yml))
   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             google:
               client-id: ${GOOGLE_CLIENT_ID}
               client-secret: ${GOOGLE_CLIENT_SECRET}
               scope: [email, profile]
               redirect-uri: "{baseUrl}/api/v1/auth/oauth2/callback/google"
   ```

2. **OAuth2Service 생성** ([backend/src/main/java/com/kanban/auth/oauth/OAuth2Service.java](../../backend/src/main/java/com/kanban/auth/oauth/OAuth2Service.java))
   - OAuth 로그인 처리 및 계정 병합 로직
   - 프로바이더별 사용자 정보 추출
   - 신규 사용자 생성 및 워크스페이스 자동 생성

3. **Success/Failure Handler 구현**
   - **OAuth2AuthenticationSuccessHandler** ([backend/src/main/java/com/kanban/auth/oauth/OAuth2AuthenticationSuccessHandler.java](../../backend/src/main/java/com/kanban/auth/oauth/OAuth2AuthenticationSuccessHandler.java))
     - OAuth 로그인 성공 시 JWT 토큰 발급
     - 프론트엔드로 리다이렉트 (`/oauth2/redirect?token=xxx`)

   - **OAuth2AuthenticationFailureHandler** ([backend/src/main/java/com/kanban/auth/oauth/OAuth2AuthenticationFailureHandler.java](../../backend/src/main/java/com/kanban/auth/oauth/OAuth2AuthenticationFailureHandler.java))
     - 에러 메시지와 함께 로그인 페이지로 리다이렉트

4. **SecurityConfig 수정** ([backend/src/main/java/com/kanban/auth/security/SecurityConfig.java](../../backend/src/main/java/com/kanban/auth/security/SecurityConfig.java))
   - OAuth2 로그인 엔드포인트 설정
   - Success/Failure 핸들러 등록

#### 계정 병합 정책

OAuth 로그인 시 다음 순서로 처리:

1. **기존 Identity 조회**: `provider` + `provider_id`로 UserIdentity 조회
   - 존재하면 → 해당 User 반환 (기존 로그인)

2. **이메일 기반 병합**: 같은 이메일로 가입된 User 조회
   - 존재하면 → 기존 User에 새로운 Identity 추가 (계정 병합)

3. **신규 사용자 생성**: User 없으면
   - 신규 User 생성 (password = null, primaryProvider = GOOGLE)
   - UserIdentity 생성
   - 기본 워크스페이스 자동 생성

### Phase 3: 프론트엔드 구현

#### 타입 정의 추가

**auth.ts 확장** ([frontend/src/types/auth.ts](../../frontend/src/types/auth.ts))

```typescript
export enum AuthProvider {
  LOCAL = 'LOCAL',
  GOOGLE = 'GOOGLE',
  GITHUB = 'GITHUB',
  KAKAO = 'KAKAO',
  NAVER = 'NAVER',
}

export interface UserProfile {
  // ... 기존 필드
  primaryProvider: AuthProvider;
  identities?: UserIdentity[];
}
```

#### UI 컴포넌트

1. **GoogleLoginButton** ([frontend/src/components/auth/GoogleLoginButton.tsx](../../frontend/src/components/auth/GoogleLoginButton.tsx))
   - Google 로고 + "Google로 로그인" 버튼
   - 클릭 시 `/api/v1/auth/oauth2/authorization/google`로 리다이렉트

2. **LoginPage 수정** ([frontend/src/pages/LoginPage.tsx](../../frontend/src/pages/LoginPage.tsx))
   - 기존 로그인 폼 아래 구분선 추가
   - Google 로그인 버튼 추가
   - OAuth 에러 메시지 처리 (`?error=oauth_failed`)

3. **OAuthRedirectPage** ([frontend/src/pages/OAuthRedirectPage.tsx](../../frontend/src/pages/OAuthRedirectPage.tsx))
   - OAuth 콜백 처리 (`/oauth2/redirect?token=xxx`)
   - Access Token 저장
   - 사용자 프로필 로드
   - Dashboard로 리다이렉트

#### 라우팅 설정

**App.tsx 수정** ([frontend/src/App.tsx](../../frontend/src/App.tsx))

```tsx
<Route path="/oauth2/redirect" element={<OAuthRedirectPage />} />
```

### Phase 4: 크로스 플랫폼 지원

#### package.json 수정

Windows와 Unix/Linux/Mac 모두에서 작동하도록 npm 스크립트 수정:

```json
{
  "scripts": {
    "dev:backend": "cd backend && (if exist gradlew.bat (gradlew.bat bootRun) else (./gradlew bootRun))",
    "build:backend": "cd backend && (if exist gradlew.bat (gradlew.bat build) else (./gradlew build))"
  }
}
```

## 사용 방법

### 환경 변수 설정

Google OAuth 사용을 위해 다음 환경 변수를 설정해야 합니다:

```bash
# .env 또는 시스템 환경 변수
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

#### Google OAuth 설정 방법

1. [Google Cloud Console](https://console.cloud.google.com/)에 접속
2. 프로젝트 생성 또는 선택
3. "API 및 서비스" > "사용자 인증 정보" 이동
4. "사용자 인증 정보 만들기" > "OAuth 클라이언트 ID" 선택
5. 애플리케이션 유형: "웹 애플리케이션"
6. 승인된 리디렉션 URI 추가:
   - 로컬 개발: `http://localhost:8080/api/v1/auth/oauth2/callback/google`
   - 프로덕션: `https://your-domain.com/api/v1/auth/oauth2/callback/google`

### 개발 서버 실행

```bash
# 루트 디렉터리에서
npm run dev

# 또는 개별 실행
npm run dev:backend        # 백엔드만
npm run dev:frontend       # 프론트엔드만
```

### 빌드

```bash
npm run build
```

## 보안 고려사항

### 구현된 보안 기능

1. **CSRF 보호**: Spring Security OAuth2 Client가 자동으로 state 파라미터 검증
2. **Redirect URI 검증**: application.yml에 명시된 URI만 허용
3. **Token 관리**:
   - Access Token: localStorage 저장 (15분)
   - Refresh Token: HttpOnly 쿠키 (14일)
   - OAuth Access Token: 백엔드에서만 사용, 프론트엔드 노출 안 함
4. **Scope 최소화**: `email`, `profile`만 요청
5. **이메일 검증**: Google의 `email_verified` 필드 확인
6. **Rate Limiting**: OAuth 콜백 엔드포인트 (권장사항 - 향후 구현)

### 주의사항

- **환경 변수 보안**: `GOOGLE_CLIENT_SECRET`은 절대 코드에 하드코딩하지 말 것
- **HTTPS 사용**: 프로덕션에서는 반드시 HTTPS 사용
- **토큰 만료**: Access Token은 15분, Refresh Token은 14일 후 만료

## API 엔드포인트

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| GET | `/api/v1/auth/oauth2/authorization/google` | Google 로그인 시작 (리다이렉트) | No |
| GET | `/api/v1/auth/oauth2/callback/google` | OAuth 콜백 처리 (Spring Security 자동 처리) | No |

## 테스트

### 백엔드 테스트

```bash
cd backend
./gradlew test
```

테스트 환경에서는 OAuth2 핸들러를 Mock으로 제공하여 단위 테스트 실행.

### 프론트엔드 테스트

```bash
cd frontend
npm test
```

## 향후 확장 계획

### Phase 2: 추가 프로바이더 (GitHub, Kakao, Naver)

1. application.yml에 프로바이더 설정 추가
2. OAuth2Service에서 프로바이더별 UserInfo 매핑 로직 확장
3. 프론트엔드에 해당 로그인 버튼 추가

### Phase 3: 계정 연결/해제 기능

1. 프로필 페이지에 "연결된 계정" 섹션 추가
2. API 엔드포인트:
   - `POST /api/v1/auth/oauth2/link/{provider}`: 계정 연결
   - `DELETE /api/v1/auth/oauth2/unlink/{provider}`: 계정 해제
3. 최소 1개의 Identity 유지 제약 추가

### Phase 4: 비밀번호 설정 기능

- OAuth 전용 계정에 나중에 비밀번호 추가 허용
- "비밀번호 설정" 페이지 제공
- LOCAL Identity 생성

## 문제 해결

### 일반적인 오류

1. **"Table USERS not found"**
   - 원인: Flyway가 활성화되어 있지만 V1 마이그레이션 없음
   - 해결: 테스트 환경에서 `flyway.enabled=false` 설정

2. **"NoSuchBeanDefinitionException: OAuth2AuthenticationSuccessHandler"**
   - 원인: 테스트 컨텍스트에 OAuth2 핸들러 빈 없음
   - 해결: `@Import(TestSecurityConfig.class)` 추가

3. **Windows에서 "./gradlew: command not found"**
   - 원인: Windows에서 Unix 스크립트 실행 시도
   - 해결: package.json에서 크로스 플랫폼 스크립트 사용

## 참고 자료

- [Spring Security OAuth2 Client 공식 문서](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Google OAuth 2.0 가이드](https://developers.google.com/identity/protocols/oauth2)
- [RFC 6749 - OAuth 2.0 Framework](https://datatracker.ietf.org/doc/html/rfc6749)
- [요구사항 문서](../requirements/Priority-2/social-login-oauth.md)

## 작성 정보

- **작성일**: 2025-01-14
- **작성자**: Claude Code
- **버전**: 1.0
- **상태**: 구현 완료 (Google OAuth)
