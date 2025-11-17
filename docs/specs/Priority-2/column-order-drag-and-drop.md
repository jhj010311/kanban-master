# spec-column-drag-drop-004 — 칼럼 순서 변경 (드래그 앤 드롭)

## 1. 개요

사용자가 보드 상세 페이지에서 마우스 드래그 앤 드롭으로 칼럼의 좌우 순서를 자유롭게 변경할 수 있는 기능을 완성한다.

- 백엔드의 `position` 필드와 업데이트 로직을 활용하여 순서 변경을 영구 저장한다.
- 프론트엔드에서 낙관적 업데이트를 통해 즉각적인 UX를 제공하고, 실패 시 자동 롤백한다.
- 접근성을 고려하여 키보드 네비게이션도 지원한다.

## 2. 연계 요구사항

- FR-04a 칼럼 드래그 앤 드롭
- FR-04b 순서 변경 저장
- FR-04c 실패 시 롤백
- FR-04d 키보드 네비게이션 (Should)
- FR-04e 시각적 피드백 (Should)

## 3. 주요 사용자 시나리오

### 시나리오 1: 기본 드래그 앤 드롭
1. 사용자가 보드 상세 페이지에서 칼럼들을 본다.
2. "진행 중" 칼럼을 마우스로 드래그한다.
3. "완료" 칼럼 앞에 드롭하면 순서가 변경된다.
4. UI는 즉시 반영되고, 백엔드에 새로운 `position` 값이 저장된다.
5. 페이지를 새로고침해도 변경된 순서가 유지된다.

### 시나리오 2: 실패 시 자동 복구
1. 사용자가 칼럼을 드래그하여 드롭한다.
2. UI는 즉시 변경되지만, 네트워크 오류로 백엔드 저장이 실패한다.
3. 토스트 알림 "칼럼 순서 변경 실패"가 표시되고, 3초 후 자동 사라진다.
4. UI는 자동으로 이전 순서로 롤백된다.

### 시나리오 3: 키보드 접근성
1. Tab 키로 칼럼 중 하나에 포커스를 맞춘다.
2. Ctrl + ← (또는 Cmd + ← for Mac)로 칼럼을 왼쪽으로 이동한다.
3. Ctrl + → 로 칼럼을 오른쪽으로 이동한다.
4. 마우스가 없어도 순서 변경이 가능하다.

## 4. 디자인 가이드라인

### 칼럼 헤더 영역
```
┌─────────────────────────────┐
│ 📌 할 일 (3)                 │ ← draggable 영역
├─────────────────────────────┤
│ [카드 1]                      │
│ [카드 2]                      │
└─────────────────────────────┘
```

- 칼럼 헤더(제목 + 카드 수)가 드래그 가능한 영역이다.
- 호버할 때: 커서가 `grab` (손가락 아이콘)으로 변한다.
- 드래그 중: 커서가 `grabbing` 으로 변하고 칼럼 투명도가 50%로 감소한다.

### 드롭 영역 하이라이트
```
[할 일]  |→  [진행중]  [완료]
         ↓
         수평선 표시 (border-left 또는 배경색)
```

- 다른 칼럼 위에 호버할 때, 드롭할 위치가 명확하도록 좌측 경계선(3px, pastel-blue-500)을 강조한다.
- 또는 배경색을 연하게 변경할 수 있다.

### 애니메이션
- **드래그 중**: 즉각적 (0ms)
- **드롭 완료**: 300ms의 smooth 트랜지션으로 칼럼이 최종 위치로 이동

### 오류 상황
```
┌─────────────────────────────────┐
│ ⚠️ 칼럼 순서 변경 실패했습니다   │ ← 토스트 (3초 후 사라짐)
└─────────────────────────────────┘
```

- 토스트 배경: `pastel-pink-500` (경고색)
- 텍스트: 흰색
- 위치: 화면 우측 하단 또는 상단 중앙
- 자동 사라짐: 3초 후

## 5. 프론트엔드 규격

### 라이브러리 및 패키지

**의존성 추가:**
```json
{
  "dependencies": {
    "@dnd-kit/core": "^8.2.0",
    "@dnd-kit/sortable": "^8.0.0",
    "@dnd-kit/utilities": "^3.2.0"
  }
}
```

