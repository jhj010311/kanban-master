import React from 'react';
import type { ParentCardSummary } from '@/types/card';

/**
 * ParentCardLink 컴포넌트
 * Spec § 5. 프론트엔드 규격 - FR-06b, FR-06c: 부모 카드 정보 표시 및 네비게이션
 *
 * 책임:
 * - 부모 카드의 제목, 우선순위, 담당자 정보를 표시
 * - 클릭 시 onNavigate 콜백 호출
 * - 호버 및 클릭 애니메이션 제공
 */
interface ParentCardLinkProps {
  parentCard: ParentCardSummary;
  onNavigate: (cardId: number) => void;
  disabled?: boolean;
}

const ParentCardLink: React.FC<ParentCardLinkProps> = ({ parentCard, onNavigate, disabled = false }) => {
  const handleClick = () => {
    if (!disabled) {
      onNavigate(parentCard.id);
    }
  };

  const getPriorityColor = (priority?: string) => {
    switch (priority) {
      case 'HIGH':
        return 'text-red-500';
      case 'MEDIUM':
        return 'text-yellow-500';
      case 'LOW':
        return 'text-blue-500';
      default:
        return 'text-gray-400';
    }
  };

  const getPriorityLabel = (priority?: string) => {
    switch (priority) {
      case 'HIGH':
        return '높음';
      case 'MEDIUM':
        return '보통';
      case 'LOW':
        return '낮음';
      default:
        return '없음';
    }
  };

  return (
    <div className="mb-4 border border-gray-200 rounded-lg p-3 bg-gray-50">
      <div className="flex items-center gap-2 mb-2">
        <span className="text-sm font-medium text-gray-600">📌 부모 카드</span>
      </div>

      <button
        type="button"
        onClick={handleClick}
        disabled={disabled}
        className={`
          w-full text-left p-3 rounded-md border transition-all duration-200
          ${disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:shadow-md active:scale-98'}
        `}
        style={{
          backgroundColor: parentCard.bgColor || '#ffffff',
          borderColor: parentCard.bgColor || '#e5e7eb',
        }}
      >
        <div className="flex items-center justify-between">
          <div className="flex-1 min-w-0">
            <h4 className="text-sm font-semibold text-gray-900 truncate">{parentCard.title}</h4>
            <div className="mt-1 flex items-center gap-3 text-xs text-gray-600">
              {parentCard.priority && (
                <span className={`font-medium ${getPriorityColor(parentCard.priority)}`}>
                  우선순위: {getPriorityLabel(parentCard.priority)}
                </span>
              )}
              {parentCard.assignee && <span>담당: {parentCard.assignee}</span>}
            </div>
          </div>

          {!disabled && (
            <svg
              className="w-4 h-4 text-gray-400 flex-shrink-0 ml-2"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          )}
        </div>
      </button>
    </div>
  );
};

export default ParentCardLink;
