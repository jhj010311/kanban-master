// Spec § 5. 프론트엔드 규격 - ColumnsSection with DnD
// FR-04a: 칼럼 드래그 앤 드롭 기능
// FR-04b: 순서 변경 저장 (낙관적 업데이트)
// FR-04c: 실패 시 롤백

import { useState, useEffect } from 'react';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  horizontalListSortingStrategy,
} from '@dnd-kit/sortable';
import { SortableColumnCard } from '@/components/SortableColumnCard';
import { useColumn } from '@/context/ColumnContext';
import type { Column } from '@/types/column';

type ColumnsSectionProps = {
  columns: Column[];
  columnsLoading: boolean;
  workspaceId: number;
  boardId: number;
  boardOwnerId: number;
  canEdit: boolean;
  onCreateColumn: () => void;
  autoOpenCardId: number | null;
  autoOpenColumnId: number | null;
  onAutoOpenHandled: () => void;
};

export const ColumnsSection = ({
  columns,
  columnsLoading,
  workspaceId,
  boardId,
  boardOwnerId,
  canEdit,
  onCreateColumn,
  autoOpenCardId,
  autoOpenColumnId,
  onAutoOpenHandled,
}: ColumnsSectionProps) => {
  const { updateColumnPosition, setColumnsOptimistic } = useColumn();
  const [localColumns, setLocalColumns] = useState<Column[]>(columns);
  const [previousColumns, setPreviousColumns] = useState<Column[]>(columns);

  // Spec § 5. 프론트엔드 규격 - columns prop 동기화
  // columns prop이 변경되면 localColumns 업데이트
  useEffect(() => {
    setLocalColumns(columns);
  }, [columns]);

  // Spec § 5. 프론트엔드 규격 - DnD Sensors
  // FR-04d: 키보드 네비게이션 지원
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  // Spec § 5. 프론트엔드 규격 - onDragEnd 핸들러
  // FR-04a: 드래그 앤 드롭으로 칼럼 순서 변경
  // FR-04b: 낙관적 업데이트로 즉시 UI 반영
  // FR-04c: 실패 시 이전 상태로 롤백
  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;

    if (!over || active.id === over.id) {
      return;
    }

    // 정렬된 칼럼 기준으로 인덱스 찾기
    const sortedColumns = [...localColumns].sort((a, b) => a.position - b.position);
    const oldIndex = sortedColumns.findIndex((col) => col.id === active.id);
    const newIndex = sortedColumns.findIndex((col) => col.id === over.id);

    if (oldIndex === -1 || newIndex === -1) {
      return;
    }

    // 드래그된 칼럼 정보 먼저 저장
    const draggedColumn = sortedColumns[oldIndex];

    // 1. 이전 상태 저장 (롤백용)
    setPreviousColumns(localColumns);

    // 2. 낙관적 업데이트 (즉시 UI 변경)
    const reorderedColumns = arrayMove(sortedColumns, oldIndex, newIndex).map((col, index) => ({
      ...col,
      position: index, // position 값도 즉시 업데이트하여 재정렬 방지
    }));
    setLocalColumns(reorderedColumns);
    setColumnsOptimistic(reorderedColumns); // Context도 함께 업데이트

    try {
      // 3. 백엔드에 position 업데이트
      // 드래그된 칼럼 하나만 업데이트 (백엔드가 나머지 칼럼들을 자동으로 조정)
      await updateColumnPosition(workspaceId, boardId, draggedColumn.id, newIndex);
      // 4. 낙관적 업데이트로 이미 UI가 변경되었으므로 추가 조회 불필요
    } catch (error) {
      // 5. 실패 시 이전 상태로 롤백
      console.error('칼럼 순서 변경 실패:', error);
      setLocalColumns(previousColumns);
      setColumnsOptimistic(previousColumns); // Context도 함께 롤백
    }
  };

  if (columnsLoading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-pastel-blue-600" />
      </div>
    );
  }

  if (columns.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-96">
        <div className="glass rounded-2xl p-8 shadow-glass-lg text-center max-w-md">
          <p className="text-lg text-pastel-blue-600 mb-4">칼럼이 없습니다</p>
          <p className="text-sm text-pastel-blue-500 mb-6">
            {canEdit ? '첫 번째 칼럼을 만들어 시작하세요' : '아직 칼럼이 생성되지 않았습니다'}
          </p>
          {canEdit && (
            <button
              onClick={onCreateColumn}
              className="px-6 py-2 rounded-lg bg-pastel-blue-500 text-white font-semibold hover:bg-pastel-blue-600 transition"
            >
              + 칼럼 생성
            </button>
          )}
        </div>
      </div>
    );
  }

  const sortedColumns = [...localColumns].sort((a, b) => a.position - b.position);
  const columnIds = sortedColumns.map((col) => col.id);

  return (
    <div className="flex-1 overflow-auto h-full">
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
      >
        <SortableContext items={columnIds} strategy={horizontalListSortingStrategy}>
          <div className="flex gap-4 pb-4 min-h-full h-full items-stretch">
            {sortedColumns.map((column) => {
              const shouldRespectColumnConstraint = autoOpenColumnId && autoOpenColumnId !== column.id;
              const cardIdForColumn = shouldRespectColumnConstraint ? null : autoOpenCardId;

              return (
                <SortableColumnCard
                  key={column.id}
                  column={column}
                  workspaceId={workspaceId}
                  boardId={boardId}
                  boardOwnerId={boardOwnerId}
                  canEdit={canEdit}
                  autoOpenCardId={cardIdForColumn}
                  onAutoOpenHandled={onAutoOpenHandled}
                />
              );
            })}

            {canEdit && (
              <button
                onClick={onCreateColumn}
                className="flex-shrink-0 w-80 min-h-28 rounded-2xl border-4 border-dashed border-white/70 bg-white/10 flex items-center justify-center text-pastel-blue-800 font-semibold text-base hover:bg-white/20 hover:border-black transition"
              >
                + 칼럼 추가
              </button>
            )}
          </div>
        </SortableContext>
      </DndContext>
    </div>
  );
};
