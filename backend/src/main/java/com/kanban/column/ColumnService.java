package com.kanban.column;

import com.kanban.activity.ActivityEventType;
import com.kanban.activity.ActivityService;
import com.kanban.activity.ActivityScopeType;
import com.kanban.board.Board;
import com.kanban.board.BoardRepository;
import com.kanban.board.member.BoardMemberRole;
import com.kanban.board.member.BoardMemberRoleValidator;
import com.kanban.column.dto.ColumnResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 칼럼(BoardColumn) 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ColumnService {

    private final ColumnRepository columnRepository;
    private final BoardRepository boardRepository;
    private final ActivityService activityService;
    private final BoardMemberRoleValidator roleValidator;

    /**
     * 특정 보드의 모든 칼럼 조회
     */
    @Transactional(readOnly = true)
    public List<ColumnResponse> getColumnsByBoard(Long boardId) {
        return columnRepository.findByBoardIdOrderByPosition(boardId).stream()
                .map(ColumnResponse::from)
                .toList();
    }

    /**
     * 칼럼 ID로 칼럼 조회
     */
    @Transactional(readOnly = true)
    public ColumnResponse getColumn(Long columnId) {
        BoardColumn column = columnRepository.findById(columnId)
            .orElseThrow(() -> new NoSuchElementException("칼럼을 찾을 수 없습니다: " + columnId));
        return ColumnResponse.from(column);
    }

    /**
     * 칼럼 생성 (권한 검증 포함)
     */
    public ColumnResponse createColumnWithValidation(Long boardId, String name, String description, String bgColor, Long userId) {
        // EDITOR 이상 권한 필요
        roleValidator.validateRole(boardId, BoardMemberRole.EDITOR);

        return createColumn(boardId, name, description, bgColor, userId);
    }

    /**
     * 칼럼 생성 (권한 검증 없음 - 내부 사용)
     */
    public ColumnResponse createColumn(Long boardId, String name, String description, String bgColor, Long userId) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new NoSuchElementException("보드를 찾을 수 없습니다: " + boardId));

        // 현재 칼럼 개수를 position으로 설정 (마지막에 추가)
        int position = columnRepository.countByBoardId(boardId);

        BoardColumn column = BoardColumn.builder()
            .board(board)
            .name(name)
            .description(description)
            .bgColor(bgColor)
            .position(position)
            .build();

        BoardColumn savedColumn = columnRepository.save(column);

        // 활동 기록
        activityService.recordActivity(
            ActivityScopeType.BOARD,
            boardId,
            ActivityEventType.COLUMN_CREATED,
            userId,
            "\"" + name + "\" 칼럼이 생성되었습니다"
        );

        return ColumnResponse.from(savedColumn);
    }

    /**
     * 칼럼 업데이트 (권한 검증 포함)
     */
    public ColumnResponse updateColumnWithValidation(Long boardId, Long columnId, String name, String description, String bgColor) {
        // EDITOR 이상 권한 필요
        roleValidator.validateRole(boardId, BoardMemberRole.EDITOR);

        return updateColumn(columnId, name, description, bgColor);
    }

    /**
     * 칼럼 업데이트 (권한 검증 없음 - 내부 사용)
     */
    public ColumnResponse updateColumn(Long columnId, String name, String description, String bgColor) {
        BoardColumn column = columnRepository.findById(columnId)
            .orElseThrow(() -> new NoSuchElementException("칼럼을 찾을 수 없습니다: " + columnId));

        if (name != null && !name.isBlank()) {
            column.setName(name);
        }
        if (description != null) {
            column.setDescription(description);
        }
        if (bgColor != null) {
            column.setBgColor(bgColor);
        }

        BoardColumn savedColumn = columnRepository.save(column);
        return ColumnResponse.from(savedColumn);
    }

    /**
     * 칼럼 위치 업데이트 (권한 검증 포함)
     */
    public ColumnResponse updateColumnPositionWithValidation(Long boardId, Long columnId, Integer newPosition, Long userId) {
        // EDITOR 이상 권한 필요
        roleValidator.validateRole(boardId, BoardMemberRole.EDITOR);

        return updateColumnPosition(boardId, columnId, newPosition, userId);
    }

    /**
     * 칼럼 위치 업데이트 (드래그 앤 드롭, 권한 검증 없음 - 내부 사용)
     */
    public ColumnResponse updateColumnPosition(Long boardId, Long columnId, Integer newPosition, Long userId) {
        BoardColumn column = columnRepository.findByIdAndBoardId(columnId, boardId)
            .orElseThrow(() -> new NoSuchElementException("칼럼을 찾을 수 없습니다: " + columnId));

        int currentPosition = column.getPosition();

        if (currentPosition < newPosition) {
            // 뒤로 이동: 현재 위치보다 뒤의 칼럼들을 앞으로 한 칸씩
            columnRepository.updatePositionsFrom(boardId, currentPosition + 1, -1);
        } else if (currentPosition > newPosition) {
            // 앞으로 이동: 새 위치 이상의 칼럼들을 뒤로 한 칸씩
            columnRepository.updatePositionsFrom(boardId, newPosition, 1);
        }

        column.setPosition(newPosition);
        BoardColumn updatedColumn = columnRepository.save(column);

        // 활동 기록
        if (currentPosition != newPosition) {
            activityService.recordActivity(
                ActivityScopeType.BOARD,
                boardId,
                ActivityEventType.COLUMN_REORDERED,
                userId,
                "\"" + column.getName() + "\" 칼럼이 이동되었습니다"
            );
        }

        return ColumnResponse.from(updatedColumn);
    }

    /**
     * 칼럼 삭제 (권한 검증 포함)
     */
    public void deleteColumnWithValidation(Long boardId, Long columnId, Long userId) {
        // EDITOR 이상 권한 필요
        roleValidator.validateRole(boardId, BoardMemberRole.EDITOR);

        deleteColumn(boardId, columnId, userId);
    }

    /**
     * 칼럼 삭제 (권한 검증 없음 - 내부 사용)
     */
    public void deleteColumn(Long boardId, Long columnId, Long userId) {
        BoardColumn column = columnRepository.findByIdAndBoardId(columnId, boardId)
            .orElseThrow(() -> new NoSuchElementException("칼럼을 찾을 수 없습니다: " + columnId));

        String columnName = column.getName();
        int deletedPosition = column.getPosition();

        // 칼럼 삭제
        columnRepository.delete(column);

        // 삭제된 칼럼 뒤의 모든 칼럼의 position을 한 칸씩 앞으로 이동
        columnRepository.updatePositionsFrom(boardId, deletedPosition, -1);

        // 활동 기록
        activityService.recordActivity(
            ActivityScopeType.BOARD,
            boardId,
            ActivityEventType.COLUMN_DELETED,
            userId,
            "\"" + columnName + "\" 칼럼이 삭제되었습니다"
        );
    }
}