### 페이지/컴포넌트 구조

#### 1. BoardDetailPage.tsx (수정)

**변경점:**
- `DndContext` 추가로 드래그 컨텍스트 감싸기
- `SortableContext` 로 칼럼 목록 감싸기
- `onDragEnd` 핸들러 구현

**핵심 로직:**
```
columns 배열 초기 상태 = API에서 로드
사용자 드래그
  ↓
onDragEnd 핸들러 호출
  ↓
oldIndex, newIndex 계산
  ↓
arrayMove()로 즉시 UI 업데이트 (낙관적 업데이트)
  ↓
position 값 재계산 (0, 1, 2, ...)
  ↓
updateColumnPosition() API 호출 (각 변경된 칼럼)
  ↓
성공: 아무것도 하지 않음 (이미 UI는 변경됨)
실패: 이전 columns 복원 + 토스트 표시
```

#### 2. SortableColumnCard.tsx (신규 생성)

**역할:** ColumnCard를 useSortable 훅으로 래핑한 버전

**구성:**
```typescript
export const SortableColumnCard = ({ column, workspaceId, boardId }) => {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: column.id,
  });

  return (
    <div
      ref={setNodeRef}
      style={{
        transform: CSS.Transform.toString(transform),
        transition,
        opacity: isDragging ? 0.5 : 1,
      }}
      {...attributes}
      {...listeners}
      className="cursor-grab active:cursor-grabbing"
    >
      <ColumnCard column={column} workspaceId={workspaceId} boardId={boardId} />
    </div>
  );
};
```

**속성:**
- `attributes`: draggable 속성 (`draggable="true"` 등)
- `listeners`: 드래그 이벤트 리스너
- `setNodeRef`: DOM 요소에 ref 할당
- `transform`: 드래그 중 칼럼 위치 변환
- `transition`: 애니메이션 설정
- `isDragging`: 현재 드래그 중인지 여부

#### 3. ColumnContext.tsx (확장)

**기존 함수:**
- `loadColumns(workspaceId, boardId)`
- `createColumn(workspaceId, boardId, request)`
- `updateColumn(workspaceId, boardId, columnId, request)`
- `deleteColumn(workspaceId, boardId, columnId)`

**신규 함수:**
```typescript
updateColumnPosition(
  workspaceId: number,
  boardId: number,
  columnId: number,
  newPosition: number
): Promise<Column>
```

### 상태 관리

**BoardDetailPage 상태:**
```typescript
const [columns, setColumns] = useState<Column[]>([]);
const [previousColumns, setPreviousColumns] = useState<Column[]>([]);
const [isUpdating, setIsUpdating] = useState(false);
```

**Context 상태 (기존과 동일):**
```typescript
{
  columns: Column[];
  loading: boolean;
  error: string | null;
}
```

### 에러 처리

**네트워크 오류:**
```typescript
const handleDragEnd = async (event: DragEndEvent) => {
  // ... 드래그 로직

  try {
    // 1. 낙관적 업데이트 (즉시 UI 변경)
    setColumns(newColumns);

    // 2. 백엔드 저장 시도
    for (const col of changedColumns) {
      await updateColumnPosition(workspaceId, boardId, col.id, col.position);
    }
  } catch (error) {
    // 3. 실패 시 이전 상태 복원
    setColumns(previousColumns);

    // 4. 토스트 알림
    showToast('칼럼 순서 변경 실패했습니다', 'error', 3000);

    // 5. 자동 재시도 (최대 3회, 각 1초 간격)
    // ... 재시도 로직
  }
};
```

**재시도 로직:**
```
시도 1: 즉시
시도 2: 1초 후
시도 3: 2초 후
모두 실패: 롤백 후 사용자에게 알림
```

### 키보드 네비게이션

**접근성 구현:**

1. 포커스 가능하도록 칼럼 헤더에 `tabIndex="0"` 추가
2. 키보드 이벤트 핸들러:

