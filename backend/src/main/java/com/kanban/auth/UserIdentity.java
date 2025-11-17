package com.kanban.auth;

import com.kanban.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 외부 인증 프로바이더 연동 정보
 * 한 사용자는 여러 프로바이더를 통해 로그인할 수 있음
 */
@Entity
@Table(
    name = "user_identities",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "unique_user_provider",
            columnNames = {"user_id", "provider"}
        ),
        @UniqueConstraint(
            name = "unique_provider_id",
            columnNames = {"provider", "provider_id"}
        )
    },
    indexes = {
        @Index(name = "idx_user_identities_user_id", columnList = "user_id"),
        @Index(name = "idx_user_identities_provider_id", columnList = "provider, provider_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연결된 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 인증 프로바이더 종류
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    /**
     * 외부 프로바이더의 사용자 고유 ID
     * Google: sub 필드
     * GitHub: id 필드
     * LOCAL: user.email
     */
    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    /**
     * 프로바이더에서 받은 이메일
     * User.email과 다를 수 있음
     */
    @Column(length = 150)
    private String email;

    /**
     * 프로바이더 연결 시각
     */
    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    @PrePersist
    protected void onCreate() {
        if (linkedAt == null) {
            linkedAt = LocalDateTime.now();
        }
    }
}
