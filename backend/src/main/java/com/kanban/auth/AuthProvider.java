package com.kanban.auth;

/**
 * 사용자 인증 프로바이더 타입
 * OAuth 2.0 및 로컬 인증 방식을 구분
 */
public enum AuthProvider {
    /**
     * 이메일/비밀번호 기반 로컬 인증
     */
    LOCAL,

    /**
     * Google OAuth 2.0 인증
     */
    GOOGLE,

    /**
     * GitHub OAuth 2.0 인증 (Phase 2)
     */
    GITHUB,

    /**
     * Kakao OAuth 2.0 인증 (Phase 3)
     */
    KAKAO,

    /**
     * Naver OAuth 2.0 인증 (Phase 3)
     */
    NAVER
}
