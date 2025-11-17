package com.kanban.auth.oauth2;

import com.kanban.auth.OAuth2Provider;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * OAuth2 사용자 정보 팩토리
 * Spec § 6. 백엔드 규격 - OAuth2UserInfoFactory
 * FR-06i: 프로바이더별 사용자 정보 매핑
 */
public class OAuth2UserInfoFactory {

    /**
     * 프로바이더별로 OAuth2UserInfo 구현체 생성
     * Spec § 6: 각 프로바이더의 사용자 정보 응답을 통일된 인터페이스로 변환
     *
     * @param provider OAuth2 프로바이더
     * @param attributes OAuth2 사용자 속성
     * @return OAuth2UserInfo 구현체
     * @throws ResponseStatusException 지원하지 않는 프로바이더인 경우
     */
    public static OAuth2UserInfo getOAuth2UserInfo(OAuth2Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
            case KAKAO, NAVER -> throw new ResponseStatusException(
                BAD_REQUEST,
                String.format("%s 프로바이더는 아직 지원하지 않습니다.", provider)
            );
        };
    }

    /**
     * 프로바이더 이름(문자열)으로 OAuth2Provider Enum 변환
     *
     * @param providerName 프로바이더 이름 (google, kakao, naver)
     * @return OAuth2Provider Enum
     * @throws ResponseStatusException 지원하지 않는 프로바이더인 경우
     */
    public static OAuth2Provider fromProviderName(String providerName) {
        try {
            return OAuth2Provider.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                BAD_REQUEST,
                String.format("지원하지 않는 OAuth2 프로바이더입니다: %s", providerName)
            );
        }
    }
}
