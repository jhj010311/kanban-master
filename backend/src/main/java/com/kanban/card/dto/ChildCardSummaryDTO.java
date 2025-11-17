package com.kanban.card.dto;

import com.kanban.card.Card;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 자식 카드 요약 정보 DTO
 * Spec § 6. 백엔드 규격 - DTO 확장
 * FR-06d: 자식 카드 목록 표시
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildCardSummaryDTO {

    /**
     * 자식 카드 ID
     */
    private Long id;

    /**
     * 자식 카드 제목
     */
    private String title;

    /**
     * 자식 카드 배경색
     */
    private String bgColor;

    /**
     * 자식 카드 우선순위
     */
    private String priority;

    /**
     * 자식 카드 완료 여부
     */
    private Boolean isCompleted;

    /**
     * Card 엔티티를 ChildCardSummaryDTO로 변환
     */
    public static ChildCardSummaryDTO from(Card card) {
        if (card == null) {
            return null;
        }

        return ChildCardSummaryDTO.builder()
                .id(card.getId())
                .title(card.getTitle())
                .bgColor(card.getBgColor())
                .priority(card.getPriority())
                .isCompleted(card.getIsCompleted())
                .build();
    }
}
