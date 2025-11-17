package com.kanban.template;

import com.kanban.board.Board;
import com.kanban.board.BoardRepository;
import com.kanban.board.dto.BoardResponse;
import com.kanban.column.BoardColumn;
import com.kanban.column.ColumnRepository;
import com.kanban.common.SecurityUtil;
import com.kanban.user.User;
import com.kanban.user.UserRepository;
import com.kanban.template.dto.ApplyTemplateRequest;
import com.kanban.template.dto.BoardTemplateResponse;
import com.kanban.template.dto.CreateTemplateRequest;
import com.kanban.template.dto.TemplateColumnDto;
import com.kanban.workspace.Workspace;
import com.kanban.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 템플릿 서비스
 * 보드 템플릿 생성, 조회, 적용 로직
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TemplateService {

    private final BoardTemplateRepository templateRepository;
    private final WorkspaceRepository workspaceRepository;
    private final BoardRepository boardRepository;
    private final ColumnRepository columnRepository;
    private final UserRepository userRepository;

    /**
     * 공개 템플릿 목록 조회
     */
    public List<BoardTemplateResponse> getPublicTemplates() {
        log.debug("Fetching public templates");
        return templateRepository.findPublicTemplates().stream()
                .map(BoardTemplateResponse::from)
                .toList();
    }

    /**
     * 워크스페이스의 템플릿 목록 조회 (공개 + private)
     */
    public List<BoardTemplateResponse> getTemplatesByWorkspace(Long workspaceId) {
        log.debug("Fetching templates for workspace: {}", workspaceId);

        // 공개 템플릿
        List<BoardTemplate> publicTemplates = templateRepository.findPublicTemplates();

        // 워크스페이스 private 템플릿
        List<BoardTemplate> privateTemplates = templateRepository.findByWorkspaceId(workspaceId);

        // 합치기
        publicTemplates.addAll(privateTemplates);

        return publicTemplates.stream()
                .map(BoardTemplateResponse::from)
                .toList();
    }

    /**
     * 카테고리별 공개 템플릿 조회
     */
    public List<BoardTemplateResponse> getPublicTemplatesByCategory(String category) {
        log.debug("Fetching public templates by category: {}", category);
        return templateRepository.findPublicTemplatesByCategory(category).stream()
                .map(BoardTemplateResponse::from)
                .toList();
    }

    /**
     * 템플릿 상세 조회
     */
    public BoardTemplateResponse getTemplate(Long templateId) {
        log.debug("Fetching template: {}", templateId);
        BoardTemplate template = templateRepository.findByIdWithColumns(templateId)
                .orElseThrow(() -> new IllegalArgumentException("템플릿을 찾을 수 없습니다: " + templateId));

        return BoardTemplateResponse.from(template);
    }

    /**
     * 기존 보드를 템플릿으로 저장
     */
    @Transactional
    public BoardTemplateResponse saveAsTemplate(Long workspaceId, Long boardId, CreateTemplateRequest request) {
        log.debug("Saving board {} as template in workspace: {}", boardId, workspaceId);

        // 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다: " + workspaceId));

        // 템플릿 생성
        BoardTemplate template = BoardTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .isPublic(request.getIsPublic())
                .workspace(request.getIsPublic() ? null : workspace)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 보드의 칼럼을 템플릿 칼럼으로 복사
        List<BoardColumn> columns = columnRepository.findByBoardIdOrderByPosition(boardId);
        for (BoardColumn column : columns) {
            TemplateColumn templateColumn = TemplateColumn.builder()
                    .name(column.getName())
                    .description(column.getDescription())
                    .position(column.getPosition())
                    .bgColor(column.getBgColor())
                    .build();
            template.addColumn(templateColumn);
        }

        BoardTemplate savedTemplate = templateRepository.save(template);
        log.info("Template created: {}", savedTemplate.getId());

        return BoardTemplateResponse.from(savedTemplate);
    }

    /**
     * 템플릿으로부터 새 보드 생성
     */
    @Transactional
    public BoardResponse applyTemplate(Long workspaceId, ApplyTemplateRequest request) {
        log.debug("Applying template {} to workspace: {}", request.getTemplateId(), workspaceId);

        // 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다: " + workspaceId));

        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 사용자 조회 - 실패 시 워크스페이스 소유자를 그대로 사용
        User owner = userRepository.findById(currentUserId)
                .orElseGet(() -> {
                    var workspaceOwner = workspace.getOwner();
                    if (workspaceOwner == null) {
                        throw new IllegalStateException("워크스페이스 소유자를 찾을 수 없습니다: " + workspaceId);
                    }
                    log.warn("현재 사용자({})를 찾을 수 없어 워크스페이스({}) 소유자({})를 보드 소유자로 사용합니다",
                            currentUserId, workspaceId, workspaceOwner.getId());
                    return workspaceOwner;
                });

        log.debug("Applying template {} by user {} (resolved owner id: {})",
                request.getTemplateId(), currentUserId, owner.getId());

        // 템플릿 조회
        BoardTemplate template = templateRepository.findByIdWithColumns(request.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("템플릿을 찾을 수 없습니다: " + request.getTemplateId()));

        // 새 보드 생성
        Board board = Board.builder()
                .name(request.getBoardName())
                .description(request.getBoardDescription())
                .owner(owner)
                .workspace(workspace)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Board savedBoard = boardRepository.save(board);

        // 템플릿의 칼럼을 새 보드에 복사
        for (TemplateColumn templateColumn : template.getColumns()) {
            BoardColumn column = BoardColumn.builder()
                    .name(templateColumn.getName())
                    .description(templateColumn.getDescription())
                    .position(templateColumn.getPosition())
                    .bgColor(templateColumn.getBgColor())
                    .board(savedBoard)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            columnRepository.save(column);
        }

        log.info("Board created from template: {}", savedBoard.getId());

        // 저장 직후 DTO 프로젝션으로 재조회하여 Lazy Proxy 문제 방지
        return boardRepository.findBoardResponseById(savedBoard.getId())
                .orElseThrow(() -> new IllegalStateException("생성된 보드를 조회할 수 없습니다: " + savedBoard.getId()));
    }

    /**
     * 템플릿 삭제
     */
    @Transactional
    public void deleteTemplate(Long templateId, Long workspaceId) {
        log.debug("Deleting template: {}", templateId);

        BoardTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("템플릿을 찾을 수 없습니다: " + templateId));

        // 권한 확인: 공개 템플릿은 삭제 불가 (실제로는 관리자 권한 필요)
        if (template.getIsPublic()) {
            throw new IllegalArgumentException("공개 템플릿은 삭제할 수 없습니다");
        }

        // 소유 워크스페이스 확인
        if (template.getWorkspace() == null || !template.getWorkspace().getId().equals(workspaceId)) {
            throw new IllegalArgumentException("템플릿 삭제 권한이 없습니다");
        }

        templateRepository.delete(template);
        log.info("Template deleted: {}", templateId);
    }

    /**
     * 커스텀 템플릿 생성 (보드 없이 직접 생성)
     */
    @Transactional
    public BoardTemplateResponse createCustomTemplate(Long workspaceId, CreateTemplateRequest request) {
        log.debug("Creating custom template in workspace: {}", workspaceId);

        Workspace workspace = null;
        if (!request.getIsPublic()) {
            workspace = workspaceRepository.findById(workspaceId)
                    .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다: " + workspaceId));
        }

        // 템플릿 생성
        BoardTemplate template = BoardTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .isPublic(request.getIsPublic())
                .workspace(workspace)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 칼럼 추가
        if (request.getColumns() != null) {
            for (TemplateColumnDto columnDto : request.getColumns()) {
                TemplateColumn column = columnDto.toEntity();
                template.addColumn(column);
            }
        }

        BoardTemplate savedTemplate = templateRepository.save(template);
        log.info("Custom template created: {}", savedTemplate.getId());

        return BoardTemplateResponse.from(savedTemplate);
    }

    /**
     * 모든 카테고리 목록 조회
     */
    public List<String> getAllCategories() {
        log.debug("Fetching all template categories");
        return templateRepository.findAllCategories();
    }
}
