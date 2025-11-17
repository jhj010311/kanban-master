import React from 'react';
import type { ChildCardSummary } from '@/types/card';

/**
 * ChildCardList 컴포넌트
 * Spec § 5. 프론트엔드 규격 - FR-06d, FR-06e: 자식 카드 목록 표시 및 네비게이션
 *
 * 책임:
 * - 자식 카드 목록을 표시
 * - 각 자식 카드 클릭 시 onNavigate 콜백 호출
 * - 완료된 카드는 취소선으로 표시 (결정 사항 1)
 * - 호버 및 클릭 애니메이션 제공
 */
interface ChildCardListProps {
  childCards: ChildCardSummary[];
  onNavigate: (cardId: number) => void;
  onCreateChild: () => void;
  disabled?: boolean;
}

const ChildCardList: React.FC<ChildCardListProps> = ({
  childCards,
  onNavigate,
  onCreateChild,
  disabled = false,
}) => {
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
        return 'HIGH';
      case 'MEDIUM':
        return 'MEDIUM';
      case 'LOW':
        return 'LOW';
      default:
        return '';
    }
  };

  return (
    <div className="mt-4">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
          <span>🔗 하위 카드</span>
          <span className="text-xs text-gray-500">({childCards.length})</span>
        </h3>
      </div>

      {childCards.length === 0 ? (
        <div className="border border-dashed border-gray-300 rounded-lg p-6 text-center bg-gray-50">
          <p className="text-sm text-gray-500 mb-1">하위 카드가 없습니다.</p>
          <p className="text-xs text-gray-400 mb-4">첫 하위 카드를 생성해보세요!</p>
          <button
            type="button"
            onClick={onCreateChild}
            disabled={disabled}
            className="text-sm text-blue-600 hover:text-blue-700 font-medium disabled:opacity-50 disabled:cursor-not-allowed"
          >
            + 하위 카드 생성
          </button>
        </div>
      ) : (
        <div className="space-y-2">
          {childCards.map((child) => (
            <button
              key={child.id}
              type="button"
              onClick={() => onNavigate(child.id)}
              disabled={disabled}
              className={`
                w-full text-left p-3 rounded-md border transition-all duration-200
                ${disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:shadow-md active:scale-98'}
              `}
              style={{
                backgroundColor: child.bgColor || '#ffffff',
                borderColor: child.bgColor || '#e5e7eb',
              }}
            >
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0 flex items-center gap-2">
                  <h4
                    className={`text-sm font-medium text-gray-900 truncate ${
                      child.isCompleted ? 'line-through opacity-60' : ''
                    }`}
                  >
                    {child.title}
                  </h4>
                  {child.isCompleted && <span className="text-green-500 text-lg flex-shrink-0">✅</span>}
                  {!child.isCompleted && <span className="text-blue-500 text-lg flex-shrink-0">🔄</span>}
                </div>

                <div className="flex items-center gap-2 flex-shrink-0 ml-2">
                  {child.priority && (
                    <span
                      className={`text-xs font-semibold ${getPriorityColor(
                        child.priority
                      )}`}
                    >
                      {getPriorityLabel(child.priority)}
                    </span>
                  )}
                  {!disabled && (
                    <svg
                      className="w-4 h-4 text-gray-400"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                  )}
                </div>
              </div>
            </button>
          ))}

          <button
            type="button"
            onClick={onCreateChild}
            disabled={disabled}
            className="w-full p-3 border-2 border-dashed border-gray-300 rounded-md text-sm text-gray-600 hover:border-blue-400 hover:text-blue-600 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            + 하위 카드 생성
          </button>
        </div>
      )}
    </div>
  );
};

export default ChildCardList;
