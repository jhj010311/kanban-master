# spec-card-hierarchy-support — 부모-자식 카드 계층 구조

## 1. 개요

칸반 보드에서 카드 간 부모-자식 관계를 지원하여 복잡한 작업을 세부 작업으로 분해하고 계층적으로 관리할 수 있도록 한다.

- 1단계 계층 구조만 지원 (부모 → 자식, 손자 불가)
- 카드 상세 모달에서 부모/자식 카드 정보를 표시하고 클릭으로 네비게이션
- 부모 카드 삭제 시 자식 카드는 최상위로 승격
- 자식 카드 이동 시 부모 관계 자동 해제

## 2. 연계 요구사항

- FR-06a 부모-자식 관계 데이터 모델
- FR-06b 부모 카드 정보 표시
- FR-06c 부모 카드 네비게이션
- FR-06d 자식 카드 목록 표시
- FR-06e 자식 카드 네비게이션
- FR-06f 하위 카드 생성 버튼
- FR-06g 자식 개수 표시
- FR-06h 부모 카드 삭제 시 자식 승격
- FR-06i 자식 카드 이동 시 관계 해제
- FR-06j 계층 제한 검증
- FR-06k 권한 검증
- NFR-06a 성능 (카드 네비게이션 < 500ms)
- NFR-06b 보안 (동일 보드 검증)

## 3. 주요 사용자 시나리오

### 시나리오 1: 하위 카드 생성
1. 사용자가 카드를 클릭하여 EditCardModal을 연다.
2. 모달 하단의 "하위 카드 목록" 섹션에서 "+ 하위 카드 생성" 버튼을 클릭한다.
3. CreateCardModal이 열리고 상단에 "📎 '부모 카드 제목'의 하위 카드 생성" 컨텍스트가 표시된다.
4. 사용자가 하위 카드 제목과 설명을 입력한다.
5. "생성" 버튼을 클릭하면 백엔드로 parentCardId와 함께 요청이 전송된다.
6. 성공 시 CreateCardModal이 닫히고, EditCardModal의 자식 카드 목록이 자동 갱신된다.

### 시나리오 2: 자식 카드로 네비게이션
1. 사용자가 부모 카드의 EditCardModal을 열고 있다.
2. 모달 하단의 "하위 카드" 섹션에 3개의 자식 카드가 목록으로 표시된다.
3. 사용자가 "하위 카드 2" 항목을 클릭한다.
4. 300ms 페이드 아웃 애니메이션이 재생되며 로딩 스피너가 표시된다.
5. 백엔드에서 "하위 카드 2"의 전체 정보를 조회한다.
6. 모달의 모든 필드가 "하위 카드 2" 데이터로 교체된다.
7. 300ms 페이드 인 애니메이션이 재생되며 모달이 다시 표시된다.
8. 모달 상단에 "📌 부모 카드" 섹션이 표시되어 원래 부모 카드 정보를 보여준다.

### 시나리오 3: 부모 카드로 네비게이션
1. 사용자가 자식 카드의 EditCardModal을 열고 있다.
2. 모달 상단에 "📌 부모 카드" 섹션이 표시되고, 부모 카드의 제목과 우선순위가 보인다.
3. 사용자가 부모 카드 링크를 클릭한다.
4. 페이드 전환 애니메이션과 함께 모달 데이터가 부모 카드로 교체된다.
5. 모달 하단에 자식 카드 목록이 다시 표시된다.

### 시나리오 4: 부모 카드 삭제
1. 사용자가 3개의 자식 카드를 가진 부모 카드를 삭제한다.
2. 백엔드에서 트랜잭션을 시작한다.
3. 3개의 자식 카드의 parentCard 필드를 null로 업데이트한다.
4. 부모 카드를 삭제한다.
5. 트랜잭션을 커밋한다.
6. 자식 카드 3개는 이제 최상위 카드로 칼럼에 표시된다.

### 시나리오 5: 자식 카드 이동
1. 사용자가 부모 카드를 가진 자식 카드를 다른 칼럼으로 드래그 앤 드롭한다.
2. 백엔드에서 카드 업데이트 요청을 받는다.
3. columnId가 변경되었음을 감지한다.
4. 자식 카드의 parentCard를 null로 설정한다.
5. 칼럼 변경과 함께 저장한다.
6. 부모 카드의 EditCardModal이 열려 있으면 자식 카드 목록에서 해당 카드가 제거된다.

