package com.kanban.user;

import com.kanban.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * 비밀번호 해시
     * Spec § 6. 백엔드 규격 - User 엔티티 변경
     * FR-06c: 소셜 로그인 전용 사용자 지원을 위해 nullable 허용
     */
    @JsonIgnore
    @Column(nullable = true)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    private LocalDateTime lastLoginAt;
}
