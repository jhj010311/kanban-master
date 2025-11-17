package com.kanban.comment;

import com.kanban.activity.ActivityEventType;
import com.kanban.activity.ActivityService;
import com.kanban.activity.ActivityScopeType;
import com.kanban.board.Board;
import com.kanban.board.BoardRepository;
import com.kanban.board.member.BoardMemberRole;
import com.kanban.board.member.BoardMemberRoleValidator;
import com.kanban.card.Card;
import com.kanban.card.CardRepository;
import com.kanban.comment.dto.CommentResponse;
import com.kanban.comment.dto.CreateCommentRequest;
import com.kanban.comment.dto.UpdateCommentRequest;
import com.kanban.common.SecurityUtil;
import com.kanban.exception.ResourceNotFoundException;
import com.kanban.user.User;
import com.kanban.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.html.PolicyFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 댓글 비즈니스 로직 서비스
 *
 * Spec § 6. 백엔드 규격 - Service
 * - HTML Sanitization 적용
 * - Activity 로그 기록
 * - 권한 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final ActivityService activityService;
    private final BoardMemberRoleValidator roleValidator;
    private final PolicyFactory htmlSanitizerPolicy;

    /**
     * 댓글 목록 조회 (페이지네이션)
     *
     * Spec § 6: 카드의 삭제되지 않은 댓글 최신순 조회
     * Spec § FR-06b: 댓글 목록 조회
     * Spec § FR-06f: 페이지네이션 (기본 20개/페이지)
     *
     * @param boardId 보드 ID
     * @param cardId 카드 ID
     * @param pageable 페이지네이션 정보
     * @return 댓글 페이지
     */
    public Page<CommentResponse> getComments(Long boardId, Long cardId, Pageable pageable) {
        // 권한 검증: 보드 멤버 (VIEWER 이상)
        roleValidator.validateRole(boardId, BoardMemberRole.VIEWER);

        // 카드 존재 확인
        cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));

        // 댓글 조회 (최신순)
        Page<Comment> comments = commentRepository.findByCardIdAndIsDeletedFalseOrderByCreatedAtDesc(
                cardId,
                pageable
        );

        log.info("Retrieved {} comments for card {}", comments.getTotalElements(), cardId);

        return comments.map(CommentResponse::from);
    }

    /**
     * 댓글 생성
     *
     * Spec § 6: HTML sanitization, Activity 로그 기록
     * Spec § FR-06a: 댓글 작성
     * Spec § FR-06g: 빈 댓글 방지 (Validation으로 처리)
     * Spec § FR-06h: Activity 로그 기록
     * Spec § FR-06i: XSS 방지
     *
     * @param boardId 보드 ID
     * @param cardId 카드 ID
     * @param request 댓글 생성 요청
     * @return 생성된 댓글
     */
    @Transactional
    public CommentResponse createComment(Long boardId, Long cardId, CreateCommentRequest request) {
        // 권한 검증: 보드 멤버 (VIEWER 이상)
        roleValidator.validateRole(boardId, BoardMemberRole.VIEWER);

        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 카드 존재 확인
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));

        // 사용자 조회
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        // HTML Sanitization
        // Spec § FR-06i: XSS 방지를 위한 HTML sanitization
        String sanitizedContent = sanitizeHtml(request.getContent());

        // 댓글 생성
        Comment comment = Comment.builder()
                .card(card)
                .author(author)
                .content(sanitizedContent)
                .isDeleted(false)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // Activity 로그 기록
        // Spec § FR-06h: COMMENT_ADDED 이벤트 기록
        activityService.recordActivity(
                ActivityScopeType.CARD,
                cardId,
                ActivityEventType.COMMENT_ADDED,
                currentUserId,
                String.format("%s님이 댓글을 작성했습니다", author.getName()),
                null
        );

        log.info("Comment created - ID: {}, Card: {}, Author: {}", savedComment.getId(), cardId, currentUserId);

        return CommentResponse.from(savedComment);
    }

    /**
     * 댓글 수정
     *
     * Spec § 6: 작성자 본인만 수정 가능, HTML sanitization
     * Spec § FR-06c: 댓글 수정
     * Spec § FR-06i: XSS 방지
     *
     * @param boardId 보드 ID
     * @param cardId 카드 ID
     * @param commentId 댓글 ID
     * @param request 댓글 수정 요청
     * @return 수정된 댓글
     */
    @Transactional
    public CommentResponse updateComment(Long boardId, Long cardId, Long commentId, UpdateCommentRequest request) {
        // 권한 검증: 보드 멤버 (VIEWER 이상)
        roleValidator.validateRole(boardId, BoardMemberRole.VIEWER);

        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 댓글 존재 확인 (삭제되지 않은 것만)
        Comment comment = commentRepository.findByIdAndCardIdAndIsDeletedFalse(commentId, cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        // 작성자 본인 확인
        // Spec § FR-06c: 댓글 작성자 본인만 수정 가능
        if (!comment.getAuthor().getId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "댓글을 수정할 권한이 없습니다"
            );
        }

        // HTML Sanitization
        // Spec § FR-06i: XSS 방지를 위한 HTML sanitization
        String sanitizedContent = sanitizeHtml(request.getContent());

        // 댓글 수정
        comment.setContent(sanitizedContent);
        // updatedAt은 BaseEntity의 @LastModifiedDate로 자동 갱신

        Comment updatedComment = commentRepository.save(comment);

        log.info("Comment updated - ID: {}, Card: {}, Author: {}", commentId, cardId, currentUserId);

        return CommentResponse.from(updatedComment);
    }

    /**
     * 댓글 삭제 (Soft Delete)
     *
     * Spec § 6: Soft delete 처리, Activity 로그 기록
     * Spec § FR-06d: 작성자 또는 보드 OWNER만 삭제 가능
     * Spec § FR-06h: Activity 로그 기록
     * Spec § FR-06n: Soft Delete 방식 사용
     *
     * @param boardId 보드 ID
     * @param cardId 카드 ID
     * @param commentId 댓글 ID
     */
    @Transactional
    public void deleteComment(Long boardId, Long cardId, Long commentId) {
        // 권한 검증: 보드 멤버 (VIEWER 이상)
        roleValidator.validateRole(boardId, BoardMemberRole.VIEWER);

        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 댓글 존재 확인 (삭제되지 않은 것만)
        Comment comment = commentRepository.findByIdAndCardIdAndIsDeletedFalse(commentId, cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        // 보드 조회 (OWNER 확인용)
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found: " + boardId));

        // 삭제 권한 확인: 작성자 본인 OR 보드 OWNER
        // Spec § FR-06d: 댓글 작성자 또는 보드 OWNER가 댓글을 삭제할 수 있다
        boolean isAuthor = comment.getAuthor().getId().equals(currentUserId);
        boolean isOwner = board.getOwner().getId().equals(currentUserId);

        if (!isAuthor && !isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "댓글을 삭제할 권한이 없습니다"
            );
        }

        // Soft Delete 처리
        // Spec § FR-06n: isDeleted 플래그로 관리
        comment.setIsDeleted(true);
        commentRepository.save(comment);

        // Activity 로그 기록
        // Spec § FR-06h: COMMENT_DELETED 이벤트 기록
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        activityService.recordActivity(
                ActivityScopeType.CARD,
                cardId,
                ActivityEventType.COMMENT_DELETED,
                currentUserId,
                String.format("%s님이 댓글을 삭제했습니다", currentUser.getName()),
                null
        );

        log.info("Comment soft deleted - ID: {}, Card: {}, Deleter: {}", commentId, cardId, currentUserId);
    }

    /**
     * HTML Sanitization
     *
     * Spec § FR-06i: XSS 방지를 위해 OWASP HTML Sanitizer 사용
     * Spec § 7: 허용 태그 - p, br, strong, em, u, strike, h1-h6, ul, ol, li, a, blockquote, code, pre
     *
     * @param html 원본 HTML
     * @return Sanitized HTML
     */
    private String sanitizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        return htmlSanitizerPolicy.sanitize(html);
    }
}