### 시나리오 6: 카드 목록에서 자식 개수 확인
1. 사용자가 보드 페이지를 연다.
2. 각 카드 아이템에 자식 카드가 있으면 "📎 3" 아이콘이 우측 하단에 표시된다.
3. 사용자가 자식 카드 개수를 통해 복잡한 작업을 한눈에 파악할 수 있다.

## 4. 디자인 가이드라인

### EditCardModal 레이아웃 (부모/자식 정보 추가)

```
┌──────────────────────────────────────────────────────────────┐
│ 카드 수정                                                     │
├──────────────────────────────────────────────────────────────┤
│ [📌 부모 카드]  ← 자식 카드인 경우에만 표시                  │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ [색상] ✨ 부모 카드 제목                                  │ │
│ │ 우선순위: HIGH | 담당: 홍길동                             │ │
│ └──────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────┤
│ 제목 입력                                                     │
│ 우선순위 선택                                                 │
│ 담당자 입력                                                   │
│ ...                                                           │
├──────────────────────────────────────────────────────────────┤
│ [🔗 하위 카드 (3)]  ← 자식이 있는 경우에만 표시              │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ [색상] 하위 카드 1        완료 ✅ | HIGH                  │ │
│ └──────────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ [색상] 하위 카드 2        진행중 🔄 | MEDIUM              │ │
│ └──────────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ [색상] 하위 카드 3        대기 ⏳ | LOW                   │ │
│ └──────────────────────────────────────────────────────────┘ │
│ + 하위 카드 생성                                              │
├──────────────────────────────────────────────────────────────┤
│ 완료 상태 체크박스                                            │
│ [취소] [수정] 버튼                                            │
└──────────────────────────────────────────────────────────────┘
```

### 부모 카드 링크 스타일

**일반 상태:**
- 배경: `bg-pastel-blue-50`
- 테두리: `border border-pastel-blue-200`
- 패딩: `px-4 py-3`
- 둥근 모서리: `rounded-xl`

**호버 상태:**
- 배경: `hover:bg-pastel-blue-100`
- 커서: `cursor-pointer`
- 그림자: `hover:shadow-md`

**클릭 효과:**
- 배경: `active:bg-pastel-blue-200`
- 크기: `scale-98`

### 자식 카드 아이템 스타일

**일반 상태:**
- 배경: 카드 bgColor 또는 기본 `bg-white`
- 테두리: `border border-pastel-blue-200`
- 패딩: `px-4 py-2`
- 둥근 모서리: `rounded-lg`
- 간격: `gap-2`

**호버 상태:**
- 배경: `hover:bg-pastel-blue-50`
- 테두리: `hover:border-pastel-blue-300`

**완료 상태 아이콘:**
- 완료: ✅ (pastel-green-500)
- 진행중: 🔄 (pastel-blue-500)
- 대기: ⏳ (pastel-yellow-500)

### "하위 카드 생성" 버튼

**스타일:**
- 배경: `bg-pastel-blue-100`
- 텍스트: `text-pastel-blue-700 font-semibold`
- 테두리: `border-2 border-dashed border-pastel-blue-300`
- 패딩: `px-4 py-2`
- 호버: `hover:bg-pastel-blue-200`
- 아이콘: `+` (왼쪽)

### CardItem 자식 개수 표시

```
┌────────────────────────────────┐
│ 카드 제목                       │
│ 설명 미리보기...                │
│                                 │
│ 우선순위: HIGH    📎 3  ← 우측 하단 │
└────────────────────────────────┘
```

**스타일:**
- 배경: `bg-pastel-blue-500`
- 텍스트: `text-white text-xs font-bold`
- 패딩: `px-2 py-1`
- 둥근 모서리: `rounded-full`

### CreateCardModal 컨텍스트 표시 (하위 카드 생성 시)

```
┌──────────────────────────────────────┐
│ 카드 생성                             │
├──────────────────────────────────────┤
│ 📎 "부모 카드 제목"의 하위 카드 생성 │  ← 상단 컨텍스트
│ (배경: pastel-blue-100)              │
├──────────────────────────────────────┤
│ 제목 입력                             │
│ ...                                   │
└──────────────────────────────────────┘
```

### 로딩 상태 (네비게이션 중)

```
┌──────────────────────────────────────┐
│         [로딩 스피너]                 │
│      카드 정보를 불러오는 중...       │
└──────────────────────────────────────┘
```

