package com.kanban.card;

import com.kanban.entity.BaseEntity;
import com.kanban.column.BoardColumn;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 카드(Card) 엔티티
 * 칼럼에 속하는 개별 카드 항목
 */
@Entity
@Table(name = "card")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card extends BaseEntity {

    /**
     * 카드 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 카드가 속한 칼럼
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id", nullable = false)
    private BoardColumn column;

    /**
     * 카드 제목
     */
    @Column(nullable = false)
    private String title;

    /**
     * 카드 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 카드 순서 (칼럼 내에서의 위치)
     */
    @Column(nullable = false)
    private Integer position;

    /**
     * 카드 색상 (HEX 코드)
     */
    private String bgColor;

    /**
     * 카드 우선순위 (HIGH, MEDIUM, LOW)
     */
    @Column(name = "priority", length = 20)
    private String priority;

    /**
     * 담당자 (추후 User 관계 추가 가능)
     */
    private String assignee;

    /**
     * 마감 날짜
     */
    private LocalDate dueDate;

    /**
     * 완료 여부
     */
    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    // Spec § 6. 백엔드 규격 - 데이터베이스 스키마 확장
    // FR-06a: 부모-자식 관계 데이터 모델

    /**
     * 부모 카드 (Self-Referential ManyToOne)
     * 1단계 계층 구조만 지원 (부모 → 자식, 손자 불가)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_card_id")
    private Card parentCard;

    /**
     * 자식 카드 목록 (OneToMany)
     * mappedBy로 양방향 관계 설정
     * Cascade 없음: 부모 삭제 시 자식 삭제 안 함 (서비스 레벨에서 차단)
     */
    @OneToMany(mappedBy = "parentCard")
    @Builder.Default
    private List<Card> childCards = new ArrayList<>();
}
