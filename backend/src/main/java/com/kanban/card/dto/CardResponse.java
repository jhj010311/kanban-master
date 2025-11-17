package com.kanban.card.dto;

import com.kanban.card.Card;
import com.kanban.label.dto.LabelResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 카드 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private Long id;
    private Long columnId;
    private String title;
    private String description;
    private Integer position;
    private String bgColor;
    private String priority;
    private String assignee;
    private String assigneeAvatarUrl;
    private LocalDate dueDate;
    private Boolean isCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LabelResponse> labels;

    // Spec § 6. 백엔드 규격 - DTO 확장
    // FR-06b, FR-06d: 부모/자식 카드 정보

    /**
     * 부모 카드 ID
     */
    private Long parentCardId;

    /**
     * 부모 카드 요약 정보
     */
    private ParentCardSummaryDTO parentCard;

    /**
     * 자식 카드 목록
     */
    private List<ChildCardSummaryDTO> childCards;

    /**
     * Card 엔티티를 CardResponse로 변환
     */
    public static CardResponse from(Card card) {
        return from(card, Collections.emptyList());
    }

    public static CardResponse from(Card card, List<LabelResponse> labels) {
        return CardResponse.builder()
                .id(card.getId())
                .columnId(card.getColumn().getId())
                .title(card.getTitle())
                .description(card.getDescription())
                .position(card.getPosition())
                .bgColor(card.getBgColor())
                .priority(card.getPriority())
                .assignee(card.getAssignee())
                .dueDate(card.getDueDate())
                .isCompleted(card.getIsCompleted())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .labels(labels)
                .parentCardId(card.getParentCard() != null ? card.getParentCard().getId() : null)
                .build();
    }

    /**
     * Card 엔티티를 CardResponse로 변환 (계층 정보 포함)
     * Spec § 6. 백엔드 규격 - FR-06b, FR-06d: 부모/자식 카드 정보 조회
     */
    public static CardResponse from(Card card, List<LabelResponse> labels,
                                    ParentCardSummaryDTO parentCard,
                                    List<ChildCardSummaryDTO> childCards) {
        return CardResponse.builder()
                .id(card.getId())
                .columnId(card.getColumn().getId())
                .title(card.getTitle())
                .description(card.getDescription())
                .position(card.getPosition())
                .bgColor(card.getBgColor())
                .priority(card.getPriority())
                .assignee(card.getAssignee())
                .dueDate(card.getDueDate())
                .isCompleted(card.getIsCompleted())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .labels(labels)
                .parentCardId(card.getParentCard() != null ? card.getParentCard().getId() : null)
                .parentCard(parentCard)
                .childCards(childCards)
                .build();
    }
}