**스피너 스타일:**
- 크기: `w-8 h-8`
- 색상: `border-pastel-blue-400`
- 애니메이션: `animate-spin`

### 빈 상태 메시지 (자식 카드 없음)

```
┌──────────────────────────────────────┐
│ 🔗 하위 카드 (0)                      │
├──────────────────────────────────────┤
│ 하위 카드가 없습니다.                 │
│ 첫 하위 카드를 생성해보세요!          │
│                                       │
│ + 하위 카드 생성                      │
└──────────────────────────────────────┘
```

## 5. 프론트엔드 규격

### 타입 정의 확장

**Card 인터페이스:**
```
interface Card {
  id: number;
  columnId: number;
  title: string;
  description?: string;
  position: number;
  bgColor?: string;
  priority?: string;
  assignee?: string;
  dueDate?: string;
  isCompleted: boolean;
  labels?: Label[];
  commentCount?: number;
  createdAt: string;
  updatedAt: string;

  // ✅ 새로 추가
  parentCardId?: number;
  parentCard?: ParentCardSummary;
  childCards?: ChildCardSummary[];
}

interface ParentCardSummary {
  id: number;
  title: string;
  bgColor?: string;
  priority?: string;
  assignee?: string;
}

interface ChildCardSummary {
  id: number;
  title: string;
  bgColor?: string;
  priority?: string;
  isCompleted: boolean;
}
```

**CreateCardRequest 확장:**
```
interface CreateCardRequest {
  title: string;
  description?: string;
  bgColor?: string;
  priority?: string;
  assignee?: string;
  dueDate?: string;

  // ✅ 새로 추가
  parentCardId?: number;
}
```

### 컴포넌트 구조

#### 새 컴포넌트: ParentCardLink

**Props 인터페이스:**
```
interface ParentCardLinkProps {
  parentCard: ParentCardSummary;
  onNavigate: (cardId: number) => void;
  disabled?: boolean;
}
```

**책임:**
- 부모 카드의 제목, 우선순위, 담당자 정보를 표시
- 클릭 시 onNavigate 콜백 호출
- 호버 및 클릭 애니메이션 제공

#### 새 컴포넌트: ChildCardList

**Props 인터페이스:**
```
interface ChildCardListProps {
  childCards: ChildCardSummary[];
  onNavigate: (cardId: number) => void;
  onCreateChild: () => void;
  disabled?: boolean;
}
```

**책임:**
- 자식 카드 목록을 렌더링
- 각 자식 카드 클릭 시 onNavigate 호출
- "하위 카드 생성" 버튼 렌더링
- 빈 상태 메시지 표시

#### EditCardModal 리팩토링

**Props 변경:**
```
interface EditCardModalProps {
  // ✅ 변경: card 객체 대신 cardId만 받기
  cardId: number;
  workspaceId: number;
  boardId: number;
  columnId: number;
  boardOwnerId: number;
  canEdit: boolean;
  onClose: () => void;
}
```

**내부 상태:**
```
- currentCardId: number
- cardData: Card | null
- isLoadingCard: boolean
- navigationError: string | null
```

**주요 로직:**
- useEffect로 cardId 변경 감지 시 데이터 재조회
- navigateToCard() 함수로 부모/자식 카드 전환
- 페이드 인/아웃 애니메이션 처리

#### CreateCardModal 확장

**Props 추가:**
```
interface CreateCardModalProps {
  workspaceId: number;
  boardId: number;
  columnId: number;
  onClose: () => void;
  onSuccess?: (card: Card) => void;

  // ✅ 새로 추가
  parentCardId?: number;
  parentCardTitle?: string;
}
```

#### CardItem 확장

**Props 추가:**
```
interface CardItemProps {
  card: Card;
  // ... 기존 props

  // ✅ 새로 추가
  childCount?: number;
}
```

### 상태 관리

#### CardContext 확장

**새 메서드:**
```
- getCardWithRelations(cardId): Promise<Card>
  → 부모/자식 정보 포함 카드 조회

- createChildCard(parentCardId, request): Promise<Card>
  → 하위 카드 생성
```

### CardService 확장

