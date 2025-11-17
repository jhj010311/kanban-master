// Spec § 5. 프론트엔드 규격 - SortableColumnCard 컴포넌트
// FR-04a: ColumnCard를 useSortable 훅으로 래핑하여 드래그 가능하게 만듦
// FR-04d: 키보드 네비게이션 지원 (Ctrl+←, Ctrl+→)
// FR-04e: 드래그 중 시각적 피드백 (opacity 50%, cursor grabbing)
// WCAG 2.1 AA: ARIA 속성 및 접근성 지원

import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { ColumnCard } from '@/components/ColumnCard';
import type { Column } from '@/types/column';

interface SortableColumnCardProps {
  column: Column;
  workspaceId: number;
  boardId: number;
  boardOwnerId: number;
  canEdit: boolean;
  autoOpenCardId?: number | null;
  onAutoOpenHandled?: () => void;
}

export const SortableColumnCard = ({
  column,
  workspaceId,
  boardId,
  boardOwnerId,
  canEdit,
  autoOpenCardId,
  onAutoOpenHandled,
}: SortableColumnCardProps) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: column.id,
    disabled: !canEdit,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1, // FR-04e: 드래그 중 투명도 50%
  };

  // Spec § 5. 프론트엔드 규격 - ARIA 속성
  // WCAG 2.1 AA 준수: 스크린 리더 사용자를 위한 명확한 설명
  const ariaLabel = canEdit
    ? `${column.name} 칼럼. 드래그하여 순서를 변경하거나 키보드로 Ctrl+왼쪽 화살표 또는 Ctrl+오른쪽 화살표를 눌러 이동할 수 있습니다.`
    : `${column.name} 칼럼`;

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      role="group"
      aria-label={ariaLabel}
      aria-roledescription={canEdit ? '드래그 가능한 칼럼' : '칼럼'}
    >
      <ColumnCard
        column={column}
        workspaceId={workspaceId}
        boardId={boardId}
        boardOwnerId={boardOwnerId}
        canEdit={canEdit}
        autoOpenCardId={autoOpenCardId}
        onAutoOpenHandled={onAutoOpenHandled}
        dragHandleProps={canEdit ? listeners : undefined}
      />
    </div>
  );
};
