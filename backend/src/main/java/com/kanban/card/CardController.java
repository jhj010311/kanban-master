package com.kanban.card;

import com.kanban.card.dto.CardResponse;
import com.kanban.card.dto.CreateCardRequest;
import com.kanban.card.dto.UpdateCardRequest;
import com.kanban.common.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 카드 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/boards/{boardId}/columns/{columnId}/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * 칼럼의 모든 카드 조회
     */
    @GetMapping
    public ResponseEntity<List<CardResponse>> listCards(
            @PathVariable Long workspaceId,
            @PathVariable Long boardId,
            @PathVariable Long columnId) {
        List<CardResponse> cards = cardService.getCardsByColumn(columnId);
        return ResponseEntity.ok(cards);
    }

    /**
     * 특정 카드 조회
     * Spec § 6. 백엔드 규격 - API 엔드포인트
     * FR-06b, FR-06d: includeRelations=true 파라미터로 부모/자식 정보 포함 여부 제어
     */
    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCard(
            @PathVariable Long workspaceId,
            @PathVariable Long boardId,
            @PathVariable Long columnId,
            @PathVariable Long cardId,
            @RequestParam(required = false, defaultValue = "false") boolean includeRelations) {
        CardResponse card;
        if (includeRelations) {
            card = cardService.getCardWithHierarchy(columnId, cardId);
        } else {
            card = cardService.getCard(columnId, cardId);
        }
        return ResponseEntity.ok(card);
    }

    /**
     * 카드 생성
     */
    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @PathVariable Long workspaceId,
            @PathVariable Long boardId,
            @PathVariable Long columnId,
            @Valid @RequestBody CreateCardRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        CardResponse card = cardService.createCardWithValidation(boardId, columnId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    /**
     * 카드 수정
     */
    @PutMapping("/{cardId}")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable Long workspaceId,
            @PathVariable Long boardId,
            @PathVariable Long columnId,
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateCardRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        CardResponse card = cardService.updateCardWithValidation(boardId, columnId, cardId, request, userId);
        return ResponseEntity.ok(card);
    }

    /**
     * 카드 삭제
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable Long workspaceId,
            @PathVariable Long boardId,
            @PathVariable Long columnId,
            @PathVariable Long cardId) {
        Long userId = SecurityUtil.getCurrentUserId();
        cardService.deleteCardWithValidation(boardId, columnId, cardId, userId);
        return ResponseEntity.noContent().build();
    }
}