**새 메서드:**
```
- getCardWithRelations(workspaceId, boardId, columnId, cardId): Promise<Card>
  → GET /api/workspaces/{wid}/boards/{bid}/columns/{cid}/cards/{cardId}?includeRelations=true

- createChildCard(workspaceId, boardId, columnId, parentCardId, request): Promise<Card>
  → POST /api/workspaces/{wid}/boards/{bid}/columns/{cid}/cards
  → Body: { ...request, parentCardId }
```

### 애니메이션

**페이드 전환:**
```
- fadeOut: opacity 1 → 0, 300ms ease-out
- fadeIn: opacity 0 → 1, 300ms ease-in
```

**호버 효과:**
```
- 배경색 변경: transition-colors duration-200
- 스케일: scale-98, transition-transform duration-150
```

## 6. 백엔드 규격

### 데이터베이스 스키마 확장

**Card 테이블:**
```
기존 필드:
- id: BIGINT (PK)
- column_id: BIGINT (FK)
- title: VARCHAR(255)
- description: TEXT
- position: INT
- bg_color: VARCHAR(20)
- priority: VARCHAR(20)
- assignee: VARCHAR(100)
- due_date: DATE
- is_completed: BOOLEAN
- created_at: TIMESTAMP
- updated_at: TIMESTAMP

✅ 추가 필드:
- parent_card_id: BIGINT (FK → card.id, NULLABLE)

인덱스:
- idx_card_parent (parent_card_id)
```

### Entity 정의

**Card Entity:**
```
필드 추가:
- parentCard: Card (ManyToOne, LAZY, nullable=true)
- childCards: List<Card> (OneToMany, mappedBy="parentCard")
```

**관계 매핑:**
- Self-Referential 관계
- Cascade: NONE (부모 삭제 시 자식 삭제 안 함)
- OrphanRemoval: false

### DTO 확장

**CardResponse:**
```
필드 추가:
- parentCardId: Long (nullable)
- parentCard: ParentCardSummaryDTO (nullable)
- childCards: List<ChildCardSummaryDTO> (nullable)

ParentCardSummaryDTO:
- id: Long
- title: String
- bgColor: String
- priority: String
- assignee: String

ChildCardSummaryDTO:
- id: Long
- title: String
- bgColor: String
- priority: String
- isCompleted: Boolean
```

**CreateCardRequest:**
```
필드 추가:
- parentCardId: Long (nullable)
```

### API 엔드포인트

#### 카드 조회 (관계 정보 포함)

**요청:**
```
GET /api/workspaces/{workspaceId}/boards/{boardId}/columns/{columnId}/cards/{cardId}?includeRelations=true
```

**응답:**
```
200 OK
{
  "id": 123,
  "columnId": 45,
  "title": "카드 제목",
  ...
  "parentCardId": 100,
  "parentCard": {
    "id": 100,
    "title": "부모 카드",
    "priority": "HIGH",
    "assignee": "홍길동"
  },
  "childCards": [
    {
      "id": 124,
      "title": "하위 카드 1",
      "isCompleted": true,
      "priority": "MEDIUM"
    }
  ]
}
```

#### 하위 카드 생성

**요청:**
```
POST /api/workspaces/{workspaceId}/boards/{boardId}/columns/{columnId}/cards

{
  "title": "하위 카드 제목",
  "description": "설명",
  "parentCardId": 100,
  ...
}
```

**응답:**
```
201 Created
{
  "id": 125,
  "parentCardId": 100,
  ...
}
```

**에러:**
```
400 Bad Request - 부모 카드가 이미 자식 카드인 경우
{
  "error": "HIERARCHY_LIMIT_EXCEEDED",
  "message": "1단계 계층 구조만 지원됩니다"
}

403 Forbidden - 부모 카드 접근 권한 없음
{
  "error": "FORBIDDEN",
  "message": "부모 카드에 대한 권한이 없습니다"
}
```

### Repository 확장

**새 쿼리 메서드:**
```
- findByParentCardId(Long parentCardId): List<Card>
  → 특정 부모 카드의 자식 카드 목록 조회

- countByParentCardId(Long parentCardId): int
  → 자식 카드 개수 조회

- findByIdWithRelations(Long cardId): Optional<Card>
  → JOIN FETCH로 부모/자식 정보 포함 조회
```

### Service 로직

#### 카드 생성 (createCard)

**플로우:**
1. parentCardId가 있으면 부모 카드 조회
2. 부모 카드의 parentCardId가 null인지 검증 (1단계 제한)
3. 부모 카드와 새 카드의 보드가 동일한지 검증
4. 권한 검증 (부모 카드 보드 편집 권한)
5. 카드 생성 및 저장
6. 활동 기록

