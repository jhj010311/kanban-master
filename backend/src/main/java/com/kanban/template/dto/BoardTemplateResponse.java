package com.kanban.template.dto;

import com.kanban.template.BoardTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 보드 템플릿 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardTemplateResponse {

    private Long id;
    private String name;
    private String description;
    private String category;
    private Boolean isPublic;
    private Long workspaceId;
    private List<TemplateColumnDto> columns;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 엔터티를 DTO로 변환
     */
    public static BoardTemplateResponse from(BoardTemplate template) {
        return BoardTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .isPublic(template.getIsPublic())
                .workspaceId(template.getWorkspace() != null ? template.getWorkspace().getId() : null)
                .columns(template.getColumns().stream()
                        .map(TemplateColumnDto::from)
                        .toList())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
