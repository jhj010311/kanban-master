package com.kanban.board.dto;

import com.kanban.board.Board;
import com.kanban.board.BoardStatus;
import com.kanban.board.member.BoardMemberRole;
import com.kanban.board.member.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponse {

    private Long id;

    private Long workspaceId;

    private Long ownerId;

    private String ownerName;

    private String name;

    private String description;

    private String themeColor;

    private String icon;

    private BoardStatus status;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 현재 사용자의 초대 상태 (null이면 owner)
     */
    private InvitationStatus invitationStatus;

    /**
     * 현재 사용자의 초대 토큰 (초대 대기 중일 때만)
     */
    private String invitationToken;

    /**
     * JPQL 생성자 표현식용 생성자 (14개 파라미터)
     * Hibernate 6.4+에서 NULL 값 처리를 위해 필요
     */
    public BoardResponse(
        Long id, Long workspaceId, Long ownerId, String ownerName,
        String name, String description, String themeColor, String icon,
        BoardStatus status, LocalDateTime deletedAt, LocalDateTime createdAt, LocalDateTime updatedAt,
        InvitationStatus invitationStatus, String invitationToken
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.name = name;
        this.description = description;
        this.themeColor = themeColor;
        this.icon = icon;
        this.status = status;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.invitationStatus = invitationStatus;
        this.invitationToken = invitationToken;
        // 나머지 필드들은 null로 초기화됨
        this.currentUserRole = null;
        this.canEdit = null;
        this.canManage = null;
    }

    /**
     * 현재 사용자의 보드 내 역할 (VIEWER, EDITOR, MANAGER)
     * Owner인 경우 MANAGER로 설정됨
     */
    private BoardMemberRole currentUserRole;

    /**
     * 현재 사용자가 편집 권한이 있는지 (EDITOR 이상)
     */
    private Boolean canEdit;

    /**
     * 현재 사용자가 관리 권한이 있는지 (MANAGER 또는 Owner)
     */
    private Boolean canManage;

    /**
     * Entity를 Response DTO로 변환 (Owner용 - MANAGER 권한)
     */
    public static BoardResponse from(Board board) {
        return BoardResponse.builder()
            .id(board.getId())
            .workspaceId(board.getWorkspace().getId())
            .ownerId(board.getOwner().getId())
            .ownerName(board.getOwner().getName())
            .name(board.getName())
            .description(board.getDescription())
            .themeColor(board.getThemeColor())
            .icon(board.getIcon())
            .status(board.getStatus())
            .deletedAt(board.getDeletedAt())
            .createdAt(board.getCreatedAt())
            .updatedAt(board.getUpdatedAt())
            .invitationStatus(null)
            .invitationToken(null)
            .currentUserRole(BoardMemberRole.MANAGER)
            .canEdit(true)
            .canManage(true)
            .build();
    }

    /**
     * Entity를 Response DTO로 변환 (초대 상태 및 권한 포함)
     */
    public static BoardResponse fromWithInvitation(Board board, InvitationStatus status, String token) {
        return BoardResponse.builder()
            .id(board.getId())
            .workspaceId(board.getWorkspace().getId())
            .ownerId(board.getOwner().getId())
            .ownerName(board.getOwner().getName())
            .name(board.getName())
            .description(board.getDescription())
            .themeColor(board.getThemeColor())
            .icon(board.getIcon())
            .status(board.getStatus())
            .deletedAt(board.getDeletedAt())
            .createdAt(board.getCreatedAt())
            .updatedAt(board.getUpdatedAt())
            .invitationStatus(status)
            .invitationToken(token)
            .currentUserRole(BoardMemberRole.MANAGER)
            .canEdit(true)
            .canManage(true)
            .build();
    }

    /**
     * Entity를 Response DTO로 변환 (멤버용 - 역할 기반 권한)
     */
    public static BoardResponse fromWithRole(Board board, BoardMemberRole role, InvitationStatus status, String token) {
        boolean canEdit = role == BoardMemberRole.EDITOR || role == BoardMemberRole.MANAGER;
        boolean canManage = role == BoardMemberRole.MANAGER;

        return BoardResponse.builder()
            .id(board.getId())
            .workspaceId(board.getWorkspace().getId())
            .ownerId(board.getOwner().getId())
            .ownerName(board.getOwner().getName())
            .name(board.getName())
            .description(board.getDescription())
            .themeColor(board.getThemeColor())
            .icon(board.getIcon())
            .status(board.getStatus())
            .deletedAt(board.getDeletedAt())
            .createdAt(board.getCreatedAt())
            .updatedAt(board.getUpdatedAt())
            .invitationStatus(status)
            .invitationToken(token)
            .currentUserRole(role)
            .canEdit(canEdit)
            .canManage(canManage)
            .build();
    }
}
