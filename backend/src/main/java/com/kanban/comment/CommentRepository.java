package com.kanban.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 댓글 Repository
 *
 * Spec § 6. 백엔드 규격 - Repository
 * - 삭제되지 않은 댓글만 조회
 * - 최신순 정렬
 * - 페이지네이션 지원
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 카드의 삭제되지 않은 댓글 목록 조회 (페이지네이션, 최신순)
     *
     * Spec § 6: 인덱스 idx_card_id_not_deleted 활용
     * @param cardId 카드 ID
     * @param pageable 페이지네이션 정보
     * @return 댓글 페이지
     */
    Page<Comment> findByCardIdAndIsDeletedFalseOrderByCreatedAtDesc(Long cardId, Pageable pageable);

    /**
     * 특정 댓글 조회 (카드 ID와 댓글 ID로, 삭제되지 않은 것만)
     *
     * Spec § 6: 수정/삭제 전 댓글 검증용
     * @param commentId 댓글 ID
     * @param cardId 카드 ID
     * @return 댓글 Optional
     */
    Optional<Comment> findByIdAndCardIdAndIsDeletedFalse(Long commentId, Long cardId);

    /**
     * 카드의 삭제되지 않은 댓글 개수 조회
     *
     * Spec § FR-06l: 카드 목록에 댓글 개수 뱃지 표시용
     * @param cardId 카드 ID
     * @return 댓글 개수
     */
    long countByCardIdAndIsDeletedFalse(Long cardId);
}
