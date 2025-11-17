package com.kanban.comment;

import com.kanban.card.Card;
import com.kanban.entity.BaseEntity;
import com.kanban.user.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 댓글(Comment) 엔티티
 * 카드에 작성되는 사용자 댓글
 *
 * Spec § 6. 백엔드 규격 - 데이터베이스 스키마
 * - Soft delete 방식 사용 (isDeleted 플래그)
 * - 카드 삭제 시에도 댓글 유지 (CASCADE 제거)
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    /**
     * 댓글 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연결된 카드
     * Spec § 6: CASCADE 제거, 카드 삭제 시에도 댓글 유지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    /**
     * 댓글 작성자
     * Spec § 6: CASCADE 제거
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * 댓글 내용 (HTML 포맷)
     * Spec § 6: RichTextEditor에서 생성된 HTML 저장
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Soft delete 플래그
     * Spec § FR-06n: Soft Delete 방식 사용
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // createdAt, updatedAt은 BaseEntity에서 상속
}
