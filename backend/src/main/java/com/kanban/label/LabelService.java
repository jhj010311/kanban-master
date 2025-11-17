package com.kanban.label;

import com.kanban.board.Board;
import com.kanban.board.BoardRepository;
import com.kanban.label.dto.LabelReorderRequest;
import com.kanban.label.dto.LabelRequest;
import com.kanban.label.dto.LabelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 라벨 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LabelService {

    private final LabelRepository labelRepository;
    private final CardLabelRepository cardLabelRepository;
    private final BoardRepository boardRepository;

    /**
     * 보드의 모든 라벨 조회
     */
    @Transactional(readOnly = true)
    public List<LabelResponse> getLabelsByBoardId(Long boardId) {
        log.debug("Fetching labels for board: {}", boardId);

        // 보드 존재 확인
        if (!boardRepository.existsById(boardId)) {
            throw new IllegalArgumentException("보드를 찾을 수 없습니다");
        }

        List<Label> labels = labelRepository.findByBoardIdOrderByDisplayOrder(boardId);
        return labels.stream()
                .map(LabelResponse::from)
                .toList();
    }

    /**
     * 라벨 생성
     */
    public LabelResponse createLabel(Long boardId, LabelRequest request) {
        log.debug("Creating label for board {}: {}", boardId, request.getName());

        // 보드 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("보드를 찾을 수 없습니다"));

        // 중복 이름 확인
        if (labelRepository.existsByBoardIdAndName(boardId, request.getName())) {
            throw new IllegalArgumentException("동일한 이름의 라벨이 이미 존재합니다");
        }

        // 마지막 순서 조회
        Integer maxOrder = labelRepository.findMaxDisplayOrderByBoardId(boardId);
        Integer newOrder = (maxOrder == null || maxOrder < 0) ? 0 : maxOrder + 1;

        // 라벨 생성
        Label label = Label.builder()
                .board(board)
                .name(request.getName())
                .colorToken(request.getColorToken())
                .description(request.getDescription())
                .displayOrder(newOrder)
                .build();

        Label savedLabel = labelRepository.save(label);
        log.info("Label created: id={}, name={}, boardId={}", savedLabel.getId(), savedLabel.getName(), boardId);

        return LabelResponse.from(savedLabel);
    }

    /**
     * 라벨 수정
     */
    public LabelResponse updateLabel(Long boardId, Long labelId, LabelRequest request) {
        log.debug("Updating label {}: {}", labelId, request.getName());

        // 라벨 조회
        Label label = labelRepository.findByIdAndBoardId(labelId, boardId)
                .orElseThrow(() -> new IllegalArgumentException("라벨을 찾을 수 없습니다"));

        // 이름 중복 확인 (자기 자신 제외)
        if (!label.getName().equals(request.getName()) &&
            labelRepository.existsByBoardIdAndNameExcludingId(boardId, request.getName(), labelId)) {
            throw new IllegalArgumentException("동일한 이름의 라벨이 이미 존재합니다");
        }

        // 라벨 정보 업데이트
        label.updateInfo(request.getName(), request.getColorToken(), request.getDescription());

        Label updatedLabel = labelRepository.save(label);
        log.info("Label updated: id={}, name={}", updatedLabel.getId(), updatedLabel.getName());

        return LabelResponse.from(updatedLabel);
    }

    /**
     * 라벨 삭제
     */
    public void deleteLabel(Long boardId, Long labelId) {
        log.debug("Deleting label: {}", labelId);

        // 라벨 조회
        Label label = labelRepository.findByIdAndBoardId(labelId, boardId)
                .orElseThrow(() -> new IllegalArgumentException("라벨을 찾을 수 없습니다"));

        // 연결된 카드-라벨 관계 삭제
        cardLabelRepository.deleteByLabelId(labelId);

        // 라벨 삭제
        labelRepository.delete(label);
        log.info("Label deleted: id={}, name={}", labelId, label.getName());
    }

    /**
     * 라벨 순서 변경
     */
    public List<LabelResponse> reorderLabels(Long boardId, LabelReorderRequest request) {
        log.debug("Reordering labels for board: {}", boardId);

        // 보드 존재 확인
        if (!boardRepository.existsById(boardId)) {
            throw new IllegalArgumentException("보드를 찾을 수 없습니다");
        }

        List<Long> labelIds = request.getLabelIds();

        // 순서 업데이트
        for (int i = 0; i < labelIds.size(); i++) {
            Long labelId = labelIds.get(i);
            Label label = labelRepository.findByIdAndBoardId(labelId, boardId)
                    .orElseThrow(() -> new IllegalArgumentException("라벨을 찾을 수 없습니다: " + labelId));

            label.updateOrder(i);
            labelRepository.save(label);
        }

        log.info("Labels reordered for board {}: {} labels", boardId, labelIds.size());

        // 업데이트된 라벨 목록 반환
        return getLabelsByBoardId(boardId);
    }

    /**
     * 라벨 단건 조회
     */
    @Transactional(readOnly = true)
    public LabelResponse getLabel(Long boardId, Long labelId) {
        log.debug("Fetching label: boardId={}, labelId={}", boardId, labelId);

        Label label = labelRepository.findByIdAndBoardId(labelId, boardId)
                .orElseThrow(() -> new IllegalArgumentException("라벨을 찾을 수 없습니다"));

        return LabelResponse.from(label);
    }
}