```typescript
const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>, columnId: number) => {
  const currentIndex = columns.findIndex(c => c.id === columnId);

  if (event.ctrlKey || event.metaKey) {  // Ctrl or Cmd
    if (event.key === 'ArrowLeft' && currentIndex > 0) {
      // 왼쪽으로 이동
      const newColumns = arrayMove(columns, currentIndex, currentIndex - 1);
      setColumns(newColumns);
      saveColumnPositions(newColumns);
      event.preventDefault();
    } else if (event.key === 'ArrowRight' && currentIndex < columns.length - 1) {
      // 오른쪽으로 이동
      const newColumns = arrayMove(columns, currentIndex, currentIndex + 1);
      setColumns(newColumns);
      saveColumnPositions(newColumns);
      event.preventDefault();
    }
  }
};
```

3. ARIA 속성:
```tsx
<div
  role="button"
  tabIndex={0}
  onKeyDown={(e) => handleKeyDown(e, column.id)}
  aria-label={`${column.name} 칼럼, 드래그하거나 Ctrl+화살표 키로 순서 변경`}
>
  {column.name}
</div>
```

## 6. 백엔드 규격

### 기존 API 재사용

**칼럼 조회:**
```
GET /api/v1/workspaces/{workspaceId}/boards/{boardId}/columns
응답: Column[] (position 순서로 정렬됨)

응답 예시:
[
  { id: 1, name: "할 일", position: 0, ... },
  { id: 2, name: "진행 중", position: 1, ... },
  { id: 3, name: "완료", position: 2, ... }
]
```

**칼럼 위치 업데이트:**
```
PUT /api/v1/workspaces/{workspaceId}/boards/{boardId}/columns/{columnId}
요청 본문:
{
  "position": 1
}

응답:
{
  "id": 1,
  "name": "할 일",
  "position": 1,
  "boardId": 10,
  "description": "...",
  "bgColor": "...",
  "createdAt": "2024-11-01T...",
  "updatedAt": "2024-11-09T..."
}
```

### 트랜잭션 안정성

**단일 칼럼 이동 시 (예: columnId=1을 position=2로):**

```
Before: position 0,1,2,3
After:  position 0,1,2,3

칼럼 1 (기존 position=0) → position=2로 변경
영향받는 칼럼:
  - 칼럼 2 (기존 position=1) → position=0으로 조정
  - 칼럼 3 (기존 position=2) → position=1로 조정
  - 칼럼 4 (기존 position=3) → 변경 없음
```

**트랜잭션:**
- 모든 position 업데이트가 한 트랜잭션으로 처리되어 일관성 보장
- 실패 시 모두 롤백되어 데이터 무결성 유지

### 동시성 처리

**경합 조건 (Race Condition):**

A 사용자와 B 사용자가 동시에 칼럼 순서를 변경할 수 있다.

**현재 전략 (낙관적 업데이트):**
- 프론트엔드에서 먼저 UI 변경
- 백엔드에 position 저장
- 백엔드는 현재 DB 상태를 기준으로 계산

**한계:**
- A가 position 0→2로 변경, B가 position 1→0으로 변경하면 충돌 가능
- 현재는 "마지막 요청이 승리" 모델 (Last Write Wins)

**개선 방안 (추후):**
- 타임스탬프 기반 낙관적 잠금 (Optimistic Locking)
- WebSocket으로 실시간 동기화

## 7. 수용 기준

1. 사용자가 마우스로 칼럼을 드래그하여 순서를 변경할 수 있다.
2. 드롭 즉시 UI가 반영되고, 동시에 백엔드에 저장된다.
3. 페이지를 새로고침한 후에도 변경된 순서가 유지된다.
4. 네트워크 오류 발생 시 자동으로 이전 순서로 복원되고, 토스트 알림이 표시된다.
5. Ctrl + ← / Ctrl + → 키로도 칼럼 순서를 변경할 수 있다.
6. 모든 모던 브라우저(Chrome, Firefox, Safari, Edge)에서 정상 동작한다.
7. 키보드 포커스 링이 명확하게 표시되어 접근성이 확보된다.
8. 드래그 중인 칼럼이 시각적으로 구분되고, 호버 위치가 하이라이트된다.

## 8. 구현 순서

### Phase 1: 환경 설정 (0.5일)
- [ ] `@dnd-kit` 라이브러리 설치
- [ ] 타입 정의 확인 및 필요시 확장

