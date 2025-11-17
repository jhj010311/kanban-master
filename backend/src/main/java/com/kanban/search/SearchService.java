package com.kanban.search;

import com.kanban.card.Card;
import com.kanban.label.CardLabelRepository;
import com.kanban.label.dto.LabelResponse;
import com.kanban.search.dto.CardSearchRequest;
import com.kanban.search.dto.CardSearchResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 검색 서비스
 * 카드 검색 및 필터링 기능 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchService {

    private final EntityManager entityManager;
    private final CardLabelRepository cardLabelRepository;

    /**
     * 보드 내 카드 검색
     *
     * @param boardId 보드 ID
     * @param request 검색 조건
     * @return 검색 결과 목록
     */
    public List<CardSearchResponse> searchCards(Long boardId, CardSearchRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Card> query = cb.createQuery(Card.class);
        Root<Card> card = query.from(Card.class);

        // Join with column and board
        Join<Object, Object> column = card.join("column");
        Join<Object, Object> board = column.join("board");

        List<Predicate> predicates = new ArrayList<>();

        // 보드 ID 필터 (필수)
        predicates.add(cb.equal(board.get("id"), boardId));

        // 키워드 검색 (제목 또는 설명)
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = "%" + request.getKeyword().trim().toLowerCase() + "%";
            Predicate titleMatch = cb.like(cb.lower(card.get("title")), keyword);
            Predicate descMatch = cb.like(cb.lower(card.get("description")), keyword);
            predicates.add(cb.or(titleMatch, descMatch));
        }

        // 우선순위 필터
        if (request.getPriorities() != null && !request.getPriorities().isEmpty()) {
            predicates.add(card.get("priority").in(request.getPriorities()));
        }

        // 담당자 필터
        if (request.getAssignees() != null && !request.getAssignees().isEmpty()) {
            predicates.add(card.get("assignee").in(request.getAssignees()));
        }

        // 완료 상태 필터
        if (request.getIsCompleted() != null) {
            predicates.add(cb.equal(card.get("isCompleted"), request.getIsCompleted()));
        }

        // 마감일 범위 필터
        if (request.getDueDateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(card.get("dueDate"), request.getDueDateFrom()));
        }
        if (request.getDueDateTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(card.get("dueDate"), request.getDueDateTo()));
        }

        // 지연된 카드 필터 (마감일이 오늘보다 이전 + 미완료)
        if (request.getOverdue() != null && request.getOverdue()) {
            predicates.add(cb.lessThan(card.get("dueDate"), LocalDate.now()));
            predicates.add(cb.equal(card.get("isCompleted"), false));
        }

        // 부모 카드가 없는 카드만 필터 (부모 카드 선택용)
        if (request.getParentCardIdIsNull() != null && request.getParentCardIdIsNull()) {
            predicates.add(cb.isNull(card.get("parentCard")));
        }

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(card.get("updatedAt")));

        List<Card> cards = entityManager.createQuery(query).getResultList();

        // 라벨 필터 적용 (라벨은 별도 테이블이므로 후처리)
        if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
            cards = cards.stream()
                    .filter(c -> hasAnyLabel(c.getId(), request.getLabelIds()))
                    .toList();
        }

        return cards.stream()
                .map(this::toSearchResponse)
                .toList();
    }

    /**
     * 워크스페이스 내 카드 검색
     *
     * @param workspaceId 워크스페이스 ID
     * @param request     검색 조건
     * @return 검색 결과 목록
     */
    public List<CardSearchResponse> searchCardsInWorkspace(Long workspaceId, CardSearchRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Card> query = cb.createQuery(Card.class);
        Root<Card> card = query.from(Card.class);

        Join<Object, Object> column = card.join("column");
        Join<Object, Object> board = column.join("board");
        Join<Object, Object> workspace = board.join("workspace");

        List<Predicate> predicates = new ArrayList<>();

        // 워크스페이스 ID 필터 (필수)
        predicates.add(cb.equal(workspace.get("id"), workspaceId));

        // 키워드 검색
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = "%" + request.getKeyword().trim().toLowerCase() + "%";
            Predicate titleMatch = cb.like(cb.lower(card.get("title")), keyword);
            Predicate descMatch = cb.like(cb.lower(card.get("description")), keyword);
            predicates.add(cb.or(titleMatch, descMatch));
        }

        // 우선순위 필터
        if (request.getPriorities() != null && !request.getPriorities().isEmpty()) {
            predicates.add(card.get("priority").in(request.getPriorities()));
        }

        // 담당자 필터
        if (request.getAssignees() != null && !request.getAssignees().isEmpty()) {
            predicates.add(card.get("assignee").in(request.getAssignees()));
        }

        // 완료 상태 필터
        if (request.getIsCompleted() != null) {
            predicates.add(cb.equal(card.get("isCompleted"), request.getIsCompleted()));
        }

        // 마감일 범위 필터
        if (request.getDueDateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(card.get("dueDate"), request.getDueDateFrom()));
        }
        if (request.getDueDateTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(card.get("dueDate"), request.getDueDateTo()));
        }

        // 지연된 카드 필터
        if (request.getOverdue() != null && request.getOverdue()) {
            predicates.add(cb.lessThan(card.get("dueDate"), LocalDate.now()));
            predicates.add(cb.equal(card.get("isCompleted"), false));
        }

        // 부모 카드가 없는 카드만 필터 (부모 카드 선택용)
        if (request.getParentCardIdIsNull() != null && request.getParentCardIdIsNull()) {
            predicates.add(cb.isNull(card.get("parentCard")));
        }

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(card.get("updatedAt")));

        List<Card> cards = entityManager.createQuery(query).getResultList();

        // 라벨 필터
        if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
            cards = cards.stream()
                    .filter(c -> hasAnyLabel(c.getId(), request.getLabelIds()))
                    .toList();
        }

        return cards.stream()
                .map(this::toSearchResponse)
                .toList();
    }

    /**
     * 카드가 특정 라벨 중 하나라도 가지고 있는지 확인
     */
    private boolean hasAnyLabel(Long cardId, List<Long> labelIds) {
        return cardLabelRepository.existsByCardIdAndLabelIds(cardId, labelIds);
    }

    /**
     * Card 엔티티를 CardSearchResponse로 변환
     */
    private CardSearchResponse toSearchResponse(Card card) {
        List<LabelResponse> labels = cardLabelRepository.findByCardId(card.getId())
                .stream()
                .map(cl -> LabelResponse.from(cl.getLabel()))
                .toList();

        return CardSearchResponse.builder()
                .id(card.getId())
                .columnId(card.getColumn().getId())
                .columnName(card.getColumn().getName())
                .boardId(card.getColumn().getBoard().getId())
                .boardName(card.getColumn().getBoard().getName())
                .title(card.getTitle())
                .description(card.getDescription())
                .position(card.getPosition())
                .bgColor(card.getBgColor())
                .priority(card.getPriority())
                .assignee(card.getAssignee())
                .dueDate(card.getDueDate())
                .isCompleted(card.getIsCompleted())
                .labels(labels)
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}