**검증 규칙:**
- parentCardId가 null이 아니면 부모 카드가 존재해야 함
- 부모 카드의 parentCardId는 null이어야 함 (1단계 제한)
- 부모 카드와 새 카드의 column.board가 동일해야 함

#### 카드 조회 (getCard)

**플로우:**
1. 카드 기본 정보 조회
2. includeRelations 파라미터가 true이면:
   - parentCard가 있으면 요약 정보 조회
   - childCards가 있으면 목록 조회
3. CardResponse 생성 시 관계 정보 포함

#### 카드 삭제 (deleteCard)

**플로우:**
1. 카드 조회
2. 트랜잭션 시작
3. 자식 카드들의 parentCard를 null로 업데이트
4. 카드 삭제
5. position 업데이트
6. 활동 기록
7. 트랜잭션 커밋

#### 카드 이동 (updateCard - columnId 변경)

**플로우:**
1. columnId 변경 감지
2. parentCard가 null이 아니면:
   - parentCard를 null로 설정
   - 활동 기록에 "부모 관계 해제" 추가
3. 기존 이동 로직 수행

## 7. 보안 처리

### 권한 검증

**하위 카드 생성 시:**
- 부모 카드가 속한 보드의 EDITOR 이상 권한 확인
- 부모 카드와 새 카드의 보드가 동일한지 검증

**카드 네비게이션 시:**
- 부모/자식 카드 조회 시 해당 보드 접근 권한 확인

### 데이터 무결성

**계층 제한:**
- 부모 카드가 이미 자식인 경우 생성 차단
- DB 제약조건으로 순환 참조 방지 (1단계 제한)

**트랜잭션:**
- 부모 카드 삭제 시 자식 승격 작업을 단일 트랜잭션으로 처리

## 8. 수용 기준

### 기능 검증
- [ ] 카드 상세 모달에서 "하위 카드 생성" 버튼 클릭 시 CreateCardModal이 열리고 부모 카드가 자동 설정됨
- [ ] 자식 카드 모달에서 부모 카드 링크 클릭 시 모달 데이터가 부모 카드로 전환됨
- [ ] 부모 카드 모달에서 자식 카드 클릭 시 모달 데이터가 자식 카드로 전환됨
- [ ] 부모 카드 삭제 시 자식 카드가 최상위로 승격됨
- [ ] 자식 카드를 다른 칼럼으로 이동 시 부모 관계가 자동 해제됨
- [ ] 카드 아이템에 자식 개수가 표시됨 (예: "📎 3")
- [ ] 이미 자식인 카드를 부모로 설정 시 에러 메시지 표시됨

### UX 검증
- [ ] 부모/자식 카드 링크에 호버 시 배경색이 변경됨
- [ ] 카드 네비게이션 시 300ms 페이드 전환 애니메이션이 적용됨
- [ ] 데이터 로딩 중 스피너가 표시됨
- [ ] 자식 카드가 없을 때 빈 상태 메시지가 표시됨
- [ ] CreateCardModal에서 부모 카드 제목이 컨텍스트로 표시됨
- [ ] Tab 키로 부모/자식 카드 링크 포커스 이동 가능함
- [ ] Enter/Space 키로 네비게이션 가능함

### 성능 검증
- [ ] 카드 네비게이션 시 데이터 로딩 시간이 500ms 이내임
- [ ] 자식 카드가 많은 경우(20개 이상) 렌더링 성능이 정상임

### 보안 검증
- [ ] 부모 카드와 자식 카드가 다른 보드인 경우 생성이 차단됨
- [ ] 편집 권한이 없는 보드의 카드를 부모로 설정 시 403 에러 반환됨

## 9. 구현 순서 (순차적 개발 계획)

### Phase 1: 백엔드 데이터 모델 구축
- [ ] Card Entity에 parentCard 필드 추가 (ManyToOne, nullable)
- [ ] Card Entity에 childCards 필드 추가 (OneToMany)
- [ ] 데이터베이스 마이그레이션 스크립트 작성 (parent_card_id 컬럼 추가)
- [ ] 테스트: 부모-자식 관계 저장 및 조회 검증

