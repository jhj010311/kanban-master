package com.kanban.auth.oauth;

import com.kanban.auth.AuthProvider;
import com.kanban.auth.UserIdentity;
import com.kanban.auth.UserIdentityRepository;
import com.kanban.user.User;
import com.kanban.user.UserRepository;
import com.kanban.user.UserStatus;
import com.kanban.workspace.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OAuth 2.0 소셜 로그인 처리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OAuth2Service {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    /**
     * OAuth 로그인 처리 및 계정 병합
     *
     * @param provider OAuth 프로바이더
     * @param oAuth2User Spring Security OAuth2User 객체
     * @return 연동된 User 객체
     */
    public User processOAuthLogin(AuthProvider provider, OAuth2User oAuth2User) {
        String providerId = extractProviderId(provider, oAuth2User);
        String email = extractEmail(oAuth2User);
        String name = extractName(oAuth2User);

        log.info("OAuth 로그인 시도: provider={}, providerId={}, email={}", provider, providerId, email);

        // 1. 기존 UserIdentity 조회
        UserIdentity existingIdentity = userIdentityRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElse(null);

        if (existingIdentity != null) {
            // 기존 연동 정보가 있으면 해당 User 반환
            User user = existingIdentity.getUser();
            user.setLastLoginAt(LocalDateTime.now());
            log.info("기존 OAuth 계정으로 로그인: userId={}", user.getId());
            return user;
        }

        // 2. 같은 이메일로 가입된 User 조회 (계정 병합)
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // 기존 User에 새로운 Identity 추가 (계정 병합)
            linkIdentity(user, provider, providerId, email);
            user.setLastLoginAt(LocalDateTime.now());
            log.info("기존 계정에 OAuth 연동: userId={}, provider={}", user.getId(), provider);
            return user;
        }

        // 3. 신규 사용자 생성
        user = createNewUser(email, name, provider);
        linkIdentity(user, provider, providerId, email);
        ensureUserHasWorkspace(user);

        log.info("신규 OAuth 계정 생성: userId={}, provider={}", user.getId(), provider);
        return user;
    }

    /**
     * 프로바이더별 고유 ID 추출
     */
    private String extractProviderId(AuthProvider provider, OAuth2User oAuth2User) {
        return switch (provider) {
            case GOOGLE -> oAuth2User.getAttribute("sub");
            case GITHUB -> String.valueOf(oAuth2User.getAttribute("id"));
            default -> throw new IllegalArgumentException("지원하지 않는 프로바이더: " + provider);
        };
    }

    /**
     * 이메일 추출 및 검증
     */
    private String extractEmail(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("OAuth 프로바이더가 이메일을 제공하지 않았습니다.");
        }

        // Google의 경우 email_verified 확인
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");
        if (emailVerified != null && !emailVerified) {
            throw new IllegalStateException("이메일이 검증되지 않았습니다.");
        }

        return email;
    }

    /**
     * 이름 추출
     */
    private String extractName(OAuth2User oAuth2User) {
        String name = oAuth2User.getAttribute("name");
        if (name == null || name.isBlank()) {
            // name이 없으면 email 앞부분 사용
            String email = oAuth2User.getAttribute("email");
            name = email != null ? email.split("@")[0] : "사용자";
        }
        return name;
    }

    /**
     * 신규 User 생성
     */
    private User createNewUser(String email, String name, AuthProvider primaryProvider) {
        User user = User.builder()
                .email(email)
                .name(name)
                .password(null)  // OAuth 사용자는 비밀번호 없음
                .status(UserStatus.ACTIVE)
                .primaryProvider(primaryProvider)
                .lastLoginAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    /**
     * UserIdentity 연동
     */
    private void linkIdentity(User user, AuthProvider provider, String providerId, String email) {
        // 이미 연동되어 있는지 확인
        if (userIdentityRepository.existsByUserAndProvider(user, provider)) {
            log.warn("이미 연동된 프로바이더: userId={}, provider={}", user.getId(), provider);
            return;
        }

        UserIdentity identity = UserIdentity.builder()
                .user(user)
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .linkedAt(LocalDateTime.now())
                .build();

        userIdentityRepository.save(identity);
    }

    /**
     * 사용자 워크스페이스 자동 생성
     */
    private void ensureUserHasWorkspace(User user) {
        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(user.getId());

        if (memberships.isEmpty()) {
            Workspace defaultWorkspace = Workspace.builder()
                    .name(user.getName() + "'s Workspace")
                    .slug(generateSlug(user))
                    .owner(user)
                    .build();
            Workspace savedWorkspace = workspaceRepository.save(defaultWorkspace);

            WorkspaceMember member = WorkspaceMember.builder()
                    .workspace(savedWorkspace)
                    .user(user)
                    .role(WorkspaceRole.OWNER)
                    .build();
            workspaceMemberRepository.save(member);

            log.info("기본 워크스페이스 생성: userId={}, workspaceId={}", user.getId(), savedWorkspace.getId());
        }
    }

    /**
     * 워크스페이스 slug 생성
     */
    private String generateSlug(User user) {
        String baseSlug = user.getEmail().split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "-");
        String slug = baseSlug;
        int counter = 1;

        while (workspaceRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}
