package com.kanban.auth.oauth2;

import com.kanban.auth.OAuth2Provider;
import com.kanban.auth.UserIdentity;
import com.kanban.auth.UserIdentityRepository;
import com.kanban.auth.token.JwtTokenProvider;
import com.kanban.user.User;
import com.kanban.user.UserRepository;
import com.kanban.user.UserStatus;
import com.kanban.workspace.UserWorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * OAuth2 인증 성공 핸들러
 * Spec § 6. 백엔드 규격 - OAuth2AuthenticationSuccessHandler
 * FR-06g: OAuth2 콜백 처리
 * FR-06h: 보안 토큰 발급
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final JwtTokenProvider tokenProvider;
    private final UserWorkspaceService userWorkspaceService;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    /**
     * OAuth2 인증 성공 시 호출
     * Spec § 7. OAuth2 플로우 - 신규 사용자 Google 로그인 플로우
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param authentication OAuth2 인증 정보
     */
    @Override
    @Transactional
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String registrationId = getRegistrationId(request);

            log.debug("OAuth2 authentication successful for provider: {}", registrationId);

            // Spec § 6: 프로바이더별 사용자 정보 매핑
            OAuth2Provider provider = OAuth2UserInfoFactory.fromProviderName(registrationId);
            OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                provider,
                oAuth2User.getAttributes()
            );

            // Spec § 7. 보안 처리 - 이메일 검증
            String email = oAuth2UserInfo.getEmail();
            if (email == null || email.isBlank()) {
                log.error("Email not provided by OAuth2 provider: {}", provider);
                redirectToError(response, "이메일 정보를 가져올 수 없습니다. OAuth2 설정을 확인해주세요.");
                return;
            }

            // Spec § 3: 시나리오 1, 2, 3 - UserIdentity 조회 또는 생성
            User user = processOAuth2User(oAuth2UserInfo);

            // 신규 OAuth2 유저도 최소 1개의 워크스페이스를 갖도록 보장
            userWorkspaceService.ensureUserHasWorkspace(user);

            // Spec § 6: JWT AccessToken 발급
            String accessToken = tokenProvider.generateAccessToken(user);

            // Spec § 6: 프론트엔드로 리다이렉션 (/auth/callback?token={JWT})
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", accessToken)
                .build()
                .toUriString();

            log.debug("OAuth2 login successful. Redirecting to: {}", targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("OAuth2 authentication failed", e);
            redirectToError(response, "소셜 로그인에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * OAuth2 사용자 처리: UserIdentity 및 User 조회 또는 생성
     * Spec § 6. 서비스 로직 - OAuth2AuthenticationSuccessHandler 플로우
     *
     * @param oAuth2UserInfo OAuth2 사용자 정보
     * @return User 엔티티
     */
    private User processOAuth2User(OAuth2UserInfo oAuth2UserInfo) {
        OAuth2Provider provider = oAuth2UserInfo.getProvider();
        String providerId = oAuth2UserInfo.getProviderId();
        String email = oAuth2UserInfo.getEmail();

        // Spec § 3: 시나리오 3 - 이미 Google 계정이 연동된 사용자의 재로그인
        UserIdentity existingIdentity = userIdentityRepository
            .findByProviderAndProviderUserId(provider, providerId)
            .orElse(null);

        if (existingIdentity != null) {
            log.debug("Existing UserIdentity found. User: {}", existingIdentity.getUser().getEmail());
            User user = existingIdentity.getUser();
            user.setLastLoginAt(LocalDateTime.now());

            // 프로필 정보 동기화 (사용자 요청 사항)
            syncUserProfile(user, existingIdentity, oAuth2UserInfo);

            return user;
        }

        // Spec § 3: 시나리오 2 - 기존 이메일/비밀번호 계정에 Google 계정 연동
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            log.debug("Existing User found by email. Creating UserIdentity for provider: {}", provider);
            UserIdentity newIdentity = createUserIdentity(user, oAuth2UserInfo);
            user.setLastLoginAt(LocalDateTime.now());

            // 프로필 정보 동기화 (사용자 요청 사항)
            syncUserProfile(user, newIdentity, oAuth2UserInfo);

            return user;
        }

        // Spec § 3: 시나리오 1 - 처음 Google 로그인하는 신규 사용자
        log.debug("Creating new User and UserIdentity for email: {}", email);
        User newUser = createUser(oAuth2UserInfo);
        createUserIdentity(newUser, oAuth2UserInfo);
        return newUser;
    }

    /**
     * 신규 User 생성
     * Spec § 3: 시나리오 1 - 신규 사용자 자동 생성
     * FR-06c: 신규 사용자 자동 생성
     */
    private User createUser(OAuth2UserInfo oAuth2UserInfo) {
        User user = User.builder()
            .email(oAuth2UserInfo.getEmail())
            .name(oAuth2UserInfo.getName())
            .password(null)  // Spec § 6: 소셜 로그인 전용 사용자는 password null
            .avatarUrl(oAuth2UserInfo.getProfileImageUrl())
            .status(UserStatus.ACTIVE)
            .lastLoginAt(LocalDateTime.now())
            .build();

        return userRepository.save(user);
    }

    /**
     * UserIdentity 생성
     * FR-06b: UserIdentity 엔티티 추가
     */
    private UserIdentity createUserIdentity(User user, OAuth2UserInfo oAuth2UserInfo) {
        UserIdentity identity = UserIdentity.builder()
            .user(user)
            .provider(oAuth2UserInfo.getProvider())
            .providerUserId(oAuth2UserInfo.getProviderId())
            .email(oAuth2UserInfo.getEmail())
            .name(oAuth2UserInfo.getName())
            .profileImageUrl(oAuth2UserInfo.getProfileImageUrl())
            .build();

        return userIdentityRepository.save(identity);
    }

    /**
     * 프로필 정보 동기화 (사용자 요청 사항)
     * Google에서 제공하는 이름과 프로필 이미지를 User 테이블에 동기화
     */
    private void syncUserProfile(User user, UserIdentity identity, OAuth2UserInfo oAuth2UserInfo) {
        // 이름 동기화
        if (oAuth2UserInfo.getName() != null && !oAuth2UserInfo.getName().isBlank()) {
            user.setName(oAuth2UserInfo.getName());
            identity.setName(oAuth2UserInfo.getName());
        }

        // 프로필 이미지 동기화
        if (oAuth2UserInfo.getProfileImageUrl() != null && !oAuth2UserInfo.getProfileImageUrl().isBlank()) {
            user.setAvatarUrl(oAuth2UserInfo.getProfileImageUrl());
            identity.setProfileImageUrl(oAuth2UserInfo.getProfileImageUrl());
        }

        log.debug("Profile synced for user: {}", user.getEmail());
    }

    /**
     * OAuth2 등록 ID 추출 (google, kakao, naver)
     */
    private String getRegistrationId(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        // /login/oauth2/code/google → google
        // /api/v1/auth/oauth2/callback/google → google
        String[] segments = requestUri.split("/");
        return segments[segments.length - 1];
    }

    /**
     * 에러 리다이렉션
     */
    private void redirectToError(HttpServletResponse response, String errorMessage) throws IOException {
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/login")
            .queryParam("error", errorMessage)
            .build()
            .toUriString();

        response.sendRedirect(targetUrl);
    }
}
