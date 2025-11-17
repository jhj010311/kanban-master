/**
 * 댓글 없음 상태 컴포넌트
 *
 * Spec § 4.5: EmptyComments - 댓글이 없을 때 표시
 */

export const EmptyComments = () => {
  return (
    <div className="text-center py-8 text-gray-500">
      <svg
        className="mx-auto h-12 w-12 text-gray-400 mb-3"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        aria-hidden="true"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
        />
      </svg>
      <p className="text-sm">아직 댓글이 없습니다</p>
      <p className="text-xs mt-1">첫 번째 댓글을 작성해보세요!</p>
    </div>
  );
};
