/**
 * 댓글 섹션 컴포넌트
 *
 * Spec § 4.1: CommentSection - 댓글 목록 및 입력 컨테이너
 * Spec § FR-06f: 페이지네이션 (더보기 버튼 방식)
 */

import { useState, useEffect } from 'react';
import type { Comment, CommentPage } from '../types/comment';
import {
  getComments,
  createComment,
  updateComment,
  deleteComment,
} from '../services/commentService';
import { CommentInput } from './CommentInput';
import { CommentItem } from './CommentItem';
import { EmptyComments } from './EmptyComments';
import { CommentSkeleton } from './CommentSkeleton';

interface CommentSectionProps {
  workspaceId: number;
  boardId: number;
  cardId: number;
  currentUserId: number;
  isOwner: boolean;
}

export const CommentSection = ({
  workspaceId,
  boardId,
  cardId,
  currentUserId,
  isOwner,
}: CommentSectionProps) => {
  const [comments, setComments] = useState<Comment[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 초기 댓글 목록 로드
  useEffect(() => {
    loadComments(0, true);
  }, [workspaceId, boardId, cardId]);

  const loadComments = async (pageNumber: number, reset: boolean = false) => {
    try {
      if (reset) {
        setIsLoading(true);
      } else {
        setIsLoadingMore(true);
      }
      setError(null);

      const response: CommentPage = await getComments(
        workspaceId,
        boardId,
        cardId,
        pageNumber
      );

      if (reset) {
        setComments(response.content);
      } else {
        setComments((prev) => [...prev, ...response.content]);
      }

      setPage(pageNumber);
      setTotalPages(response.totalPages);
    } catch (err) {
      console.error('Failed to load comments:', err);
      setError(err instanceof Error ? err.message : '댓글을 불러오는데 실패했습니다');
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  };

  const handleCreateComment = async (content: string) => {
    const newComment = await createComment(workspaceId, boardId, cardId, {
      content,
    });

    // 새 댓글을 목록 맨 위에 추가
    setComments((prev) => [newComment, ...prev]);
  };

  const handleUpdateComment = async (commentId: number, content: string) => {
    const updatedComment = await updateComment(
      workspaceId,
      boardId,
      cardId,
      commentId,
      { content }
    );

    setComments((prev) =>
      prev.map((comment) =>
        comment.id === commentId ? updatedComment : comment
      )
    );
  };

  const handleDeleteComment = async (commentId: number) => {
    await deleteComment(workspaceId, boardId, cardId, commentId);

    setComments((prev) => prev.filter((comment) => comment.id !== commentId));
  };

  const handleLoadMore = () => {
    loadComments(page + 1, false);
  };

  if (isLoading) {
    return (
      <div className="h-full flex flex-col">
        <h3 className="text-lg font-semibold mb-4">댓글</h3>
        <CommentSkeleton />
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-full flex flex-col">
        <h3 className="text-lg font-semibold mb-4">댓글</h3>
        <div className="text-center py-8 text-red-500">
          <p>{error}</p>
          <button
            onClick={() => loadComments(0, true)}
            className="mt-2 text-sm text-blue-600 hover:underline"
          >
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      <h3 className="text-lg font-semibold mb-4">댓글</h3>

      {/* 댓글 입력 */}
      <CommentInput onSubmit={handleCreateComment} />

      {/* 댓글 목록 */}
      <div className="flex-1 overflow-y-auto">
        {comments.length === 0 ? (
          <EmptyComments />
        ) : (
          <>
            {comments.map((comment) => (
              <CommentItem
                key={comment.id}
                comment={comment}
                currentUserId={currentUserId}
                isOwner={isOwner}
                onUpdate={handleUpdateComment}
                onDelete={handleDeleteComment}
              />
            ))}

            {/* 더보기 버튼 */}
            {page + 1 < totalPages && (
              <div className="text-center py-4">
                <button
                  onClick={handleLoadMore}
                  disabled={isLoadingMore}
                  className="px-4 py-2 text-sm text-blue-600 hover:bg-blue-50 rounded disabled:opacity-50"
                >
                  {isLoadingMore ? '로딩 중...' : '더보기'}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};
