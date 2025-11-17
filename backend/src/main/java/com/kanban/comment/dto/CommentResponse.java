package com.kanban.comment.dto;

import com.kanban.comment.Comment;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 댓글 응답 DTO
 *
 * Spec § 6. 백엔드 규격 - DTO 클래스
 * Spec § FR-06e: 작성자 정보 표시 (이름, 이메일, 아바타, 작성 시간)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    /**
     * 댓글 ID
     */
    private Long id;

    /**
     * 카드 ID
     */
    private Long cardId;

    /**
     * 작성자 ID
     */
    private Long authorId;

    /**
     * 작성자 이름
     */
    private String authorName;

    /**
     * 작성자 이메일
     */
    private String authorEmail;

    /**
     * 작성자 아바타 URL
     */
    private String authorAvatarUrl;

    /**
     * 댓글 내용 (HTML 포맷)
     */
    private String content;

    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     * Spec § FR-06m: 수정 이력 표시용
     */
    private LocalDateTime updatedAt;

    /**
     * Comment 엔티티를 CommentResponse로 변환
     *
     * @param comment Comment 엔티티
     * @return CommentResponse
     */
    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .cardId(comment.getCard().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getName())
                .authorEmail(comment.getAuthor().getEmail())
                .authorAvatarUrl(comment.getAuthor().getAvatarUrl())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
