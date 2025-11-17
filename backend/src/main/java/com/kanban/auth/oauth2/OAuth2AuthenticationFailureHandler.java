package com.kanban.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 인증 실패 핸들러
 * Spec § 3: 시나리오 5 - OAuth2 인증 실패
 * Spec § 6. 백엔드 규격 - OAuth2AuthenticationFailureHandler
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    /**
     * OAuth2 인증 실패 시 호출
     * Spec § 3: 시나리오 5 - 에러 처리 및 로그인 페이지로 리다이렉션
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param exception 인증 예외
     */
    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        log.error("OAuth2 authentication failed", exception);

        // Spec § 9. 에러 처리 - OAuth2 인증 실패
        String errorMessage = determineErrorMessage(exception);

        // Spec § 6: 프론트엔드 로그인 페이지로 리다이렉션 (/login?error={ERROR_MESSAGE})
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/login")
            .queryParam("error", errorMessage)
            .build()
            .toUriString();

        log.debug("Redirecting to login page with error: {}", errorMessage);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * 예외 타입에 따른 에러 메시지 결정
     */
    private String determineErrorMessage(AuthenticationException exception) {
        String message = exception.getMessage();

        if (message != null) {
            if (message.contains("access_denied")) {
                return "소셜 로그인이 취소되었습니다";
            }
            if (message.contains("email")) {
                return "이메일 정보를 가져올 수 없습니다";
            }
        }

        return "소셜 로그인에 실패했습니다. 다시 시도해주세요.";
    }
}
