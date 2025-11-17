package com.kanban.auth.oauth2;

import com.kanban.auth.OAuth2Provider;

/**
 * OAuth2 사용자 정보 인터페이스
 * Spec § 6. 백엔드 규격 - OAuth2UserInfo (내부 인터페이스)
 * FR-06i: 프로바이더별 사용자 정보 매핑
 */
public interface OAuth2UserInfo {

    /**
     * OAuth2 프로바이더 (GOOGLE, KAKAO, NAVER)
     */
    OAuth2Provider getProvider();

    /**
     * 프로바이더의 사용자 고유 ID
     * Google: 'sub' 클레임
     * Kakao: 'id' 필드
     * Naver: 'id' 필드
     */
    String getProviderId();

    /**
     * 사용자 이메일
     * Spec § 7. 보안 처리 - 이메일 검증
     * 이메일이 없으면 null 반환 (필수 검증 필요)
     */
    String getEmail();

    /**
     * 사용자 이름
     */
    String getName();

    /**
     * 프로필 이미지 URL (선택)
     */
    String getProfileImageUrl();
}
