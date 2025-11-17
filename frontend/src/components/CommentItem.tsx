/**
 * 댓글 아이템 컴포넌트
 *
 * Spec § 4.3: CommentItem - 개별 댓글 표시
 * Spec § FR-06e: 작성자 정보 표시
 * Spec § FR-06m: 본인 댓글만 수정/삭제 가능
 */

import { useState } from 'react';
import type { Comment } from '../types/comment';
import RichTextEditor from './RichTextEditor';

interface CommentItemProps {
  comment: Comment;
  currentUserId: number;
  isOwner: boolean;
  onUpdate: (commentId: number, content: string) => Promise<void>;
  onDelete: (commentId: number) => Promise<void>;
}

export const CommentItem = ({
  comment,
  currentUserId,
  isOwner,
  onUpdate,
  onDelete,
}: CommentItemProps) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isAuthor = comment.authorId === currentUserId;
  const canEdit = isAuthor;
  const canDelete = isAuthor || isOwner;

  const handleSave = async () => {
    if (!editContent.trim()) {
      alert('댓글 내용을 입력해주세요');
      return;
    }

    if (editContent.length > 10000) {
      alert('댓글은 10,000자를 초과할 수 없습니다');
      return;
    }

    try {
      setIsSubmitting(true);
      await onUpdate(comment.id, editContent);
      setIsEditing(false);
    } catch (error) {
      console.error('Failed to update comment:', error);
      alert(error instanceof Error ? error.message : '댓글 수정에 실패했습니다');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = () => {
    setEditContent(comment.content);
    setIsEditing(false);
  };

  const handleDelete = async () => {
    if (!confirm('댓글을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await onDelete(comment.id);
    } catch (error) {
      console.error('Failed to delete comment:', error);
      alert(error instanceof Error ? error.message : '댓글 삭제에 실패했습니다');
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return '방금 전';
    if (minutes < 60) return `${minutes}분 전`;
    if (hours < 24) return `${hours}시간 전`;
    if (days < 7) return `${days}일 전`;
    return date.toLocaleDateString('ko-KR');
  };

  return (
    <div className="flex gap-3 py-3 border-b border-gray-200 last:border-0">
      {/* 작성자 아바타 */}
      <div className="flex-shrink-0">
        {comment.authorAvatarUrl ? (
          <img
            src={comment.authorAvatarUrl}
            alt={comment.authorName}
            className="w-8 h-8 rounded-full"
          />
        ) : (
          <div className="w-8 h-8 rounded-full bg-gray-300 flex items-center justify-center text-sm font-medium text-gray-700">
            {comment.authorName.charAt(0).toUpperCase()}
          </div>
        )}
      </div>

      {/* 댓글 내용 */}
      <div className="flex-1 min-w-0">
        {/* 작성자 정보 */}
        <div className="flex items-center gap-2 mb-1">
          <span className="font-medium text-sm text-gray-900">
            {comment.authorName}
          </span>
          <span className="text-xs text-gray-500">
            {formatDate(comment.updatedAt)}
            {comment.updatedAt !== comment.createdAt && ' (수정됨)'}
          </span>
        </div>

        {/* 댓글 본문 */}
        {isEditing ? (
          <div className="space-y-2">
            <RichTextEditor
              value={editContent}
              onChange={setEditContent}
              placeholder="댓글을 입력하세요..."
            />
            <div className="flex gap-2">
              <button
                onClick={handleSave}
                disabled={isSubmitting}
                className="px-3 py-1 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
              >
                {isSubmitting ? '저장 중...' : '저장'}
              </button>
              <button
                onClick={handleCancel}
                disabled={isSubmitting}
                className="px-3 py-1 text-sm bg-gray-200 text-gray-700 rounded hover:bg-gray-300 disabled:opacity-50"
              >
                취소
              </button>
            </div>
          </div>
        ) : (
          <>
            <div
              className="text-sm text-gray-800 prose prose-sm max-w-none"
              dangerouslySetInnerHTML={{ __html: comment.content }}
            />

            {/* 액션 버튼 */}
            {(canEdit || canDelete) && (
              <div className="flex gap-2 mt-2">
                {canEdit && (
                  <button
                    onClick={() => setIsEditing(true)}
                    className="text-xs text-blue-600 hover:underline"
                  >
                    수정
                  </button>
                )}
                {canDelete && (
                  <button
                    onClick={handleDelete}
                    className="text-xs text-red-600 hover:underline"
                  >
                    삭제
                  </button>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};
