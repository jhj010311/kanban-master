package com.kanban.card;

import com.kanban.activity.ActivityEventType;
import com.kanban.activity.ActivityService;
import com.kanban.activity.ActivityScopeType;
import com.kanban.board.member.BoardMemberRole;
import com.kanban.board.member.BoardMemberRoleValidator;
import com.kanban.card.dto.CardResponse;
import com.kanban.card.dto.ChildCardSummaryDTO;
import com.kanban.card.dto.CreateCardRequest;
import com.kanban.card.dto.ParentCardSummaryDTO;
import com.kanban.card.dto.UpdateCardRequest;
import com.kanban.column.BoardColumn;
import com.kanban.column.ColumnRepository;
import com.kanban.exception.CardHasChildrenException;
import com.kanban.exception.ResourceNotFoundException;
import com.kanban.label.CardLabel;
import com.kanban.label.CardLabelRepository;
import com.kanban.label.dto.LabelResponse;
import com.kanban.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.owasp.html.PolicyFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카드 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CardService {

    private final CardRepository cardRepository;
    private final ColumnRepository columnRepository;
    private final ActivityService activityService;
    private final BoardMemberRoleValidator roleValidator;
    private final CardLabelRepository cardLabelRepository;
    private final UserRepository userRepository;
    private final PolicyFactory htmlSanitizerPolicy;

    /**
     * 특정 칼럼의 모든 카드 조회
     * Spec § 5. 기능 요구사항 - FR-06g: 자식 개수 표시
     */
    public List<CardResponse> getCardsByColumn(Long columnId) {
        List<Card> cards = cardRepository.findByColumnIdOrderByPosition(columnId);
        if (cards.isEmpty()) {
            return List.of();
        }

        Map<Long, List<LabelResponse>> labelsByCardId = getLabelsByCardIds(cards.stream()
                .map(Card::getId)
                .toList());

        // 자식 카드 개수 조회 (FR-06g)
        Map<Long, Integer> childCountByCardId = getChildCountByCardIds(cards.stream()
                .map(Card::getId)
                .toList());

        return cards.stream()
                .map(card -> {
                    CardResponse response = CardResponse.from(card, labelsByCardId.getOrDefault(card.getId(), List.of()));
                    // 자식 카드가 있으면 빈 리스트 설정 (개수만 필요)
                    int childCount = childCountByCardId.getOrDefault(card.getId(), 0);
                    if (childCount > 0) {
                        response.setChildCards(List.of());  // 프론트엔드에서 childCards != null로 자식 존재 여부 판단
                    }
                    return enrichWithAssigneeAvatar(response);
                })
                .toList();
    }

    /**
     * 특정 카드 조회
     */
    public CardResponse getCard(Long columnId, Long cardId) {
        Card card = cardRepository.findByIdAndColumnId(cardId, columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        List<LabelResponse> labels = cardLabelRepository.findByCardId(cardId)
                .stream()
                .map(cardLabel -> LabelResponse.from(cardLabel.getLabel()))
                .toList();

        return enrichWithAssigneeAvatar(CardResponse.from(card, labels));
    }

    /**
     * 특정 카드 조회 (계층 정보 포함)
     * Spec § 6. 백엔드 규격 - FR-06b, FR-06d: 부모/자식 카드 정보 조회
     * 결정 사항 1: 자식 카드는 생성일 오름차순 정렬
     * 결정 사항 4: 초기 20개 자식 카드만 로드, 더보기 버튼으로 추가 로드
     */
    public CardResponse getCardWithHierarchy(Long columnId, Long cardId) {
        // 카드 조회 (부모 정보 포함)
        Card card = cardRepository.findByIdWithParent(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        // 칼럼 검증
        if (!card.getColumn().getId().equals(columnId)) {
            throw new ResourceNotFoundException("Card not found");
        }

        // 라벨 조회
        List<LabelResponse> labels = cardLabelRepository.findByCardId(cardId)
                .stream()
                .map(cardLabel -> LabelResponse.from(cardLabel.getLabel()))
                .toList();

        // 부모 카드 정보 (있는 경우)
        ParentCardSummaryDTO parentCard = null;
        if (card.getParentCard() != null) {
            parentCard = ParentCardSummaryDTO.from(card.getParentCard());
        }

        // 자식 카드 목록 (최대 20개, 생성일 오름차순)
        List<Card> childCardEntities = cardRepository.findByParentCardIdOrderByCreatedAt(cardId);
        List<ChildCardSummaryDTO> childCards = childCardEntities.stream()
                .limit(20)  // 초기 20개만 로드
                .map(ChildCardSummaryDTO::from)
                .toList();

        return enrichWithAssigneeAvatar(
                CardResponse.from(card, labels, parentCard, childCards)
        );
    }

    /**
     * 카드 생성 (권한 검증 포함)
     */
    public CardResponse createCardWithValidation(Long boardId, Long columnId, CreateCardRequest request, Long userId) {
        // EDITOR 이상 권한 필요
        roleValidator.validateRole(boardId, BoardMemberRole.EDITOR);

        return createCard(columnId, request, userId);
    }

    /**
     * 카드 생성 (권한 검증 없음 - 내부 사용)
     * Spec § 5. 기능 요구사항 - FR-06j: 계층 제한 검증
     */
    public CardResponse createCard(Long columnId, CreateCardRequest request, Long userId) {
        BoardColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));

        // 부모 카드 검증 (FR-06j: 1단계 계층 구조만 지원)
        Card parentCard = null;
        if (request.getParentCardId() != null) {
            parentCard = cardRepository.findById(request.getParentCardId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent card not found"));

            // 부모 카드가 이미 자식 카드인 경우 (손자 카드 생성 방지)
            if (parentCard.getParentCard() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "1단계 계층 구조만 지원합니다. 손자 카드는 생성할 수 없습니다."
                );
            }

            // 부모 카드와 자식 카드가 같은 보드에 속하는지 검증
            if (!parentCard.getColumn().getBoard().getId().equals(column.getBoard().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "부모 카드와 자식 카드는 같은 보드에 속해야 합니다."
                );
            }
        }

        // 현재 칼럼의 카드 개수를 조회하여 position 설정
        int nextPosition = cardRepository.countByColumnId(columnId);

        Card card = Card.builder()
                .column(column)
                .title(request.getTitle())
                .description(sanitizeHtml(request.getDescription()))
                .position(nextPosition)
                .bgColor(request.getBgColor())
                .priority(request.getPriority())
                .assignee(request.getAssignee())
                .dueDate(request.getDueDate())
                .parentCard(parentCard)
                .build();

        Card savedCard = cardRepository.save(card);

        // 활동 기록
        String activityMessage = "\"" + savedCard.getTitle() + "\" 카드가 생성되었습니다";
        if (parentCard != null) {
            activityMessage += " (부모: \"" + parentCard.getTitle() + "\")";
        }
        activityService.recordActivity(
            ActivityScopeType.CARD,
            savedCard.getId(),
            ActivityEventType.CARD_CREATED,
            userId,
            activityMessage
        );

        return enrichWithAssigneeAvatar(CardResponse.from(savedCard));
    }

    /**
     * 카드 수정 (권한 검증 포함)
     */
    public CardResponse updateCardWithValidation(Long boardId, Long columnId, Long cardId, UpdateCardRequest request, Long userId) {
        // EDITOR 이상 권한 필요
        roleValidator.validateRole(boardId, BoardMemberRole.EDITOR);

        return updateCard(columnId, cardId, request, userId);
    }

    /**
     * 카드 수정 (활동 기록 포함, 권한 검증 없음 - 내부 사용)
     * Spec § 5. 기능 요구사항 - FR-06i: 컬럼 이동 시 부모 관계 해제
     */
    public CardResponse updateCard(Long columnId, Long cardId, UpdateCardRequest request, Long userId) {
        Card card = cardRepository.findByIdAndColumnId(cardId, columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        String originalTitle = card.getTitle();
        boolean isMoved = false;
        boolean parentRelationRemoved = false;

        if (request.getTitle() != null) {
            card.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            card.setDescription(sanitizeHtml(request.getDescription()));
        }
        if (request.getBgColor() != null) {
            card.setBgColor(request.getBgColor());
        }
        if (request.getPriority() != null) {
            card.setPriority(request.getPriority());
        }
        if (request.getAssignee() != null) {
            card.setAssignee(request.getAssignee());
        }
        if (request.getDueDate() != null) {
            card.setDueDate(request.getDueDate());
        }
        if (request.getIsCompleted() != null) {
            card.setIsCompleted(request.getIsCompleted());
        }

        // 다른 컬럼으로 이동하는 경우
        if (request.getColumnId() != null && !request.getColumnId().equals(columnId)) {
            BoardColumn newColumn = columnRepository.findById(request.getColumnId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target column not found"));

            // FR-06i: 자식 카드가 다른 컬럼으로 이동 시 부모 관계 해제
            if (card.getParentCard() != null) {
                card.setParentCard(null);
                parentRelationRemoved = true;
            }

            // 기존 컬럼에서 position 업데이트
            cardRepository.updatePositionsFrom(columnId, card.getPosition() + 1, -1);

            // 새 컬럼으로 이동
            card.setColumn(newColumn);
            isMoved = true;

            // 새 컬럼에서의 position 설정
            if (request.getPosition() != null) {
                // 새 컬럼에서 해당 position 이상의 카드들 뒤로 밀기
                cardRepository.updatePositionsFrom(request.getColumnId(), request.getPosition(), 1);
                card.setPosition(request.getPosition());
            } else {
                // position 지정 없으면 끝에 추가
                int nextPosition = cardRepository.countByColumnId(request.getColumnId());
                card.setPosition(nextPosition);
            }
        } else {
            // 같은 컬럼 내 위치 변경 처리
            if (request.getPosition() != null && !request.getPosition().equals(card.getPosition())) {
                handleCardPositionChange(columnId, card, request.getPosition());
            }
        }

        Card updatedCard = cardRepository.save(card);

        // 활동 기록
        if (isMoved) {
            String moveMessage = "\"" + originalTitle + "\" 카드가 이동되었습니다";
            if (parentRelationRemoved) {
                moveMessage += " (부모 관계 해제됨)";
            }
            activityService.recordActivity(
                ActivityScopeType.CARD,
                cardId,
                ActivityEventType.CARD_MOVED,
                userId,
                moveMessage
            );
        } else {
            activityService.recordActivity(
                ActivityScopeType.CARD,
                cardId,
                ActivityEventType.CARD_UPDATED,
                userId,
                "\"" + updatedCard.getTitle() + "\" 카드가 업데이트되었습니다"
            );
        }

        return enrichWithAssigneeAvatar(CardResponse.from(updatedCard));
    }

    /**
     * 카드 삭제 (권한 검증 포함)
     */
    public void deleteCardWithValidation(Long boardId, Long columnId, Long cardId, Long userId) {
        // EDITOR 이상 권한 필요
        roleValidator.validateRole(boardId, BoardMemberRole.EDITOR);

        deleteCard(columnId, cardId, userId);
    }

    /**
     * 카드 삭제 (권한 검증 없음 - 내부 사용)
     * Spec § 7. 보안 처리 - 데이터 무결성
     * FR-06h 변경: 부모 카드 삭제 차단
     * 결정 사항 2: 자식이 있으면 부모 삭제 차단
     */
    public void deleteCard(Long columnId, Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndColumnId(cardId, columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        // 자식 카드 존재 여부 확인 (FR-06h: 부모 카드 삭제 차단)
        int childCount = cardRepository.countByParentCardId(cardId);
        if (childCount > 0) {
            throw new CardHasChildrenException(childCount);
        }

        String cardTitle = card.getTitle();
        int deletedPosition = card.getPosition();

        // 카드 삭제 전에 관련된 모든 카드-라벨 연결을 먼저 삭제
        cardLabelRepository.deleteByCardId(cardId);

        // 카드 삭제
        cardRepository.delete(card);

        // 삭제된 카드 이후의 카드들의 position을 업데이트
        cardRepository.updatePositionsFrom(columnId, deletedPosition + 1, -1);

        // 활동 기록
        activityService.recordActivity(
            ActivityScopeType.CARD,
            cardId,
            ActivityEventType.CARD_DELETED,
            userId,
            "\"" + cardTitle + "\" 카드가 삭제되었습니다"
        );
    }

    /**
     * 카드 위치 변경 처리
     */
    private void handleCardPositionChange(Long columnId, Card card, Integer newPosition) {
        int oldPosition = card.getPosition();

        if (newPosition < oldPosition) {
            // 위로 이동: newPosition 이상 oldPosition 미만의 카드들을 아래로 한 칸 이동
            cardRepository.updatePositionsFrom(columnId, newPosition, 1);
            cardRepository.updatePositionsFrom(columnId, oldPosition + 1, -1);
        } else if (newPosition > oldPosition) {
            // 아래로 이동: oldPosition 초과 newPosition 이하의 카드들을 위로 한 칸 이동
            cardRepository.updatePositionsFrom(columnId, oldPosition + 1, -1);
            cardRepository.updatePositionsFrom(columnId, newPosition, 1);
        }

        card.setPosition(newPosition);
    }

    private Map<Long, List<LabelResponse>> getLabelsByCardIds(List<Long> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }

        List<CardLabel> cardLabels = cardLabelRepository.findByCardIdIn(cardIds);

        return cardLabels.stream()
                .collect(Collectors.groupingBy(
                        cardLabel -> cardLabel.getCard().getId(),
                        Collectors.mapping(cardLabel -> LabelResponse.from(cardLabel.getLabel()), Collectors.toList())
                ));
    }

    /**
     * 여러 카드의 자식 개수 조회
     * FR-06g: 자식 개수 표시를 위한 헬퍼 메서드
     */
    private Map<Long, Integer> getChildCountByCardIds(List<Long> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }

        return cardIds.stream()
                .collect(Collectors.toMap(
                        cardId -> cardId,
                        cardId -> cardRepository.countByParentCardId(cardId)
                ));
    }

    /**
     * CardResponse에 담당자의 아바타 URL 추가
     */
    private CardResponse enrichWithAssigneeAvatar(CardResponse cardResponse) {
        if (cardResponse.getAssignee() != null && !cardResponse.getAssignee().isEmpty()) {
            userRepository.findByName(cardResponse.getAssignee())
                    .ifPresent(user -> cardResponse.setAssigneeAvatarUrl(user.getAvatarUrl()));
        }
        return cardResponse;
    }

    /**
     * HTML Sanitization
     * XSS 공격 방지를 위해 위험한 HTML 태그와 속성을 제거
     */
    private String sanitizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        return htmlSanitizerPolicy.sanitize(html);
    }
}