### Phase 2: 프론트엔드 기본 구현 (1.5일)
- [ ] BoardDetailPage에 DndContext, SortableContext 추가
- [ ] SortableColumnCard 컴포넌트 작성
- [ ] onDragEnd 핸들러 구현 (낙관적 업데이트)

### Phase 3: 백엔드 연동 (0.5일)
- [ ] updateColumnPosition API 호출 로직
- [ ] 재시도 로직 구현 (3회)
- [ ] 에러 처리 및 롤백

### Phase 4: UX 개선 (1일)
- [ ] 토스트 알림 구현
- [ ] 드래그 시각적 피드백 (cursor, opacity)
- [ ] 호버 영역 하이라이트

### Phase 5: 접근성 (0.5일)
- [ ] 키보드 네비게이션 (Ctrl+←, Ctrl+→)
- [ ] ARIA 속성 추가
- [ ] 포커스 링 스타일링

### Phase 6: 테스트 (1.5일)
- [ ] 단위 테스트 (position 계산, arrayMove)
- [ ] 통합 테스트 (API 호출, 상태 동기화)
- [ ] E2E 테스트 (사용자 플로우)
- [ ] 브라우저 호환성 테스트

**총 소요 시간: ~5.5일**

## 9. 위험 요소 및 완화 전략

| 위험 | 영향 | 완화 전략 |
|------|------|----------|
| 동시 업데이트 충돌 | 데이터 불일치 | 타임스탐프 기반 낙관적 잠금 (추후) |
| 네트워크 오류 | 사용자 혼동 | 자동 재시도 + 명확한 에러 메시지 |
| 브라우저 호환성 | 일부 사용자 사용 불가 | dnd-kit는 모던 브라우저만 지원 (IE 미지원) |
| 성능 저하 | 칼럼 많을 때 느려짐 | 가상화 검토 (칼럼 50개 이상일 때) |

## 10. 테스트 전략

### Unit Tests
- Position 재계산 로직 (0→2로 이동 시 배열 재정렬)
- arrayMove 함수 검증
- 롤백 로직 테스트

### Integration Tests
```
1. 칼럼 드래그 후 위치 변경 확인
2. API 호출 여부 확인
3. 응답 성공 시 상태 유지
4. 응답 실패 시 상태 복원
5. 재시도 로직 작동 확인
```

### E2E Tests (Playwright)
```
시나리오 1: 정상 드래그 앤 드롭
  1. 보드 상세 페이지 로드
  2. 칼럼 1을 드래그하여 칼럼 3 앞에 드롭
  3. UI에서 순서 변경 확인
  4. 새로고침 후 순서 유지 확인

시나리오 2: 네트워크 오류 복구
  1. 드래그 후 드롭
  2. 네트워크 오류 시뮬레이션
  3. 토스트 알림 표시 확인
  4. UI 자동 복원 확인
  5. 재시도 후 성공 확인

시나리오 3: 키보드 네비게이션
  1. Tab으로 칼럼에 포커스
  2. Ctrl+← 로 왼쪽 이동
  3. Ctrl+→ 로 오른쪽 이동
  4. 순서 변경 확인

시나리오 4: 브라우저 호환성
  - Chrome, Firefox, Safari, Edge 모두 테스트
```

### 성능 테스트
- 칼럼 20개 기준: 드래그~드롭 완료까지 100ms 이내
- 애니메이션 프레임율: 60fps 유지

## 11. Notes

- **CardContext와의 충돌**: 카드의 드래그 기능과 겹치지 않도록 이벤트 전파(propagation) 주의
- **모바일**: 이번 구현은 마우스 중심. 터치는 Priority-3에서 다룰 예정
- **다중 보드**: 칼럼 순서는 보드별로 독립적으로 관리 (다른 보드에 영향 없음)
- **아카이브된 칼럼**: 아직 아카이브 기능이 없으므로 고려 불필요

## 12. Related Documents

- `../Priority-2/column-order-drag-and-drop.md` - 요구사항 문서
- `../Priority-1/spec-columns-002.md` - 칼럼 관리 명세
- `../Priority-1/spec-cards-003.md` - 카드 드래그 앤 드롭 명세 (참고)
- `../Priority-1/model-columns-002.md` - 칼럼 데이터 모델
