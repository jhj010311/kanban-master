package com.kanban.board.member;

import com.kanban.activity.ActivityEventType;
import com.kanban.activity.ActivityService;
import com.kanban.activity.ActivityScopeType;
import com.kanban.board.Board;
import com.kanban.board.BoardRepository;
import com.kanban.board.member.dto.BoardMemberResponse;
import com.kanban.user.User;
import com.kanban.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 보드 멤버 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final BoardMemberRepository boardMemberRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;

    /**
     * 초대 토큰 생성
     */
    private String generateInvitationToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * 멤버 초대
     *
     * @param boardId 보드 ID
     * @param userId  초대받을 사용자 ID
     * @param invitedByUserId 초대하는 사용자 ID
     * @param role    초대할 권한
     * @return 생성된 BoardMemberResponse
     */
    @Transactional
    public BoardMemberResponse inviteMember(Long boardId, Long userId, Long invitedByUserId, BoardMemberRole role) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("보드를 찾을 수 없습니다: " + boardId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        User invitedByUser = userRepository.findById(invitedByUserId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + invitedByUserId));

        // 이미 멤버인지 확인
        if (boardMemberRepository.findByBoardIdAndUserId(boardId, userId).isPresent()) {
            throw new IllegalArgumentException("이미 보드의 멤버입니다");
        }

        // 새 멤버 생성
        BoardMember member = BoardMember.builder()
            .id(BoardMemberId.builder()
                .boardId(boardId)
                .userId(userId)
                .build())
            .board(board)
            .user(user)
            .role(role)
            .invitationStatus(InvitationStatus.PENDING)
            .invitationToken(generateInvitationToken())
            .invitedAt(LocalDateTime.now())
            .build();

        BoardMember savedMember = boardMemberRepository.save(member);

        // 활동 기록
        activityService.recordActivity(
            ActivityScopeType.BOARD,
            boardId,
            ActivityEventType.MEMBER_INVITED,
            invitedByUserId,
            invitedByUser.getName() + "님이 " + user.getName() + "님을 초대했습니다"
        );

        log.info("Member invited - Board: {}, User: {}, Role: {}", boardId, userId, role);
        return BoardMemberResponse.from(savedMember);
    }

    /**
     * 초대 수락 (멱등성 보장: 이미 ACCEPTED인 경우 성공으로 반환)
     *
     * @param token 초대 토큰
     */
    @Transactional
    public BoardMemberResponse acceptInvitation(String token) {
        BoardMember member = boardMemberRepository.findByInvitationToken(token)
            .orElseThrow(() -> new IllegalArgumentException("초대 토큰이 유효하지 않습니다"));

        // 이미 ACCEPTED 상태라면 중복 요청이므로 그대로 반환 (멱등성)
        if (member.getInvitationStatus() == InvitationStatus.ACCEPTED) {
            log.debug("Invitation already accepted (idempotent request) - Board: {}, User: {}",
                member.getBoard().getId(), member.getUser().getId());
            return BoardMemberResponse.from(member);
        }

        // DECLINED나 EXPIRED 상태인 경우는 에러
        if (member.getInvitationStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("이미 처리된 초대입니다");
        }

        member.acceptInvitation();
        BoardMember savedMember = boardMemberRepository.save(member);

        // 활동 기록
        activityService.recordActivity(
            ActivityScopeType.BOARD,
            member.getBoard().getId(),
            ActivityEventType.MEMBER_INVITED,
            member.getUser().getId(),
            member.getUser().getName() + "님이 초대를 수락했습니다"
        );

        log.info("Invitation accepted - Board: {}, User: {}", member.getBoard().getId(), member.getUser().getId());
        return BoardMemberResponse.from(savedMember);
    }

    /**
     * 초대 거절 (멱등성 보장: 이미 DECLINED인 경우 성공으로 반환)
     *
     * @param token 초대 토큰
     */
    @Transactional
    public void declineInvitation(String token) {
        BoardMember member = boardMemberRepository.findByInvitationToken(token)
            .orElseThrow(() -> new IllegalArgumentException("초대 토큰이 유효하지 않습니다"));

        // 이미 DECLINED 상태라면 중복 요청이므로 그대로 반환 (멱등성)
        if (member.getInvitationStatus() == InvitationStatus.DECLINED) {
            log.debug("Invitation already declined (idempotent request) - Board: {}, User: {}",
                member.getBoard().getId(), member.getUser().getId());
            return;
        }

        // ACCEPTED나 EXPIRED 상태인 경우는 에러
        if (member.getInvitationStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("이미 처리된 초대입니다");
        }

        member.declineInvitation();
        boardMemberRepository.save(member);

        log.info("Invitation declined - Board: {}, User: {}", member.getBoard().getId(), member.getUser().getId());
    }

    /**
     * 멤버 권한 변경
     *
     * @param boardId 보드 ID
     * @param memberId 멤버 사용자 ID
     * @param newRole 새로운 권한
     * @param changedByUserId 권한을 변경하는 사용자 ID
     */
    @Transactional
    public BoardMemberResponse changeMemberRole(Long boardId, Long memberId, BoardMemberRole newRole, Long changedByUserId) {
        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, memberId)
            .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다"));

        User changedByUser = userRepository.findById(changedByUserId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + changedByUserId));

        String oldRole = member.getRole().toString();
        member.setRole(newRole);
        BoardMember savedMember = boardMemberRepository.save(member);

        // 활동 기록
        activityService.recordActivity(
            ActivityScopeType.BOARD,
            boardId,
            ActivityEventType.MEMBER_ROLE_CHANGED,
            changedByUserId,
            changedByUser.getName() + "님이 " + member.getUser().getName() + "님의 권한을 변경했습니다 (" + oldRole + " → " + newRole + ")"
        );

        log.info("Member role changed - Board: {}, User: {}, NewRole: {}", boardId, memberId, newRole);
        return BoardMemberResponse.from(savedMember);
    }

    /**
     * 멤버 제거
     *
     * @param boardId 보드 ID
     * @param memberId 제거할 멤버 사용자 ID
     * @param removedByUserId 제거하는 사용자 ID
     */
    @Transactional
    public void removeMember(Long boardId, Long memberId, Long removedByUserId) {
        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, memberId)
            .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다"));

        User removedByUser = userRepository.findById(removedByUserId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + removedByUserId));

        String memberName = member.getUser().getName();
        boardMemberRepository.delete(member);

        // 활동 기록
        activityService.recordActivity(
            ActivityScopeType.BOARD,
            boardId,
            ActivityEventType.MEMBER_REMOVED,
            removedByUserId,
            removedByUser.getName() + "님이 " + memberName + "님을 제거했습니다"
        );

        log.info("Member removed - Board: {}, User: {}", boardId, memberId);
    }

    /**
     * 보드의 모든 멤버 조회
     */
    public List<BoardMemberResponse> getBoardMembers(Long boardId) {
        return boardMemberRepository.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
                .map(BoardMemberResponse::from)
                .toList();
    }

    /**
     * 보드의 모든 수락된 멤버 조회
     */
    public List<BoardMemberResponse> getBoardAcceptedMembers(Long boardId) {
        return boardMemberRepository.findByBoardIdAndInvitationStatusOrderByCreatedAtAsc(boardId, InvitationStatus.ACCEPTED).stream()
                .map(BoardMemberResponse::from)
                .toList();
    }

    /**
     * 보드의 멤버 페이지네이션 조회
     */
    public Page<BoardMemberResponse> getBoardMembersPage(Long boardId, Pageable pageable) {
        return boardMemberRepository.findByBoardIdOrderByCreatedAtAsc(boardId, pageable)
                .map(BoardMemberResponse::from);
    }

    /**
     * 보드의 멤버 페이지네이션 조회 (응답 DTO로 변환 - 트랜잭션 내에서 처리)
     */
    public Page<BoardMemberResponse> getBoardMembersPageResponse(Long boardId, Pageable pageable) {
        Page<BoardMember> members = boardMemberRepository.findByBoardIdOrderByCreatedAtAsc(boardId, pageable);
        return members.map(BoardMemberResponse::from);
    }

    /**
     * 멤버 정보 조회
     */
    public BoardMemberResponse getMember(Long boardId, Long userId) {
        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
            .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다"));
        return BoardMemberResponse.from(member);
    }

    /**
     * 사용자의 모든 보드 멤버십 조회
     */
    public List<BoardMemberResponse> getUserBoardMemberships(Long userId) {
        return boardMemberRepository.findByUserIdAndInvitationStatus(userId, InvitationStatus.ACCEPTED).stream()
                .map(BoardMemberResponse::from)
                .toList();
    }

    /**
     * 만료된 초대 처리 (72시간 이상)
     */
    @Transactional
    public void expireOldInvitations(Long boardId) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(72);
        List<BoardMember> expiredMembers = boardMemberRepository.findExpiredInvitations(boardId, cutoffDate);

        for (BoardMember member : expiredMembers) {
            member.expireInvitation();
            boardMemberRepository.save(member);
            log.info("Invitation expired - Board: {}, User: {}", boardId, member.getUser().getId());
        }
    }

    /**
     * 보드 멤버 수 조회 (수락된 멤버만)
     */
    public long getBoardMemberCount(Long boardId) {
        return boardMemberRepository.countByBoardIdAndInvitationStatus(boardId, InvitationStatus.ACCEPTED);
    }

    /**
     * 사용자의 대기 중인 초대 조회
     */
    public List<BoardMemberResponse> getPendingInvitations(Long userId) {
        return boardMemberRepository.findPendingInvitationsByUserId(userId, InvitationStatus.PENDING).stream()
                .map(BoardMemberResponse::from)
                .toList();
    }

    /**
     * 사용자의 모든 초대 조회 (디버깅용)
     */
    public List<BoardMemberResponse> getAllInvitations(Long userId) {
        return boardMemberRepository.findAllInvitationsByUserId(userId).stream()
                .map(BoardMemberResponse::from)
                .toList();
    }
}
