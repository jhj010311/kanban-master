package com.kanban.workspace;

import com.kanban.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 워크스페이스 초기화 담당 서비스.
 */
@Service
@RequiredArgsConstructor
public class UserWorkspaceService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;

    /**
     * 사용자가 소속된 워크스페이스가 없으면 기본 워크스페이스와 멤버십을 생성한다.
     */
    @Transactional
    public void ensureUserHasWorkspace(User user) {
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
        }
    }

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
