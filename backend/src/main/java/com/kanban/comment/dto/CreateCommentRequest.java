package com.kanban.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 댓글 생성 요청 DTO
 *
 * Spec § 6. 백엔드 규격 - DTO 클래스
 * Spec § FR-06g: 빈 댓글 방지
 * Spec § NFR-06c: 최대 10,000자 제한
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCommentRequest {

    /**
     * 댓글 내용 (HTML 포맷)
     */
    @NotBlank(message = "댓글 내용을 입력해주세요")
    @Size(max = 10000, message = "댓글은 10,000자를 초과할 수 없습니다")
    private String content;
}
