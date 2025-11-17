package com.kanban.auth.oauth;

import com.kanban.auth.AuthProvider;
import com.kanban.auth.AuthToken;
import com.kanban.auth.AuthTokenRepository;
import com.kanban.auth.TokenType;
import com.kanban.auth.config.JwtProperties;
import com.kanban.auth.token.JwtTokenProvider;
import com.kanban.user.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OAuth 2.0 로그인 성공 핸들러
 * JWT 토큰 발급 및 프론트엔드로 리다이렉트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2Service oAuth2Service;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenRepository authTokenRepository;
    private final JwtProperties jwtProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        try {
            // 프로바이더 추출 (URL에서)
            String provider = extractProvider(request);
            AuthProvider authProvider = AuthProvider.valueOf(provider.toUpperCase());

            // OAuth 로그인 처리
            User user = oAuth2Service.processOAuthLogin(authProvider, oAuth2User);

            // JWT 토큰 발급
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            AuthToken refreshToken = createRefreshToken(user);

            // Refresh Token을 HttpOnly 쿠키로 설정
            ResponseCookie cookie = buildRefreshCookie(refreshToken.getToken());
            response.addHeader("Set-Cookie", cookie.toString());

            // 프론트엔드로 리다이렉트 (Access Token 전달)
            String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect")
                    .queryParam("token", accessToken)
                    .build()
                    .toUriString();

            log.info("OAuth 로그인 성공: userId={}, provider={}", user.getId(), authProvider);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("OAuth 로그인 실패", e);

            String errorUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/login")
                    .queryParam("error", "oauth_failed")
                    .queryParam("message", e.getMessage())
                    .build()
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    /**
     * URL에서 프로바이더 추출
     * 예: /api/v1/auth/oauth2/callback/google -> "google"
     */
    private String extractProvider(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String[] parts = uri.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Refresh Token 생성
     */
    private AuthToken createRefreshToken(User user) {
        AuthToken token = AuthToken.builder()
                .token(UUID.randomUUID().toString())
                .type(TokenType.REFRESH)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.refreshTokenValiditySeconds()))
                .revoked(false)
                .user(user)
                .build();
        return authTokenRepository.save(token);
    }

    /**
     * Refresh Token 쿠키 생성
     */
    private ResponseCookie buildRefreshCookie(String value) {
        return ResponseCookie.from(jwtProperties.refreshTokenCookieName(), value)
                .httpOnly(true)
                .secure(false)  // 로컬 개발 환경에서는 false (프로덕션에서는 true)
                .path("/")
                .maxAge(jwtProperties.refreshTokenValiditySeconds())
                .sameSite("Lax")
                .build();
    }
}
