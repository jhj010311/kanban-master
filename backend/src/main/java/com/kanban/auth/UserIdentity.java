package com.kanban.auth;

import com.kanban.entity.BaseEntity;
import com.kanban.user.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * OAuth2 프로바이더 사용자 식별 정보 엔티티
 * Spec § 6. 백엔드 규격 - UserIdentity 엔티티
 * FR-06b: UserIdentity 엔티티 - 외부 프로바이더의 사용자 ID 저장
 */
@Entity
@Table(
    name = "user_identities",
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_provider_user", columnList = "provider, provider_user_id", unique = true)
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연결된 사용자
     * Spec § 6: User와 1:N 관계 (한 사용자가 여러 소셜 계정 연동 가능)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * OAuth2 프로바이더 (GOOGLE, KAKAO, NAVER)
     * Spec § 6: provider 필드는 Enum 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuth2Provider provider;

    /**
     * 프로바이더의 사용자 고유 ID
     * Spec § 6: provider_user_id는 String(255)
     * Google의 경우 'sub' 클레임 값
     */
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    /**
     * 프로바이더에서 제공한 이메일
     * Spec § 6: email은 String(150)
     */
    @Column(nullable = false, length = 150)
    private String email;

    /**
     * 프로바이더에서 제공한 이름
     * Spec § 6: name은 String(100)
     */
    @Column(length = 100)
    private String name;

    /**
     * 프로바이더에서 제공한 프로필 이미지 URL (선택)
     * FR-06i: 프로바이더별 사용자 정보 매핑
     */
    @Column(length = 512)
    private String profileImageUrl;

    // BaseEntity의 createdAt, updatedAt 상속
}