### Phase 2: 백엔드 DTO 및 Repository 확장
- [ ] ParentCardSummaryDTO 생성
- [ ] ChildCardSummaryDTO 생성
- [ ] CardResponse에 parentCard, childCards 필드 추가
- [ ] CreateCardRequest에 parentCardId 필드 추가
- [ ] CardRepository에 쿼리 메서드 추가 (findByParentCardId, countByParentCardId, findByIdWithRelations)
- [ ] 테스트: Repository 쿼리 메서드 단위 테스트

### Phase 3: 백엔드 서비스 로직 구현
- [ ] CardService.createCard()에 parentCardId 처리 로직 추가
- [ ] 계층 제한 검증 로직 구현 (부모 카드가 이미 자식인지 확인)
- [ ] 부모 카드와 동일 보드 검증 로직 구현
- [ ] CardService.getCard()에 관계 정보 포함 로직 추가
- [ ] CardService.deleteCard()에 자식 승격 로직 추가
- [ ] CardService.updateCard()에 칼럼 이동 시 관계 해제 로직 추가
- [ ] 테스트: 서비스 로직 통합 테스트 (생성, 조회, 삭제, 이동)

### Phase 4: 백엔드 API 엔드포인트 구현
- [ ] CardController에 includeRelations 쿼리 파라미터 추가
- [ ] 하위 카드 생성 요청 처리 (parentCardId 포함)
- [ ] 에러 응답 추가 (HIERARCHY_LIMIT_EXCEEDED, FORBIDDEN)
- [ ] 권한 검증 로직 통합 (BoardMemberRoleValidator)
- [ ] 테스트: API 통합 테스트 (Postman 또는 RestAssured)

### Phase 5: 프론트엔드 타입 정의 확장
- [ ] Card 인터페이스에 parentCardId, parentCard, childCards 추가
- [ ] ParentCardSummary 인터페이스 생성
- [ ] ChildCardSummary 인터페이스 생성
- [ ] CreateCardRequest에 parentCardId 추가

### Phase 6: 프론트엔드 API 서비스 확장
- [ ] CardService.getCardWithRelations() 메서드 구현
- [ ] CardService.createChildCard() 메서드 구현
- [ ] CardContext에 새 메서드 통합
- [ ] 테스트: API 호출 및 응답 검증

### Phase 7: 프론트엔드 UI 컴포넌트 구현
- [ ] ParentCardLink 컴포넌트 생성
- [ ] ChildCardList 컴포넌트 생성
- [ ] ChildCardItem 서브 컴포넌트 생성
- [ ] 스타일링 적용 (Pastel 테마)
- [ ] 테스트: 컴포넌트 단위 테스트

### Phase 8: EditCardModal 리팩토링
- [ ] Props를 card → cardId로 변경
- [ ] 내부 상태 추가 (currentCardId, cardData, isLoadingCard)
- [ ] useEffect로 cardId 변경 감지 및 데이터 재조회
- [ ] navigateToCard() 함수 구현
- [ ] ParentCardLink 컴포넌트 통합 (자식 카드인 경우)
- [ ] ChildCardList 컴포넌트 통합 (자식이 있는 경우)
- [ ] 페이드 인/아웃 애니메이션 추가
- [ ] 로딩 스피너 표시
- [ ] 테스트: 모달 네비게이션 E2E 테스트

### Phase 9: CreateCardModal 확장
- [ ] Props에 parentCardId, parentCardTitle 추가
- [ ] 상단 컨텍스트 표시 ("부모 카드의 하위 카드 생성")
- [ ] 카드 생성 요청 시 parentCardId 포함
- [ ] 테스트: 하위 카드 생성 플로우 검증

### Phase 10: CardItem 확장
- [ ] childCount prop 추가
- [ ] 자식 개수 표시 UI 구현 ("📎 3")
- [ ] 스타일링 적용
- [ ] 테스트: 자식 개수 표시 검증

### Phase 11: 통합 테스트 및 버그 수정
- [ ] 전체 플로우 E2E 테스트 (Playwright)
- [ ] 에지 케이스 테스트 (부모 삭제, 자식 이동, 권한 없음)
- [ ] 성능 테스트 (많은 자식 카드 렌더링)
- [ ] 버그 수정 및 리팩토링

### Phase 12: 문서화 및 배포 준비
- [ ] API 문서 업데이트 (Swagger)
- [ ] 사용자 가이드 작성
- [ ] 변경 사항 CHANGELOG 작성
- [ ] 코드 리뷰 및 최종 검증

