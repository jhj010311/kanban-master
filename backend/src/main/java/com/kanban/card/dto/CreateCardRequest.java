package com.kanban.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 카드 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest {

    @NotBlank(message = "카드 제목은 필수입니다")
    private String title;

    @Size(max = 50000, message = "설명은 50,000자를 초과할 수 없습니다")
    private String description;

    private String bgColor;

    private String priority;

    private String assignee;

    private LocalDate dueDate;

    // Spec § 6. 백엔드 규격 - DTO 확장
    // FR-06f: 하위 카드 생성 버튼

    /**
     * 부모 카드 ID (하위 카드 생성 시 사용)
     */
    private Long parentCardId;
}
