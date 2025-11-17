package com.kanban.comment;

import com.kanban.comment.dto.CommentResponse;
import com.kanban.comment.dto.CreateCommentRequest;
import com.kanban.comment.dto.UpdateCommentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 댓글 REST API 컨트롤러
 *
 * Spec § 5. API 명세
 * - GET /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments
 * - POST /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments
 * - PUT /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments/{commentId}
 * - DELETE /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments/{commentId}
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 목록 조회 (페이지네이션)
     *
     * Spec § 5.1: GET /comments
     * Spec § FR-06f: 페이지네이션 (기본 20개/페이지)
     *
     * @param workspaceId 워크스페이스 ID (경로 변수)
     * @param boardId 보드 ID
     * @param cardId 카드 ID
     * @param page 페이지 번호 (0부터 시작, 기본값 0)
     * @param size 페이지 크기 (기본값 20)
     * @return 댓글 페이지 응답
     */
    @GetMapping
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable Long workspaceId,
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/v1/workspaces/{}/boards/{}/cards/{}/comments?page={}&size={}",
                workspaceId, boardId, cardId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<CommentResponse> comments = commentService.getComments(boardId, cardId, pageable);

        return ResponseEntity.ok(comments);
    }

    /**
     * 댓글 생성
     *
     * Spec § 5.2: POST /comments
     * Spec § FR-06a: 댓글 작성
     * Spec § FR-06g: 빈 댓글 방지
     * Spec § FR-06h: Activity 로그 기록
     * Spec § FR-06i: XSS 방지
     *
     * @param workspaceId 워크스페이스 ID (경로 변수)
     * @param boardId 보드 ID
     * @param cardId 카드 ID
     * @param request 댓글 생성 요청
     * @return 생성된 댓글 응답
     */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long workspaceId,
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        log.info("POST /api/v1/workspaces/{}/boards/{}/cards/{}/comments - content length: {}",
                workspaceId, boardId, cardId, request.getContent().length());

        CommentResponse response = commentService.createComment(boardId, cardId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 댓글 수정
     *
     * Spec § 5.3: PUT /comments/{commentId}
     * Spec § FR-06c: 댓글 수정 (작성자 본인만 가능)
     * Spec § FR-06i: XSS 방지
     *
     * @param workspaceId 워크스페이스 ID (경로 변수)
     * @param boardId 보드 ID
     * @param cardId 카드 ID
     * @param commentId 댓글 ID
     * @param request 댓글 수정 요청
     * @return 수정된 댓글 응답
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long workspaceId,
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        log.info("PUT /api/v1/workspaces/{}/boards/{}/cards/{}/comments/{} - content length: {}",
                workspaceId, boardId, cardId, commentId, request.getContent().length());

        CommentResponse response = commentService.updateComment(boardId, cardId, commentId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 삭제 (Soft Delete)
     *
     * Spec § 5.4: DELETE /comments/{commentId}
     * Spec § FR-06d: 댓글 삭제 (작성자 또는 보드 OWNER만 가능)
     * Spec § FR-06h: Activity 로그 기록
     * Spec § FR-06n: Soft Delete 방식 사용
     *
     * @param workspaceId 워크스페이스 ID (경로 변수)
     * @param boardId 보드 ID
     * @param cardId 카드 ID
     * @param commentId 댓글 ID
     * @return 삭제 성공 응답 (204 No Content)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long workspaceId,
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @PathVariable Long commentId
    ) {
        log.info("DELETE /api/v1/workspaces/{}/boards/{}/cards/{}/comments/{}",
                workspaceId, boardId, cardId, commentId);

        commentService.deleteComment(boardId, cardId, commentId);

        return ResponseEntity.noContent().build();
    }
}
