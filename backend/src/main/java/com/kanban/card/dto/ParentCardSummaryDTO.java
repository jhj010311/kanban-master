package com.kanban.card.dto;

import com.kanban.card.Card;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 부모 카드 요약 정보 DTO
 * Spec § 6. 백엔드 규격 - DTO 확장
 * FR-06b: 부모 카드 정보 표시
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentCardSummaryDTO {

    /**
     * 부모 카드 ID
     */
    private Long id;

    /**
     * 부모 카드 제목
     */
    private String title;

    /**
     * 부모 카드 배경색
     */
    private String bgColor;

    /**
     * 부모 카드 우선순위
     */
    private String priority;

    /**
     * 부모 카드 담당자
     */
    private String assignee;

    /**
     * Card 엔티티를 ParentCardSummaryDTO로 변환
     */
    public static ParentCardSummaryDTO from(Card card) {
        if (card == null) {
            return null;
        }

        return ParentCardSummaryDTO.builder()
                .id(card.getId())
                .title(card.getTitle())
                .bgColor(card.getBgColor())
                .priority(card.getPriority())
                .assignee(card.getAssignee())
                .build();
    }
}
