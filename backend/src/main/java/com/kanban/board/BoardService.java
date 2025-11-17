package com.kanban.board;

import com.kanban.activity.ActivityEventType;
import com.kanban.activity.ActivityService;
import com.kanban.activity.ActivityScopeType;
import com.kanban.board.dto.BoardResponse;
import com.kanban.board.dto.CreateBoardRequest;
import com.kanban.board.dto.UpdateBoardRequest;
import com.kanban.board.member.BoardMemberRepository;
import com.kanban.common.SecurityUtil;
import com.kanban.user.User;
import com.kanban.user.UserRepository;
import com.kanban.workspace.Workspace;
import com.kanban.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final BoardMemberRepository boardMemberRepository;

    /**
     * 새로운 보드를 생성한다.
     * - 워크스페이스와 사용자가 유효한지 확인
     * - 보드를 생성하고 소유자로 등록
     * - 생성자를 보드 멤버로 추가 (추후 구현)
     */
    public BoardResponse createBoard(Long workspaceId, Long userId, CreateBoardRequest request) {
        log.debug("Creating board in workspace {} by user {}", workspaceId, userId);

        // 워크스페이스와 사용자 조회
        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다"));

        User owner = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 보드 이름 검증 (워크스페이스 내 중복 확인은 정책에 따라 선택적)
        // 현재는 중복 허용으로 구현, 필요시 변경 가능

        // 보드 생성
        Board board = Board.builder()
            .workspace(workspace)
            .owner(owner)
            .name(request.getName())
            .description(request.getDescription())
            .themeColor(request.getThemeColor())
            .icon(request.getIcon())
            .status(BoardStatus.ACTIVE)
            .build();

        Board savedBoard = boardRepository.save(board);
        log.info("Board created successfully with id: {}", savedBoard.getId());

        // 활동 기록
        activityService.recordActivity(
            ActivityScopeType.BOARD,
            savedBoard.getId(),
            ActivityEventType.BOARD_CREATED,
            userId,
            owner.getName() + "님이 \"" + savedBoard.getName() + "\" 보드를 생성했습니다"
        );

        return BoardResponse.from(savedBoard);
    }

    /**
     * 보드 정보를 조회한다.
     * 현재 사용자의 초대 상태 정보 및 권한도 함께 반환한다.
     * 접근 권한: owner 또는 ACCEPTED 멤버만 조회 가능
     */
    @Transactional(readOnly = true)
    public BoardResponse getBoard(Long workspaceId, Long boardId) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("보드를 찾을 수 없습니다"));

        // DELETED 상태인 보드는 조회 불가
        if (board.getStatus() == BoardStatus.DELETED) {
            throw new IllegalArgumentException("삭제된 보드입니다");
        }

        // 현재 사용자의 접근 권한 확인
        Long currentUserId = SecurityUtil.getCurrentUserId();

        // Owner인 경우 항상 접근 가능 (MANAGER 권한)
        if (board.getOwner().getId().equals(currentUserId)) {
            var memberOptional = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUserId);
            if (memberOptional.isPresent()) {
                var member = memberOptional.get();
                return BoardResponse.fromWithInvitation(board, member.getInvitationStatus(), member.getInvitationToken());
            }
            return BoardResponse.from(board);
        }

        // Owner가 아닌 경우, ACCEPTED 멤버만 접근 가능
        var memberOptional = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUserId);
        if (memberOptional.isEmpty()) {
            throw new IllegalArgumentException("보드에 접근할 권한이 없습니다");
        }

        var member = memberOptional.get();
        if (member.getInvitationStatus() != com.kanban.board.member.InvitationStatus.ACCEPTED) {
            throw new IllegalArgumentException("초대를 수락하지 않아 보드에 접근할 수 없습니다");
        }

        return BoardResponse.fromWithRole(board, member.getRole(), member.getInvitationStatus(), member.getInvitationToken());
    }

    /**
     * 워크스페이스의 활성 보드 목록을 조회한다.
     * 현재 사용자가 멤버인 보드만 반환한다. (owner가 만든 보드 + 초대받은 보드)
     * PENDING 상태의 초대는 제외한다.
     */
    @Transactional(readOnly = true)
    public List<BoardResponse> getBoardsInWorkspace(Long workspaceId) {
        log.debug("Fetching boards for workspace {}", workspaceId);

        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 현재 사용자가 owner인 보드 조회
        var ownerBoards = boardRepository.findActiveByWorkspaceIdAndOwnerId(workspaceId, currentUserId);

        // 현재 사용자가 ACCEPTED 멤버인 보드 조회
        var memberBoards = boardMemberRepository.findByUserIdAndInvitationStatusOrderByCreatedAtDesc(
            currentUserId,
            com.kanban.board.member.InvitationStatus.ACCEPTED
        ).stream()
            .map(bm -> bm.getBoard())
            .filter(board -> board.getStatus() == BoardStatus.ACTIVE)
            .toList();

        // Owner 보드와 멤버 보드 합치기 (중복 제거)
        java.util.Map<Long, Board> allBoards = new java.util.HashMap<>();
        ownerBoards.forEach(board -> allBoards.put(board.getId(), board));
        memberBoards.forEach(board -> allBoards.putIfAbsent(board.getId(), board));

        // 최종 응답 구성 (멤버 정보 및 권한 포함)
        var userBoards = allBoards.values().stream()
            .filter(board -> {
                // Owner인 경우는 항상 포함
                if (board.getOwner().getId().equals(currentUserId)) {
                    return true;
                }

                // Owner가 아닌 경우, ACCEPTED 멤버만 포함
                var memberOpt = boardMemberRepository.findByBoardIdAndUserId(board.getId(), currentUserId);
                if (memberOpt.isEmpty()) {
                    return false;
                }

                var member = memberOpt.get();
                // PENDING 상태인 초대는 제외
                return member.getInvitationStatus() == com.kanban.board.member.InvitationStatus.ACCEPTED;
            })
            .map(board -> {
                // Owner인 경우 MANAGER 권한
                if (board.getOwner().getId().equals(currentUserId)) {
                    var memberOpt = boardMemberRepository.findByBoardIdAndUserId(board.getId(), currentUserId);
                    if (memberOpt.isPresent()) {
                        var member = memberOpt.get();
                        return BoardResponse.fromWithInvitation(board, member.getInvitationStatus(), member.getInvitationToken());
                    }
                    return BoardResponse.from(board);
                }

                // 멤버인 경우 역할 기반 권한
                var memberOpt = boardMemberRepository.findByBoardIdAndUserId(board.getId(), currentUserId);
                if (memberOpt.isPresent()) {
                    var member = memberOpt.get();
                    return BoardResponse.fromWithRole(board, member.getRole(), member.getInvitationStatus(), member.getInvitationToken());
                }
                return BoardResponse.from(board);
            })
            .toList();

        return userBoards;
    }

    /**
     * 워크스페이스의 최근 활성 보드를 조회한다. (대시보드용)
     * 모든 워크스페이스의 보드를 대상으로 하며, PENDING 상태의 초대는 제외한다.
     */
    @Transactional(readOnly = true)
    public List<BoardResponse> getRecentBoardsInWorkspace(Long workspaceId, int limit) {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        return boardRepository.findRecentActiveBoardsAllWorkspaces(limit).stream()
            .filter(board -> {
                // 현재 사용자가 멤버인지 확인
                var memberOpt = boardMemberRepository.findByBoardIdAndUserId(board.getId(), currentUserId);

                // PENDING 상태인 초대는 제외
                if (memberOpt.isPresent() && memberOpt.get().getInvitationStatus() == com.kanban.board.member.InvitationStatus.PENDING) {
                    return false;
                }

                // Owner이거나 ACCEPTED 멤버인 경우만 포함
                return board.getOwner().getId().equals(currentUserId) ||
                       (memberOpt.isPresent() && memberOpt.get().getInvitationStatus() == com.kanban.board.member.InvitationStatus.ACCEPTED);
            })
            .map(BoardResponse::from)
            .toList();
    }

    /**
     * 보드 정보를 업데이트한다.
     * - 소유자만 업데이트 가능 (향후 MANAGER 권한도 추가)
     */
    public BoardResponse updateBoard(Long workspaceId, Long boardId, Long userId, UpdateBoardRequest request) {
        log.debug("Updating board {} in workspace {} by user {}", boardId, workspaceId, userId);

        Board board = boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("보드를 찾을 수 없습니다"));

        // 권한 확인: 소유자만 수정 가능
        if (!board.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("보드를 수정할 권한이 없습니다");
        }

        // DELETED 상태인 보드는 수정 불가
        if (board.getStatus() == BoardStatus.DELETED) {
            throw new IllegalArgumentException("삭제된 보드는 수정할 수 없습니다");
        }

        // 필드 업데이트 (Null인 필드는 무시)
        if (request.getName() != null) {
            board.setName(request.getName());
        }
        if (request.getDescription() != null) {
            board.setDescription(request.getDescription());
        }
        if (request.getThemeColor() != null) {
            board.setThemeColor(request.getThemeColor());
        }
        if (request.getIcon() != null) {
            board.setIcon(request.getIcon());
        }

        Board updatedBoard = boardRepository.save(board);
        log.info("Board {} updated successfully", boardId);

        // 활동 기록
        activityService.recordActivity(
            ActivityScopeType.BOARD,
            boardId,
            ActivityEventType.BOARD_UPDATED,
            userId,
            "\"" + board.getName() + "\" 보드가 업데이트되었습니다"
        );

        return BoardResponse.from(updatedBoard);
    }

    /**
     * 보드를 아카이브한다.
     */
    public BoardResponse archiveBoard(Long workspaceId, Long boardId, Long userId) {
        log.debug("Archiving board {} in workspace {} by user {}", boardId, workspaceId, userId);

        Board board = boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("보드를 찾을 수 없습니다"));

        // 권한 확인: 소유자만 아카이브 가능
        if (!board.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("보드를 아카이브할 권한이 없습니다");
        }

        board.archive();
        Board updatedBoard = boardRepository.save(board);
        log.info("Board {} archived successfully", boardId);

        return BoardResponse.from(updatedBoard);
    }

    /**
     * 아카이브된 보드를 복구한다.
     */
    public BoardResponse unarchiveBoard(Long workspaceId, Long boardId, Long userId) {
        log.debug("Unarchiving board {} in workspace {} by user {}", boardId, workspaceId, userId);

        Board board = boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("보드를 찾을 수 없습니다"));

        // 권한 확인
        if (!board.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("보드를 복구할 권한이 없습니다");
        }

        board.unarchive();
        Board updatedBoard = boardRepository.save(board);
        log.info("Board {} unarchived successfully", boardId);

        return BoardResponse.from(updatedBoard);
    }

    /**
     * 보드를 삭제한다 (소프트 삭제).
     * - DELETED 상태로 변경하고 deletedAt 타임스탐프 기록
     * - 30일 후 배치 작업으로 물리 삭제
     */
    public void deleteBoard(Long workspaceId, Long boardId, Long userId) {
        log.debug("Deleting board {} in workspace {} by user {}", boardId, workspaceId, userId);

        Board board = boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("보드를 찾을 수 없습니다"));

        // 권한 확인: 소유자만 삭제 가능
        if (!board.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("보드를 삭제할 권한이 없습니다");
        }

        // DELETED 상태인 보드는 다시 삭제 불가
        if (board.getStatus() == BoardStatus.DELETED) {
            throw new IllegalArgumentException("이미 삭제된 보드입니다");
        }

        board.markAsDeleted();
        boardRepository.save(board);
        log.info("Board {} deleted (soft delete) successfully", boardId);
    }

    /**
     * 삭제된 보드를 복구한다.
     */
    public BoardResponse restoreBoard(Long workspaceId, Long boardId, Long userId) {
        log.debug("Restoring board {} in workspace {} by user {}", boardId, workspaceId, userId);

        Board board = boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("보드를 찾을 수 없습니다"));

        // 권한 확인
        if (!board.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("보드를 복구할 권한이 없습니다");
        }

        // DELETED 상태인 보드만 복구 가능
        if (board.getStatus() != BoardStatus.DELETED) {
            throw new IllegalArgumentException("삭제되지 않은 보드는 복구할 수 없습니다");
        }

        board.restore();
        Board restoredBoard = boardRepository.save(board);
        log.info("Board {} restored successfully", boardId);

        return BoardResponse.from(restoredBoard);
    }

    /**
     * 배치 작업: 30일 이상 경과된 삭제된 보드를 물리 삭제한다.
     */
    @Transactional
    public void purgePermanentlyDeletedBoards() {
        log.info("Starting purge of permanently deleted boards");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<Board> boardsToPurge = boardRepository.findPermanentlyDeletableBoards(cutoffDate);

        if (boardsToPurge.isEmpty()) {
            log.info("No boards to purge");
            return;
        }

        boardRepository.deleteAll(boardsToPurge);
        log.info("Purged {} permanently deleted boards", boardsToPurge.size());
    }
}
