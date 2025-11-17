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

    @JsonIgnore
    @Column(nullable = true)  // OAuth 사용자는 비밀번호가 없을 수 있음
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /**
     * 최초 가입 경로 (LOCAL, GOOGLE 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_provider", nullable = false, length = 20)
    @Builder.Default
    private com.kanban.auth.AuthProvider primaryProvider = com.kanban.auth.AuthProvider.LOCAL;

    private LocalDateTime lastLoginAt;
}
