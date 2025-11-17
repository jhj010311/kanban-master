package com.kanban.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 카드 검색 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSearchRequest {

    /**
     * 검색 키워드 (제목, 설명에서 검색)
     */
    private String keyword;

    /**
     * 우선순위 필터
     */
    private List<String> priorities;

    /**
     * 담당자 필터
     */
    private List<String> assignees;

    /**
     * 라벨 ID 필터
     */
    private List<Long> labelIds;

    /**
     * 완료 상태 필터
     */
    private Boolean isCompleted;

    /**
     * 마감일 시작 범위
     */
    private LocalDate dueDateFrom;

    /**
     * 마감일 종료 범위
     */
    private LocalDate dueDateTo;

    /**
     * 지연된 카드만 조회
     */
    private Boolean overdue;

    /**
     * 부모 카드가 없는 카드만 조회 (부모 카드 선택 시 사용)
     */
    private Boolean parentCardIdIsNull;
}
