package com.kanban.auth;

/**
 * OAuth2 프로바이더 열거형
 * Spec § 6. 백엔드 규격 - UserIdentity 엔티티
 * FR-06f: 다중 프로바이더 지원 (Google, Kakao, Naver)
 */
public enum OAuth2Provider {
    /**
     * Google OAuth2 프로바이더
     */
    GOOGLE,

    /**
     * Kakao OAuth2 프로바이더 (향후 구현)
     */
    KAKAO,

    /**
     * Naver OAuth2 프로바이더 (향후 구현)
     */
    NAVER
}