## 10. 위험 요소 및 완화 전략

| 위험 요소 | 영향도 | 완화 전략 |
|-----------|--------|-----------|
| 자식 카드가 많은 경우(100개 이상) 렌더링 성능 저하 | 중 | 페이지네이션 또는 가상 스크롤 적용, 초기 20개만 표시 후 "더 보기" 버튼 |
| 모달 네비게이션 중 데이터 로딩 지연 | 중 | 낙관적 업데이트, 스켈레톤 UI, 캐싱 전략 적용 |
| 부모 카드 삭제 시 트랜잭션 실패로 인한 데이터 불일치 | 높음 | @Transactional 적용, 실패 시 롤백 보장, 에러 로그 모니터링 |
| 순환 참조 발생 가능성 | 낮음 | 1단계 제한으로 근본적으로 차단, 서비스 레벨 검증 강화 |
| 자식 카드 이동 시 부모 관계 해제 누락 | 중 | 칼럼 변경 감지 로직 강화, 통합 테스트로 검증 |
| 권한 검증 누락으로 인한 보안 취약점 | 높음 | BoardMemberRoleValidator 통합, API 레벨 권한 체크, 보안 테스트 |
| 모달 애니메이션 중 사용자 클릭으로 인한 동시성 문제 | 중 | 애니메이션 중 버튼 비활성화, 디바운싱 적용 |
| 백엔드 API 응답에 관계 정보 누락 | 중 | includeRelations 파라미터 검증, 스키마 검증 테스트 |

## 11. 테스트 전략

### 백엔드 단위 테스트
- CardRepository 쿼리 메서드 테스트
- CardService 계층 제한 검증 로직 테스트
- CardService 자식 승격 로직 테스트
- CardService 관계 해제 로직 테스트

### 백엔드 통합 테스트
- 하위 카드 생성 API 테스트 (성공, 계층 제한 에러, 권한 에러)
- 카드 조회 API 테스트 (관계 정보 포함)
- 부모 카드 삭제 시 자식 승격 테스트
- 자식 카드 이동 시 관계 해제 테스트

### 프론트엔드 컴포넌트 테스트
- ParentCardLink 렌더링 및 클릭 테스트
- ChildCardList 렌더링 및 클릭 테스트
- EditCardModal 네비게이션 테스트
- CreateCardModal 부모 카드 컨텍스트 테스트
- CardItem 자식 개수 표시 테스트

### E2E 테스트 (Playwright)
- 하위 카드 생성 플로우 (시나리오 1)
- 자식 카드로 네비게이션 플로우 (시나리오 2)
- 부모 카드로 네비게이션 플로우 (시나리오 3)
- 부모 카드 삭제 플로우 (시나리오 4)
- 자식 카드 이동 플로우 (시나리오 5)

### 성능 테스트
- 자식 카드 50개 렌더링 성능 측정
- 카드 네비게이션 응답 시간 측정 (목표: < 500ms)

### 보안 테스트
- 다른 보드의 카드를 부모로 설정 시도 (차단 확인)
- 권한 없는 사용자의 하위 카드 생성 시도 (차단 확인)

## 12. Notes

- 현재 구현은 1단계 계층 구조만 지원하지만, 향후 2단계 이상 확장 시 데이터 모델 변경 없이 서비스 로직만 수정하면 됨
- 자식 카드가 매우 많은 경우(100개 이상) 초기 20개만 표시하고 "더 보기" 버튼으로 추가 로딩하는 방식을 고려해야 함
- 모달 네비게이션 히스토리(뒤로가기 버튼) 구현은 사용자 피드백 후 결정
- 자식 카드 목록의 정렬 기준은 기본적으로 생성일 오름차순, 향후 우선순위 정렬 옵션 추가 고려
- 부모 카드 삭제 시 "자식 카드가 승격됩니다" 경고 메시지는 UX 개선 단계에서 추가 검토

## 13. Related Documents

- `docs/requirements/Priority-2/card-hierarchy-support.md` - 요구사항 문서
- `backend/src/main/java/com/kanban/card/Card.java` - Card Entity
- `frontend/src/components/EditCardModal.tsx` - 카드 상세 모달
- `frontend/src/components/CreateCardModal.tsx` - 카드 생성 모달
- `frontend/src/types/card.ts` - 카드 타입 정의
