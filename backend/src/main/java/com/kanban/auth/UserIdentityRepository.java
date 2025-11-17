package com.kanban.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * UserIdentity Repository
 * Spec § 6. 백엔드 규격 - UserIdentityRepository
 */
public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    /**
     * 프로바이더와 프로바이더 사용자 ID로 UserIdentity 조회
     * Spec § 3: 시나리오 3 - 이미 Google 계정이 연동된 사용자의 재로그인
     * FR-06g: OAuth2 콜백 처리 - 사용자 정보 조회
     *
     * @param provider OAuth2 프로바이더 (GOOGLE, KAKAO, NAVER)
     * @param providerUserId 프로바이더의 사용자 고유 ID
     * @return UserIdentity (Optional)
     */
    Optional<UserIdentity> findByProviderAndProviderUserId(OAuth2Provider provider, String providerUserId);

    /**
     * 사용자 ID로 연동된 모든 소셜 계정 조회
     * Spec § 6. API 엔드포인트 3 - 사용자 연동 계정 목록 조회
     * FR-06e: 소셜 계정 연동 해제
     *
     * @param userId 사용자 ID
     * @return UserIdentity 목록
     */
    List<UserIdentity> findByUserId(Long userId);

    /**
     * 사용자 ID로 연동된 소셜 계정 개수 조회
     * 연동 해제 시 최소 인증 수단 확인에 사용
     *
     * @param userId 사용자 ID
     * @return 소셜 계정 개수
     */
    long countByUserId(Long userId);
}
