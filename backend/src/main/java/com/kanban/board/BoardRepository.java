package com.kanban.board;

import com.kanban.board.dto.BoardResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    /**
     * 워크스페이스 내의 모든 활성 보드 조회 (정렬: 최근 업데이트 순)
     */
    @Query("SELECT b FROM Board b " +
           "WHERE b.workspace.id = :workspaceId AND b.status = 'ACTIVE' " +
           "ORDER BY b.updatedAt DESC")
    List<Board> findActiveByWorkspaceId(@Param("workspaceId") Long workspaceId);

    /**
     * 워크스페이스 내의 특정 보드 조회
     */
    Optional<Board> findByIdAndWorkspaceId(Long boardId, Long workspaceId);

    /**
     * 워크스페이스에서 특정 소유자의 보드 조회
     */
    @Query("SELECT b FROM Board b " +
           "WHERE b.workspace.id = :workspaceId AND b.owner.id = :ownerId AND b.status = 'ACTIVE' " +
           "ORDER BY b.updatedAt DESC")
    List<Board> findActiveByWorkspaceIdAndOwnerId(
        @Param("workspaceId") Long workspaceId,
        @Param("ownerId") Long ownerId
    );

    /**
     * 워크스페이스 내에서 최근 N개의 활성 보드 조회
     */
    @Query("SELECT b FROM Board b " +
           "WHERE b.workspace.id = :workspaceId AND b.status = 'ACTIVE' " +
           "ORDER BY b.updatedAt DESC " +
           "LIMIT :limit")
    List<Board> findRecentActiveBoards(
        @Param("workspaceId") Long workspaceId,
        @Param("limit") int limit
    );

    /**
     * 최근 N개의 활성 보드 조회 (모든 워크스페이스)
     */
    @Query("SELECT b FROM Board b " +
           "WHERE b.status = 'ACTIVE' " +
           "ORDER BY b.updatedAt DESC " +
           "LIMIT :limit")
    List<Board> findRecentActiveBoardsAllWorkspaces(@Param("limit") int limit);

    /**
     * 30일 이상 경과된 삭제된 보드 조회 (배치 삭제용)
     */
    @Query("SELECT b FROM Board b " +
           "WHERE b.status = 'DELETED' AND b.deletedAt < :cutoffDate")
    List<Board> findPermanentlyDeletableBoards(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 워크스페이스 내 모든 보드 개수 (활성만)
     */
    @Query("SELECT COUNT(b) FROM Board b " +
           "WHERE b.workspace.id = :workspaceId AND b.status = 'ACTIVE'")
    long countActiveByWorkspaceId(@Param("workspaceId") Long workspaceId);

    /**
     * 특정 이름의 보드가 워크스페이스에 존재하는지 확인
     */
    @Query("SELECT COUNT(b) > 0 FROM Board b " +
           "WHERE b.workspace.id = :workspaceId AND b.name = :name AND b.status = 'ACTIVE'")
    boolean existsByNameInWorkspace(
        @Param("workspaceId") Long workspaceId,
        @Param("name") String name
    );

    /**
     * DTO 형태로 보드 조회 (워크스페이스/소유자 정보 포함)
     * 14개 파라미터 생성자 사용 (Hibernate 6.4+ 호환)
     */
    @Query("SELECT new com.kanban.board.dto.BoardResponse(" +
           "b.id, b.workspace.id, b.owner.id, b.owner.name, b.name, b.description, " +
           "b.themeColor, b.icon, b.status, b.deletedAt, b.createdAt, b.updatedAt, " +
           "NULL, NULL) " +
           "FROM Board b " +
           "WHERE b.id = :boardId")
    Optional<BoardResponse> findBoardResponseById(@Param("boardId") Long boardId);
}
