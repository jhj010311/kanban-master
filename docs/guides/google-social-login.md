# Google OAuth2 소셜 로그인 설정 가이드

Modern Kanban Service에 Google 소셜 로그인을 설정하는 방법을 안내합니다.

## 목차

1. [Google Cloud Console 설정](#1-google-cloud-console-설정)
2. [OAuth2 Credentials 생성](#2-oauth2-credentials-생성)
3. [환경 변수 설정](#3-환경 변수-설정)
4. [로컬 개발 환경 테스트](#4-로컬-개발-환경-테스트)
5. [프로덕션 배포 설정](#5-프로덕션-배포-설정)
6. [문제 해결](#6-문제-해결)

---

## 1. Google Cloud Console 설정

### 1.1 Google Cloud Console 접속

1. [Google Cloud Console](https://console.cloud.google.com/)에 접속합니다.
2. Google 계정으로 로그인합니다.

### 1.2 새 프로젝트 생성

1. 상단 메뉴에서 **프로젝트 선택** → **새 프로젝트** 클릭
2. 프로젝트 이름 입력: `Modern Kanban Service` (또는 원하는 이름)
3. **만들기** 버튼 클릭
4. 프로젝트 생성 완료 후 해당 프로젝트 선택

---

## 2. OAuth2 Credentials 생성

### 2.1 OAuth 동의 화면 구성

1. 좌측 메뉴에서 **API 및 서비스** → **OAuth 동의 화면** 선택
2. **User Type** 선택:
    - **외부(External)**: 모든 Google 계정 사용자 허용 (권장)
    - **내부(Internal)**: Google Workspace 조직 내부 사용자만 허용
3. **만들기** 버튼 클릭

### 2.2 앱 정보 입력

**1단계: OAuth 동의 화면**

| 필드                 | 값                              |
| -------------------- | ------------------------------- |
| 앱 이름              | Modern Kanban Service           |
| 사용자 지원 이메일   | your-email@example.com          |
| 앱 로고              | (선택 사항) 로고 이미지 업로드  |
| 앱 도메인            | (선택 사항) https://yourapp.com |
| 승인된 도메인        | yourapp.com (프로덕션 도메인)   |
| 개발자 연락처 이메일 | your-email@example.com          |

**2단계: 범위 설정**

1. **범위 추가 또는 삭제** 클릭
2. 다음 범위를 선택합니다:
    - `openid` (기본 선택됨)
    - `email` (필수)
    - `profile` (필수)
3. **업데이트** 클릭

**3단계: 테스트 사용자 (외부 앱인 경우)**

1. **테스트 사용자 추가** 클릭
2. 테스트할 Google 계정 이메일 입력
3. **추가** 클릭

**4단계: 요약**

-   입력한 정보 확인 후 **대시보드로 돌아가기** 클릭

### 2.3 OAuth2 Client ID 생성

1. 좌측 메뉴에서 **API 및 서비스** → **사용자 인증 정보** 선택
2. 상단에서 **+ 사용자 인증 정보 만들기** → **OAuth 클라이언트 ID** 클릭
3. 애플리케이션 유형: **웹 애플리케이션** 선택
4. 이름 입력: `Modern Kanban - Web Client`

**승인된 자바스크립트 원본 (선택 사항)**

```
http://localhost:3000
http://localhost:8080
```

**승인된 리디렉션 URI (필수)**

```
http://localhost:8080/api/v1/auth/oauth2/callback/google
http://localhost:8080/login/oauth2/code/google
```

> **참고**: Spring Security OAuth2는 두 가지 콜백 패턴을 지원합니다.
>
> -   `/login/oauth2/code/{provider}` (기본)
> -   `/api/v1/auth/oauth2/callback/{provider}` (커스텀)

5. **만들기** 클릭

### 2.4 Client ID 및 Secret 복사

OAuth 클라이언트가 생성되면 다음 형식의 정보가 표시됩니다:

```
클라이언트 ID: YOUR_CLIENT_ID.apps.googleusercontent.com
클라이언트 보안 비밀번호: YOUR_CLIENT_SECRET
```

⚠️ **중요**: 이 정보를 안전한 곳에 복사해 두세요. 나중에 다시 확인할 수 있지만, 보안 비밀번호는 다시 생성해야 할 수도 있습니다.

---

## 3. 환경 변수 설정

### 3.1 Backend 환경 변수

#### 방법 1: `.env` 파일 생성 (권장)

`backend/.env` 파일 생성:

```env
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```

⚠️ **보안 주의**: `.env` 파일을 `.gitignore`에 추가하여 Git에 커밋되지 않도록 합니다.

`backend/.gitignore`에 추가:

```
.env
.env.local
.env.*.local
```

#### 방법 2: 시스템 환경 변수 설정

**macOS/Linux:**

```bash
export GOOGLE_CLIENT_ID="..."
export GOOGLE_CLIENT_SECRET="..."
```

**Windows (PowerShell):**

```powershell
$env:GOOGLE_CLIENT_ID="..."
$env:GOOGLE_CLIENT_SECRET="..."
```

#### 방법 3: IntelliJ IDEA 실행 구성

1. **Run** → **Edit Configurations** 선택
2. Spring Boot 실행 구성 선택
3. **Environment variables** 필드에 입력:

```
GOOGLE_CLIENT_ID=1234567890-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com;GOOGLE_CLIENT_SECRET=GOCSPX-abcdefghijklmnopqrstuvwxyz
```

### 3.2 application.yml 설정 확인

`backend/src/main/resources/application.yml`에 OAuth2 설정이 추가되어 있는지 확인:

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
                        redirect-uri: '{baseUrl}/api/v1/auth/oauth2/callback/google'
                        authorization-grant-type: authorization_code
                        client-name: Google

                provider:
                    google:
                        authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
                        token-uri: https://oauth2.googleapis.com/token
                        user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
                        user-name-attribute: sub
```

---

## 4. 로컬 개발 환경 테스트

### 4.1 백엔드 서버 실행

```bash
cd backend
./gradlew bootRun
```

서버 로그에서 OAuth2 설정이 로드되었는지 확인:

```
INFO  c.k.a.s.SecurityConfig - OAuth2 Login enabled for providers: [google]
```

### 4.2 프론트엔드 서버 실행

```bash
cd frontend
npm run dev
```

### 4.3 Google 로그인 테스트

1. 브라우저에서 http://localhost:3000/login 접속
2. **Google로 로그인** 버튼 클릭
3. Google 계정 선택 및 권한 승인
4. 로그인 성공 후 대시보드로 리다이렉션 확인

### 4.4 디버깅 로그 확인

**Backend 로그:**

```
DEBUG c.k.a.o.CustomOAuth2UserService - Loading OAuth2 user for provider: google
DEBUG c.k.a.o.OAuth2AuthenticationSuccessHandler - OAuth2 authentication successful for user: user@gmail.com
DEBUG c.k.a.o.OAuth2AuthenticationSuccessHandler - JWT token issued: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Frontend 로그 (브라우저 콘솔):**

```
🔑 [AuthContext.login] Starting login...
✔️ [AuthContext.login] Token saved. Token in storage: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
✔️ [AuthContext.login] User set: user@gmail.com
```

---

## 5. 프로덕션 배포 설정

### 5.1 프로덕션 도메인 추가

Google Cloud Console에서 프로덕션 도메인을 추가합니다:

1. **API 및 서비스** → **사용자 인증 정보** 선택
2. 생성한 OAuth 클라이언트 ID 클릭
3. **승인된 리디렉션 URI**에 프로덕션 URL 추가:

```
https://yourapp.com/api/v1/auth/oauth2/callback/google
```

4. **저장** 클릭

### 5.2 프로덕션 환경 변수

**Docker:**

```yaml
# docker-compose.yml
services:
    backend:
        environment:
            - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
            - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
```

**Kubernetes:**

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
    name: kanban-oauth2-secret
type: Opaque
stringData:
    google-client-id: '1234567890-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com'
    google-client-secret: 'GOCSPX-abcdefghijklmnopqrstuvwxyz'
```

**AWS Elastic Beanstalk:**

```bash
eb setenv GOOGLE_CLIENT_ID="..." GOOGLE_CLIENT_SECRET="..."
```

**Heroku:**

```bash
heroku config:set GOOGLE_CLIENT_ID="..." GOOGLE_CLIENT_SECRET="..."
```

### 5.3 HTTPS 필수 설정

⚠️ **중요**: 프로덕션 환경에서는 반드시 HTTPS를 사용해야 합니다.

Google OAuth2는 HTTPS가 아닌 Redirect URI를 차단합니다 (localhost 제외).

**Nginx 설정 예시:**

```nginx
server {
    listen 443 ssl http2;
    server_name yourapp.com;

    ssl_certificate /etc/ssl/certs/yourapp.com.crt;
    ssl_certificate_key /etc/ssl/private/yourapp.com.key;

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 6. 문제 해결

### 6.1 "redirect_uri_mismatch" 에러

**에러 메시지:**

```
Error 400: redirect_uri_mismatch
The redirect URI in the request: http://localhost:8080/api/v1/auth/oauth2/callback/google
did not match a registered redirect URI
```

**해결 방법:**

1. Google Cloud Console → **사용자 인증 정보** 확인
2. OAuth 클라이언트 ID의 **승인된 리디렉션 URI** 목록 확인
3. 정확히 일치하는 URI를 추가 (trailing slash 주의)
4. URI는 대소문자 구분됨

### 6.2 "invalid_client" 에러

**에러 메시지:**

```
Error 401: invalid_client
The OAuth client was not found.
```

**해결 방법:**

1. `GOOGLE_CLIENT_ID`와 `GOOGLE_CLIENT_SECRET` 확인
2. 환경 변수가 올바르게 로드되었는지 확인
3. 백엔드 서버 재시작

**확인 명령어:**

```bash
echo $GOOGLE_CLIENT_ID
echo $GOOGLE_CLIENT_SECRET
```

### 6.3 "access_denied" 에러

**에러 메시지:**

```
Error: access_denied
The user denied the request.
```

**원인:**

-   사용자가 Google 로그인 화면에서 "취소" 버튼을 클릭했습니다.

**해결 방법:**

-   정상적인 사용자 행동입니다. 프론트엔드에서 "Google 로그인이 취소되었습니다" 메시지 표시.

### 6.4 "email scope not granted" 에러

**에러 메시지:**

```
Error: Email information not provided by Google
```

**해결 방법:**

1. Google Cloud Console → **OAuth 동의 화면** → **범위** 확인
2. `email` 범위가 추가되어 있는지 확인
3. `application.yml`의 `scope`에 `email`이 포함되어 있는지 확인

### 6.5 테스트 사용자 제한 (외부 앱)

**에러 메시지:**

```
Error 403: access_denied
This app is in testing mode and you are not authorized to access it.
```

**해결 방법:**

1. Google Cloud Console → **OAuth 동의 화면** → **테스트 사용자** 확인
2. 테스트할 Google 계정을 테스트 사용자 목록에 추가
3. 또는 앱을 **게시** 상태로 변경 (검토 필요)

### 6.6 CORS 에러

**에러 메시지:**

```
Access to XMLHttpRequest has been blocked by CORS policy
```

**해결 방법:**

1. `SecurityConfig.java`에서 CORS 설정 확인:

```java
http.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowCredentials(true);
    return config;
}));
```

---

## 7. 보안 체크리스트

-   [ ] `.env` 파일이 `.gitignore`에 추가되었는가?
-   [ ] `GOOGLE_CLIENT_SECRET`이 Git에 커밋되지 않았는가?
-   [ ] 프로덕션 환경에서 HTTPS를 사용하는가?
-   [ ] OAuth2 Redirect URI가 정확히 일치하는가?
-   [ ] `email` scope가 필수로 요청되고 있는가?
-   [ ] JWT Secret Key가 충분히 강력한가? (최소 256비트)
-   [ ] RefreshToken이 HttpOnly Cookie로 저장되고 있는가?

---

## 8. 추가 참고 자료

-   [Google OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)
-   [Spring Security OAuth2 Client Guide](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
-   [Google OAuth2 Playground](https://developers.google.com/oauthplayground/)

---

## 9. 다음 단계

Google 로그인이 성공적으로 설정되었다면, 다음 프로바이더를 추가할 수 있습니다:

-   Kakao OAuth2
-   Naver OAuth2
-   GitHub OAuth2
-   Facebook OAuth2

각 프로바이더별 설정 가이드는 별도 문서를 참조하세요.

---

**작성일**: 2025-11-14
**버전**: 1.0.0
**담당자**: Modern Kanban Team
