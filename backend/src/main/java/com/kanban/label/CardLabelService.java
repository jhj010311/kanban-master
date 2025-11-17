package com.kanban.label;

import com.kanban.card.Card;
import com.kanban.card.CardRepository;
import com.kanban.label.dto.CardLabelRequest;
import com.kanban.label.dto.LabelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카드-라벨 연결 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CardLabelService {

    private final CardLabelRepository cardLabelRepository;
    private final CardRepository cardRepository;
    private final LabelRepository labelRepository;

    /**
     * 카드에 할당된 라벨 목록 조회
     */
    @Transactional(readOnly = true)
    public List<LabelResponse> getCardLabels(Long cardId) {
        log.debug("Fetching labels for card: {}", cardId);

        List<CardLabel> cardLabels = cardLabelRepository.findByCardId(cardId);
        return cardLabels.stream()
                .map(cl -> LabelResponse.from(cl.getLabel()))
                .toList();
    }

    /**
     * 카드에 라벨 할당 (기존 라벨 모두 제거 후 새로 할당)
     */
    public List<LabelResponse> assignLabelsToCard(Long cardId, CardLabelRequest request) {
        log.debug("Assigning labels to card {}: {}", cardId, request.getLabelIds());

        // 카드 조회
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("카드를 찾을 수 없습니다"));

        // 기존 라벨 모두 제거
        cardLabelRepository.deleteByCardId(cardId);

        // 새 라벨 할당
        for (Long labelId : request.getLabelIds()) {
            Label label = labelRepository.findById(labelId)
                    .orElseThrow(() -> new IllegalArgumentException("라벨을 찾을 수 없습니다: " + labelId));

            CardLabel cardLabel = CardLabel.of(card, label);
            cardLabelRepository.save(cardLabel);
        }

        log.info("Labels assigned to card {}: {} labels", cardId, request.getLabelIds().size());

        // 업데이트된 라벨 목록 반환
        return getCardLabels(cardId);
    }

    /**
     * 카드에 라벨 추가
     */
    public LabelResponse addLabelToCard(Long cardId, Long labelId) {
        log.debug("Adding label {} to card {}", labelId, cardId);

        // 카드 조회
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("카드를 찾을 수 없습니다"));

        // 라벨 조회
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new IllegalArgumentException("라벨을 찾을 수 없습니다"));

        // 이미 할당되어 있는지 확인
        if (cardLabelRepository.existsByCardIdAndLabelId(cardId, labelId)) {
            throw new IllegalArgumentException("이미 할당된 라벨입니다");
        }

        // 카드-라벨 연결 생성
        CardLabel cardLabel = CardLabel.of(card, label);
        cardLabelRepository.save(cardLabel);

        log.info("Label {} added to card {}", labelId, cardId);

        return LabelResponse.from(label);
    }

    /**
     * 카드에서 라벨 제거
     */
    public void removeLabelFromCard(Long cardId, Long labelId) {
        log.debug("Removing label {} from card {}", labelId, cardId);

        CardLabel cardLabel = cardLabelRepository.findByCardIdAndLabelId(cardId, labelId)
                .orElseThrow(() -> new IllegalArgumentException("카드-라벨 연결을 찾을 수 없습니다"));

        cardLabelRepository.delete(cardLabel);

        log.info("Label {} removed from card {}", labelId, cardId);
    }
}
