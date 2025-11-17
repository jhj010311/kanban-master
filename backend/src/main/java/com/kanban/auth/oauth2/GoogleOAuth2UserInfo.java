package com.kanban.auth.oauth2;

import com.kanban.auth.OAuth2Provider;

import java.util.Map;

/**
 * Google OAuth2 사용자 정보 구현
 * Spec § 6. 백엔드 규격 - OAuth2UserInfoFactory
 * FR-06i: 프로바이더별 사용자 정보 매핑 - Google
 */
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public OAuth2Provider getProvider() {
        return OAuth2Provider.GOOGLE;
    }

    /**
     * Google 사용자 고유 ID ('sub' 클레임)
     * Spec § 6: Google 응답 예시 - "sub": "1234567890"
     */
    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    /**
     * Google 사용자 이메일
     * Spec § 6: Google 응답 예시 - "email": "user@gmail.com"
     */
    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    /**
     * Google 사용자 이름
     * Spec § 6: Google 응답 예시 - "name": "홍길동"
     */
    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    /**
     * Google 프로필 이미지 URL
     * Spec § 6: Google 응답 예시 - "picture": "https://lh3.googleusercontent.com/..."
     */
    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("picture");
    }
}
