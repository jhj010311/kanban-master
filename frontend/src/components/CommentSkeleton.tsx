/**
 * 댓글 로딩 스켈레톤 컴포넌트
 *
 * Spec § 4.6: CommentSkeleton - 로딩 상태 표시
 */

export const CommentSkeleton = () => {
  return (
    <div className="animate-pulse">
      {[1, 2, 3].map((index) => (
        <div
          key={index}
          className="flex gap-3 py-3 border-b border-gray-200 last:border-0"
        >
          {/* 아바타 스켈레톤 */}
          <div className="flex-shrink-0">
            <div className="w-8 h-8 bg-gray-200 rounded-full" />
          </div>

          {/* 콘텐츠 스켈레톤 */}
          <div className="flex-1 space-y-2">
            {/* 작성자 이름 */}
            <div className="h-4 bg-gray-200 rounded w-24" />
            {/* 댓글 내용 */}
            <div className="space-y-1">
              <div className="h-3 bg-gray-200 rounded w-full" />
              <div className="h-3 bg-gray-200 rounded w-5/6" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};
