package com.kanban.auth.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * 커스텀 OAuth2 사용자 정보 로드 서비스
 * Spec § 6. 백엔드 규격 - OAuth2UserInfoFactory
 * FR-06g: OAuth2 콜백 처리 - 사용자 정보 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    /**
     * OAuth2 사용자 정보 로드
     * Spring Security OAuth2 Client가 자동으로 호출
     *
     * @param userRequest OAuth2 사용자 요청 (access token 포함)
     * @return OAuth2User (프로바이더에서 받은 사용자 정보)
     * @throws OAuth2AuthenticationException OAuth2 인증 실패 시
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Spring Security의 기본 구현으로 OAuth2 사용자 정보 로드
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.debug("Loading OAuth2 user for provider: {}", registrationId);
        log.debug("OAuth2 user attributes: {}", oAuth2User.getAttributes());

        // OAuth2User를 그대로 반환 (OAuth2AuthenticationSuccessHandler에서 처리)
        return oAuth2User;
    }
}
