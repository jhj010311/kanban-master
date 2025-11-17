package com.kanban.auth;

import com.kanban.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 외부 인증 프로바이더 연동 정보 Repository
 */
@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    /**
     * 프로바이더와 프로바이더 ID로 UserIdentity 조회
     * @param provider 인증 프로바이더
     * @param providerId 프로바이더의 사용자 고유 ID
     * @return UserIdentity
     */
    Optional<UserIdentity> findByProviderAndProviderId(AuthProvider provider, String providerId);

    /**
     * 사용자의 모든 연동 정보 조회
     * @param user 사용자
     * @return 연동 정보 목록
     */
    List<UserIdentity> findByUser(User user);

    /**
     * 사용자의 특정 프로바이더 연동 정보 조회
     * @param user 사용자
     * @param provider 인증 프로바이더
     * @return UserIdentity
     */
    Optional<UserIdentity> findByUserAndProvider(User user, AuthProvider provider);

    /**
     * 사용자가 특정 프로바이더를 연동했는지 확인
     * @param user 사용자
     * @param provider 인증 프로바이더
     * @return 연동 여부
     */
    boolean existsByUserAndProvider(User user, AuthProvider provider);
}
