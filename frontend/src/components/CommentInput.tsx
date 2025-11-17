/**
 * 댓글 입력 컴포넌트
 *
 * Spec § 4.2: CommentInput - 댓글 작성 폼
 * Spec § FR-06a: 댓글 작성
 * Spec § FR-06g: 빈 댓글 방지
 * Spec § FR-06i: XSS 방지
 */

import { useState } from 'react';
import RichTextEditor from './RichTextEditor';

interface CommentInputProps {
  onSubmit: (content: string) => Promise<void>;
  placeholder?: string;
}

export const CommentInput = ({
  onSubmit,
  placeholder = '댓글을 입력하세요...',
}: CommentInputProps) => {
  const [content, setContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async () => {
    // Spec § FR-06g: 빈 댓글 방지
    if (!content.trim()) {
      alert('댓글 내용을 입력해주세요');
      return;
    }

    // Spec § NFR-06c: 최대 10,000자 제한
    if (content.length > 10000) {
      alert('댓글은 10,000자를 초과할 수 없습니다');
      return;
    }

    try {
      setIsSubmitting(true);
      await onSubmit(content);
      setContent(''); // 성공 시 입력 필드 초기화
    } catch (error) {
      console.error('Failed to submit comment:', error);
      alert(error instanceof Error ? error.message : '댓글 작성에 실패했습니다');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="border-b border-gray-200 pb-4 mb-4">
      <RichTextEditor
        value={content}
        onChange={setContent}
        placeholder={placeholder}
      />
      <div className="flex justify-between items-center mt-2">
        <span className="text-xs text-gray-500">
          {content.length.toLocaleString()} / 10,000
        </span>
        <button
          onClick={handleSubmit}
          disabled={isSubmitting || !content.trim()}
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? '작성 중...' : '댓글 작성'}
        </button>
      </div>
    </div>
  );
};
