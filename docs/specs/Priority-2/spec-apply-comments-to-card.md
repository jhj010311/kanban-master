# spec-apply-comments-to-card — 카드 댓글 기능

## 1. 개요

칸반 보드의 각 카드에 댓글 기능을 추가하여 팀원 간 협업과 의사소통을 지원한다.

- 카드 상세 모달을 2-column 레이아웃으로 재설계하여 우측에 댓글 섹션을 배치한다.
- 댓글 입력 시 RichTextEditor를 사용하여 카드 설명과 일관된 편집 경험을 제공한다.
- 댓글 작성, 조회, 수정, 삭제 기능을 제공하며, 권한 기반 접근 제어를 적용한다.
- Soft delete 방식으로 삭제된 댓글을 관리하며, GDPR 요구사항에 대응한다.
- 카드 목록에 댓글 개수 뱃지를 표시하여 활발한 논의를 시각적으로 표시한다.
- XSS 공격을 방지하기 위해 서버/클라이언트 양측에서 HTML sanitization을 수행한다.
- Activity 시스템과 연동하여 댓글 작성/삭제 이벤트를 기록한다.
- 실시간 업데이트는 Phase 1에서 제외하고 추후 구현한다.

## 2. 연계 요구사항

- FR-06a 댓글 작성
- FR-06b 댓글 목록 조회
- FR-06c 댓글 수정
- FR-06d 댓글 삭제
- FR-06e 작성자 정보 표시
- FR-06f 페이지네이션
- FR-06g 빈 댓글 방지
- FR-06h Activity 로그
- FR-06i XSS 방지
- FR-06j 카드 상세 모달 UI 통합
- FR-06k RichTextEditor 통합
- FR-06l 댓글 개수 표시
- FR-06m 수정 이력 표시
- FR-06n Soft Delete
- NFR-06a 성능 (댓글 조회 < 300ms, 작성/수정/삭제 < 500ms)
- NFR-06b 보안 (XSS 방지, 권한 기반 제어)
- NFR-06c 데이터 제한 (최대 10,000자)

## 3. 주요 사용자 시나리오

### 시나리오 1: 댓글 작성 (RichTextEditor 사용)
1. 사용자가 카드를 클릭하여 EditCardModal을 연다.
2. 모달이 2-column 레이아웃으로 표시되며, 우측에 댓글 섹션이 보인다.
3. 댓글 입력 영역(RichTextEditor)에 포커스를 이동한다.
4. 툴바에서 "굵기" 버튼을 클릭하고 "중요:"라고 입력한 후, 일반 텍스트로 "이 작업 내일까지 완료할 수 있을까요?"를 입력한다.
5. "게시" 버튼을 클릭한다.
6. 댓글이 HTML 포맷으로 저장되고 즉시 목록 상단에 추가된다.
7. 입력창이 초기화되고 Activity 로그에 "COMMENT_ADDED" 이벤트가 기록된다.

### 시나리오 2: 댓글 목록 조회
1. 사용자가 댓글이 5개 있는 카드를 클릭한다.
2. EditCardModal이 열리고 우측 댓글 섹션에 최신순으로 댓글이 표시된다.
3. 각 댓글에는 작성자 아바타, 이름, 작성 시간, 내용이 표시된다.
4. 본인이 작성한 댓글에는 [편집] [삭제] 버튼이 표시된다.
5. 다른 사용자의 댓글에는 버튼이 표시되지 않는다 (OWNER 제외).

### 시나리오 3: 댓글 수정 및 이력 표시
1. 사용자가 본인이 작성한 댓글의 [편집] 버튼을 클릭한다.
2. 댓글 내용이 RichTextEditor로 전환되고 기존 HTML 내용이 표시된다.
3. 내용을 "내일까지 완료 가능합니다!"로 수정한다.
4. [저장] 버튼을 클릭한다.
5. 댓글이 즉시 업데이트되고 읽기 모드로 전환된다.
6. updatedAt 시간이 갱신되고 "(수정됨)" 표시가 나타난다.

### 시나리오 4: 댓글 삭제 (Soft Delete)
1. 사용자가 본인이 작성한 댓글의 [삭제] 버튼을 클릭한다.
2. "댓글을 삭제하시겠습니까?" 확인 다이얼로그가 표시된다.
3. "확인"을 클릭한다.
4. 백엔드에서 isDeleted 플래그가 true로 설정된다.
5. 댓글이 UI 목록에서 즉시 제거된다 (숨김 처리).
6. Activity 로그에 "COMMENT_DELETED" 이벤트가 기록된다.
7. 성공 토스트 메시지가 표시된다.

### 시나리오 5: 보드 OWNER가 다른 사람의 댓글 삭제
1. 보드 OWNER가 카드를 열고 다른 멤버의 댓글을 확인한다.
2. 해당 댓글의 [삭제] 버튼이 표시된다 (OWNER 권한).
3. [삭제] 버튼을 클릭하고 확인한다.
4. 댓글이 삭제되고 Activity 로그가 기록된다.

### 시나리오 6: 페이지네이션
1. 사용자가 댓글이 25개 있는 카드를 연다.
2. 처음에는 최신 20개의 댓글만 표시된다.
3. 스크롤을 내려 "더 보기" 버튼을 클릭한다.
4. 다음 5개의 댓글이 로드되어 목록에 추가된다.

### 시나리오 7: XSS 공격 시도
1. 악의적인 사용자가 댓글에 `<script>alert('XSS')</script>`를 입력한다.
2. "게시" 버튼을 클릭한다.
3. 백엔드에서 HTML sanitization이 수행되어 스크립트가 제거된다.
4. DB에는 안전한 텍스트만 저장된다.
5. 다른 사용자가 해당 카드를 열어도 스크립트가 실행되지 않는다.

### 시나리오 8: 카드 목록에서 댓글 개수 표시
1. 사용자가 보드 페이지에서 카드 목록을 확인한다.
2. 각 카드 하단에 댓글 개수 뱃지가 표시된다 (예: 💬 5).
3. 댓글이 없는 카드에는 뱃지가 표시되지 않는다.
4. 댓글이 있는 카드를 클릭하면 모달이 열리고 우측에 댓글 섹션이 보인다.

### 시나리오 9: 모바일에서 댓글 작성
1. 사용자가 모바일 기기(화면 너비 < 1024px)에서 카드를 연다.
2. 모달이 단일 컬럼으로 표시되고, 댓글 섹션이 카드 정보 아래에 배치된다.
3. RichTextEditor의 간소화된 툴바가 표시된다.
4. 댓글을 작성하고 게시한다.
5. 정상적으로 등록되고 UI에 반영된다.

## 4. 디자인 가이드라인

### 카드 상세 모달 2-Column 레이아웃

**Desktop (≥1024px):**
```
┌─────────────────────────────────────────────────────────────────┐
│  카드 수정                                                  [X]  │
├──────────────────────────────┬──────────────────────────────────┤
│                              │                                  │
│  왼쪽: 카드 정보 (60%)       │  우측: 댓글 섹션 (40%)           │
│  ──────────────────          │  ──────────────────              │
│                              │                                  │
│  • 카드 제목                 │  💬 댓글 (5)                     │
│  • 설명 (RichTextEditor)     │                                  │
│  • 우선순위                  │  ┌────────────────────────┐      │
│  • 담당자                    │  │ 댓글을 입력하세요...   │      │
│  • 마감일                    │  │                        │      │
│  • 라벨                      │  └────────────────────────┘      │
│  • 상세 정보 (접기)          │  [게시]                          │
│                              │                                  │
│                              │  ─────────────────────           │
│                              │                                  │
│                              │  [댓글 목록 - 독립 스크롤]       │
│                              │                                  │
│                              │  👤 김철수                       │
│                              │     좋은 아이디어네요!           │
│                              │     2시간 전    [편집] [삭제]    │
│                              │                                  │
│  [수정] [취소]               │  👤 이영희                       │
│                              │     동의합니다.                  │
│                              │     1일 전                       │
│                              │                                  │
│                              │  [더 보기]                       │
│                              │                                  │
└──────────────────────────────┴──────────────────────────────────┘
```

**Mobile (<1024px):**
```
┌─────────────────────────────────┐
│  카드 수정                  [X] │
├─────────────────────────────────┤
│                                 │
│  • 카드 제목                    │
│  • 설명                         │
│  • 우선순위                     │
│  • 담당자                       │
│  • 마감일                       │
│  • 라벨                         │
│                                 │
│  [수정] [취소]                  │
│                                 │
├─────────────────────────────────┤
│                                 │
│  💬 댓글 (5)                    │
│                                 │
│  ┌───────────────────────────┐ │
│  │ 댓글을 입력하세요...      │ │
│  └───────────────────────────┘ │
│  [게시]                         │
│                                 │
│  ─────────────────────          │
│                                 │
│  👤 김철수                      │
│     좋은 아이디어네요!          │
│     2시간 전    [편집] [삭제]   │
│                                 │
│  👤 이영희                      │
│     동의합니다.                 │
│     1일 전                      │
│                                 │
└─────────────────────────────────┘
```

### 댓글 섹션 상세 UI

**댓글 입력 영역:**
```
┌────────────────────────────────────────┐
│ 💬 댓글 (5)                            │
├────────────────────────────────────────┤
│ ┌────────────────────────────────────┐ │
│ │ 댓글을 입력하세요 (최대 10,000자) │ │
│ │                                    │ │
│ │                                    │ │
│ └────────────────────────────────────┘ │
│ [게시]  [취소]           1,234 / 10,000 │
└────────────────────────────────────────┘
```

**댓글 아이템 (본인 댓글):**
```
┌────────────────────────────────────────┐
│ 👤 김철수 (kcs@example.com)            │
│    좋은 아이디어네요! 이 방법으로       │
│    진행하면 훨씬 효율적일 것 같습니다.  │
│                                         │
│    2시간 전              [편집] [삭제]  │
└────────────────────────────────────────┘
```

**댓글 아이템 (다른 사람 댓글):**
```
┌────────────────────────────────────────┐
│ 👤 이영희 (lyh@example.com)            │
│    동의합니다.                          │
│                                         │
│    1일 전                               │
└────────────────────────────────────────┘
```

**댓글 편집 모드:**
```
┌────────────────────────────────────────┐
│ 👤 김철수 (kcs@example.com)            │
│ ┌────────────────────────────────────┐ │
│ │ 좋은 아이디어네요! 이 방법으로     │ │
│ │ 진행하면 훨씬 효율적일 것 같습니다.│ │
│ │                                    │ │
│ └────────────────────────────────────┘ │
│ [저장]  [취소]           124 / 10,000   │
└────────────────────────────────────────┘
```

**빈 상태:**
```
┌────────────────────────────────────────┐
│ 💬 댓글 (0)                            │
├────────────────────────────────────────┤
│ ┌────────────────────────────────────┐ │
│ │ 댓글을 입력하세요...               │ │
│ └────────────────────────────────────┘ │
│ [게시]                                  │
│                                         │
│ ─────────────────────                  │
│                                         │
│        💬                               │
│   아직 댓글이 없습니다                  │
│   첫 댓글을 작성해보세요!               │
│                                         │
└────────────────────────────────────────┘
```

### Pastel 디자인 테마 적용

**댓글 섹션 스타일:**
- 배경: `bg-pastel-blue-50/30`
- 테두리: `border-l border-pastel-blue-200` (좌측만)
- 제목: `text-lg font-semibold text-pastel-blue-900`

**댓글 입력 영역 (RichTextEditor):**
- Editor 배경: `bg-white`
- Editor 테두리: `border border-pastel-blue-200`
- Editor 포커스: `focus:ring-2 focus:ring-pastel-blue-300`
- 툴바: 카드 설명 편집기와 동일한 Quill 스타일 적용
- 게시 버튼: `bg-pastel-blue-500 hover:bg-pastel-blue-600 text-white`

**댓글 아이템:**
- 배경: `bg-white`
- 테두리: `border border-pastel-blue-100`
- 본인 댓글 강조: `border-l-4 border-l-pastel-blue-400`
- 작성자 이름: `text-pastel-blue-900 font-semibold`
- 내용: `text-pastel-blue-700`
- 시간: `text-xs text-pastel-blue-500`

**버튼 스타일:**
- 편집: `text-pastel-blue-600 hover:text-pastel-blue-700`
- 삭제: `text-pastel-pink-600 hover:text-pastel-pink-700`

**카드 목록 댓글 개수 뱃지:**
- 배경: `bg-pastel-blue-100`
- 텍스트: `text-pastel-blue-700 text-xs`
- 아이콘: `💬` 또는 `<MessageCircle>` (lucide-react)
- 위치: 카드 하단, 라벨 영역 옆
- 표시 조건: 댓글 개수 > 0

### 에러 메시지 및 토스트

**에러 메시지:**
- 빈 댓글: "댓글 내용을 입력해주세요"
- 길이 초과: "댓글은 10,000자를 초과할 수 없습니다 (현재: {count}자)"
- 삭제 실패: "댓글 삭제에 실패했습니다. 다시 시도해주세요"
- 권한 없음: "댓글을 수정/삭제할 권한이 없습니다"

**토스트 스타일:**
- 성공: `bg-pastel-green-500 text-white`
- 에러: `bg-pastel-pink-500 text-white`
- 위치: 화면 우측 상단
- 자동 사라짐: 3초 후

## 5. 프론트엔드 규격

### 데이터 타입 정의

**frontend/src/types/comment.ts (신규 생성):**

```typescript
export interface Comment {
  id: number;
  cardId: number;
  authorId: number;
  authorName: string;
  authorEmail: string;
  authorAvatarUrl: string | null;
  content: string;  // HTML format (from RichTextEditor)
  createdAt: string;  // ISO 8601
  updatedAt: string;  // ISO 8601
}

export interface CreateCommentRequest {
  content: string;
}

export interface UpdateCommentRequest {
  content: string;
}

export interface CommentListResponse {
  content: Comment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

### 컴포넌트 구조

#### 1. CommentSection.tsx (신규 생성)

**역할:** 댓글 섹션 전체를 담당하는 컨테이너 컴포넌트

**Props:**
```typescript
interface CommentSectionProps {
  cardId: number;
  workspaceId: number;
  boardId: number;
  canComment: boolean;  // 댓글 작성 권한 (VIEWER 이상)
  isOwner: boolean;     // 보드 OWNER 여부
}
```

**상태:**
```typescript
const [comments, setComments] = useState<Comment[]>([]);
const [loading, setLoading] = useState<boolean>(false);
const [page, setPage] = useState<number>(0);
const [totalPages, setTotalPages] = useState<number>(0);
const [hasMore, setHasMore] = useState<boolean>(true);
```

**핵심 로직:**
- 댓글 목록 조회 (페이지네이션)
- 새 댓글 추가 시 목록 최상단에 삽입
- 댓글 수정/삭제 후 목록 업데이트
- 무한 스크롤 또는 "더 보기" 버튼

**렌더링:**
```typescript
<div className="flex flex-col h-full bg-pastel-blue-50/30 border-l border-pastel-blue-200">
  <CommentInput onSubmit={handleCreateComment} disabled={!canComment} />
  <div className="flex-1 overflow-y-auto p-4 space-y-3">
    {loading && <CommentSkeleton />}
    {comments.length === 0 && <EmptyComments />}
    {comments.map(comment => (
      <CommentItem
        key={comment.id}
        comment={comment}
        isOwner={isOwner}
        onEdit={handleEditComment}
        onDelete={handleDeleteComment}
      />
    ))}
    {hasMore && <button onClick={loadMore}>더 보기</button>}
  </div>
</div>
```

#### 2. CommentInput.tsx (신규 생성)

**역할:** 댓글 입력 영역 (RichTextEditor 사용)

**Props:**
```typescript
interface CommentInputProps {
  onSubmit: (content: string) => Promise<void>;
  disabled?: boolean;
  initialValue?: string;  // 수정 모드용 (HTML)
  onCancel?: () => void;  // 수정 모드용
}
```

**상태:**
```typescript
const [content, setContent] = useState<string>(initialValue || '');
const [submitting, setSubmitting] = useState<boolean>(false);
const editorRef = useRef<ReactQuill>(null);
```

**핵심 로직:**
```typescript
const handleSubmit = async () => {
  // 빈 내용 체크 (HTML 태그 제거 후)
  const textContent = editorRef.current?.getEditor().getText() || '';
  if (!textContent.trim()) {
    showToast('댓글 내용을 입력해주세요', 'error');
    return;
  }

  if (textContent.length > 10000) {
    showToast(`댓글은 10,000자를 초과할 수 없습니다 (현재: ${textContent.length}자)`, 'error');
    return;
  }

  setSubmitting(true);
  try {
    await onSubmit(content);  // HTML 포맷으로 전송
    setContent('');
    editorRef.current?.focus();
  } catch (error) {
    showToast('댓글 작성에 실패했습니다', 'error');
  } finally {
    setSubmitting(false);
  }
};
```

**렌더링:**
```typescript
<div className="p-4 bg-white border-b border-pastel-blue-200">
  <RichTextEditor
    ref={editorRef}
    value={content}
    onChange={setContent}
    placeholder="댓글을 입력하세요 (최대 10,000자)"
    disabled={disabled || submitting}
    maxLength={10000}
    className="min-h-[120px]"
  />
  <div className="flex items-center justify-between mt-2">
    <span className="text-xs text-pastel-blue-500">
      {editorRef.current?.getEditor().getText().length || 0} / 10,000
    </span>
    <div className="space-x-2">
      {onCancel && (
        <button onClick={onCancel} disabled={submitting} className="text-pastel-blue-600">
          취소
        </button>
      )}
      <button
        onClick={handleSubmit}
        disabled={disabled || submitting || !content.trim()}
        className="bg-pastel-blue-500 text-white px-4 py-2 rounded hover:bg-pastel-blue-600"
      >
        게시
      </button>
    </div>
  </div>
</div>
```

**Note:** RichTextEditor는 기존 `card-description-html-editor` 스펙에서 정의한 컴포넌트를 재사용한다.

#### 3. CommentItem.tsx (신규 생성)

**역할:** 개별 댓글 아이템

**Props:**
```typescript
interface CommentItemProps {
  comment: Comment;
  isOwner: boolean;
  onEdit: (commentId: number, newContent: string) => Promise<void>;
  onDelete: (commentId: number) => Promise<void>;
}
```

**상태:**
```typescript
const [isEditing, setIsEditing] = useState<boolean>(false);
const [editContent, setEditContent] = useState<string>(comment.content);
const currentUserId = useAuthContext().user?.id;
const isAuthor = currentUserId === comment.authorId;
const canEdit = isAuthor;
const canDelete = isAuthor || isOwner;
```

**핵심 로직:**
```typescript
const handleEdit = async () => {
  if (!editContent.trim()) {
    showToast('댓글 내용을 입력해주세요', 'error');
    return;
  }

  try {
    await onEdit(comment.id, editContent);
    setIsEditing(false);
    showToast('댓글이 수정되었습니다', 'success');
  } catch (error) {
    showToast('댓글 수정에 실패했습니다', 'error');
  }
};

const handleDelete = async () => {
  if (!confirm('댓글을 삭제하시겠습니까?')) {
    return;
  }

  try {
    await onDelete(comment.id);
    showToast('댓글이 삭제되었습니다', 'success');
  } catch (error) {
    showToast('댓글 삭제에 실패했습니다', 'error');
  }
};
```

**렌더링:**
```typescript
<div className={`p-4 bg-white border border-pastel-blue-100 rounded-lg ${isAuthor ? 'border-l-4 border-l-pastel-blue-400' : ''}`}>
  <div className="flex items-start space-x-3">
    <Avatar src={comment.authorAvatarUrl} name={comment.authorName} />
    <div className="flex-1">
      <div className="flex items-center justify-between">
        <div>
          <span className="font-semibold text-pastel-blue-900">{comment.authorName}</span>
          <span className="text-xs text-pastel-blue-500 ml-2">{comment.authorEmail}</span>
        </div>
        <span className="text-xs text-pastel-blue-500">{formatRelativeTime(comment.createdAt)}</span>
      </div>

      {isEditing ? (
        <CommentInput
          initialValue={editContent}
          onSubmit={handleEdit}
          onCancel={() => setIsEditing(false)}
        />
      ) : (
        <>
          <div
            className="mt-2 text-pastel-blue-700 prose prose-sm max-w-none"
            dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(comment.content) }}
          />
          {comment.updatedAt !== comment.createdAt && (
            <span className="text-xs text-pastel-blue-400 ml-2">
              (수정됨 · {formatRelativeTime(comment.updatedAt)})
            </span>
          )}
        </>
      )}

      {!isEditing && (
        <div className="mt-2 space-x-2">
          {canEdit && (
            <button onClick={() => setIsEditing(true)} className="text-pastel-blue-600">
              편집
            </button>
          )}
          {canDelete && (
            <button onClick={handleDelete} className="text-pastel-pink-600">
              삭제
            </button>
          )}
        </div>
      )}
    </div>
  </div>
</div>
```

#### 4. EditCardModal.tsx (수정)

**변경점:**
- 단일 컬럼 → 2-column 그리드 레이아웃
- 우측 컬럼에 `<CommentSection>` 추가
- 모달 최대 너비 확장 (`max-w-4xl` → `max-w-6xl`)

**레이아웃 구조:**
```typescript
<div className="fixed inset-0 z-50 overflow-y-auto">
  <div className="flex items-center justify-center min-h-screen p-4">
    <div className="bg-white rounded-lg shadow-xl max-w-6xl w-full max-h-[90vh] overflow-hidden">
      <div className="flex items-center justify-between p-6 border-b">
        <h2>카드 수정</h2>
        <button onClick={onClose}>X</button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[60%_40%] h-[calc(90vh-120px)]">
        {/* 왼쪽: 카드 정보 */}
        <div className="p-6 overflow-y-auto">
          {/* 기존 카드 편집 폼 */}
        </div>

        {/* 우측: 댓글 섹션 */}
        <CommentSection
          cardId={card.id}
          workspaceId={workspaceId}
          boardId={boardId}
          canComment={canEdit}
          isOwner={isOwner}
        />
      </div>

      <div className="flex justify-end space-x-2 p-6 border-t">
        <button onClick={onClose}>취소</button>
        <button onClick={handleSave}>수정</button>
      </div>
    </div>
  </div>
</div>
```

### API Service

**frontend/src/services/commentService.ts (신규 생성):**

```typescript
import { axiosInstance } from './axiosInstance';
import { Comment, CreateCommentRequest, UpdateCommentRequest, CommentListResponse } from '../types/comment';

const BASE_PATH = '/api/v1/workspaces';

export const commentService = {
  /**
   * 카드의 댓글 목록 조회 (페이지네이션)
   */
  listComments: async (
    workspaceId: number,
    boardId: number,
    cardId: number,
    page: number = 0,
    size: number = 20
  ): Promise<CommentListResponse> => {
    const response = await axiosInstance.get(
      `${BASE_PATH}/${workspaceId}/boards/${boardId}/cards/${cardId}/comments`,
      { params: { page, size } }
    );
    return response.data;
  },

  /**
   * 댓글 생성
   */
  createComment: async (
    workspaceId: number,
    boardId: number,
    cardId: number,
    request: CreateCommentRequest
  ): Promise<Comment> => {
    const response = await axiosInstance.post(
      `${BASE_PATH}/${workspaceId}/boards/${boardId}/cards/${cardId}/comments`,
      request
    );
    return response.data;
  },

  /**
   * 댓글 수정
   */
  updateComment: async (
    workspaceId: number,
    boardId: number,
    cardId: number,
    commentId: number,
    request: UpdateCommentRequest
  ): Promise<Comment> => {
    const response = await axiosInstance.put(
      `${BASE_PATH}/${workspaceId}/boards/${boardId}/cards/${cardId}/comments/${commentId}`,
      request
    );
    return response.data;
  },

  /**
   * 댓글 삭제
   */
  deleteComment: async (
    workspaceId: number,
    boardId: number,
    cardId: number,
    commentId: number
  ): Promise<void> => {
    await axiosInstance.delete(
      `${BASE_PATH}/${workspaceId}/boards/${boardId}/cards/${cardId}/comments/${commentId}`
    );
  },
};
```

### 상태 관리

**현재 상태 관리 방식 유지:**
- CommentSection 컴포넌트 내부에서 로컬 state로 댓글 목록 관리
- Context API 사용하지 않음 (필요 시 향후 추가)
- 댓글 CRUD 후 즉시 로컬 state 업데이트

### 에러 처리

**클라이언트 검증:**
```typescript
// 빈 댓글 방지
if (!content.trim()) {
  showToast('댓글 내용을 입력해주세요', 'error');
  return;
}

// 길이 제한
if (content.length > 10000) {
  showToast(`댓글은 10,000자를 초과할 수 없습니다 (현재: ${content.length}자)`, 'error');
  return;
}
```

**서버 에러 처리:**
```typescript
try {
  await commentService.createComment(workspaceId, boardId, cardId, { content });
  showToast('댓글이 작성되었습니다', 'success');
} catch (error: any) {
  const errorMessage = error.response?.data?.message || '댓글 작성에 실패했습니다';
  showToast(errorMessage, 'error');
}
```

## 6. 백엔드 규격

### 데이터베이스 스키마

**Comment Entity (신규 생성):**

```sql
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (card_id) REFERENCES cards(id),
    FOREIGN KEY (author_id) REFERENCES users(id),
    INDEX idx_card_id_not_deleted (card_id, is_deleted),
    INDEX idx_author_id (author_id),
    INDEX idx_created_at (created_at DESC)
);
```

**주요 필드:**
- `id`: 댓글 고유 ID (BIGINT, AUTO_INCREMENT)
- `card_id`: 연결된 카드 ID (BIGINT, FK to cards.id, CASCADE 제거)
- `author_id`: 작성자 ID (BIGINT, FK to users.id, CASCADE 제거)
- `content`: 댓글 내용 (TEXT, NOT NULL, HTML 포맷)
- `is_deleted`: Soft delete 플래그 (BOOLEAN, DEFAULT FALSE)
- `created_at`: 생성 시간 (TIMESTAMP, 자동)
- `updated_at`: 수정 시간 (TIMESTAMP, 자동 업데이트)

**인덱스:**
- `idx_card_id_not_deleted`: 카드별 활성 댓글 조회 성능 향상 (복합 인덱스)
- `idx_author_id`: 작성자별 댓글 조회 (선택사항)
- `idx_created_at`: 최신순 정렬 성능 향상

**Soft Delete 정책:**
- 카드 삭제 시에도 댓글은 유지됨 (CASCADE 제거)
- 삭제된 댓글은 `is_deleted = true`로 설정
- 조회 시 `WHERE is_deleted = false` 조건 필수

### Entity 클래스

**Comment.java (신규 생성):**

```java
package com.kanban.entity;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // createdAt, updatedAt은 BaseEntity에서 상속
}
```

### DTO 클래스

**CreateCommentRequest.java (신규 생성):**

```java
package com.kanban.dto.comment;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCommentRequest {

    @NotBlank(message = "댓글 내용을 입력해주세요")
    @Size(max = 10000, message = "댓글은 10,000자를 초과할 수 없습니다")
    private String content;
}
```

**UpdateCommentRequest.java (신규 생성):**

```java
package com.kanban.dto.comment;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCommentRequest {

    @NotBlank(message = "댓글 내용을 입력해주세요")
    @Size(max = 10000, message = "댓글은 10,000자를 초과할 수 없습니다")
    private String content;
}
```

**CommentResponse.java (신규 생성):**

```java
package com.kanban.dto.comment;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private Long id;
    private Long cardId;
    private Long authorId;
    private String authorName;
    private String authorEmail;
    private String authorAvatarUrl;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .cardId(comment.getCard().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getName())
                .authorEmail(comment.getAuthor().getEmail())
                .authorAvatarUrl(comment.getAuthor().getAvatarUrl())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
```

### Repository

**CommentRepository.java (신규 생성):**

```java
package com.kanban.repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 카드의 삭제되지 않은 댓글 목록 조회 (페이지네이션, 최신순)
     */
    Page<Comment> findByCardIdAndIsDeletedFalseOrderByCreatedAtDesc(Long cardId, Pageable pageable);

    /**
     * 특정 댓글 조회 (카드 ID와 댓글 ID로, 삭제되지 않은 것만)
     */
    Optional<Comment> findByIdAndCardIdAndIsDeletedFalse(Long commentId, Long cardId);

    /**
     * 카드의 삭제되지 않은 댓글 개수 조회
     */
    long countByCardIdAndIsDeletedFalse(Long cardId);
}
```

### Service

**CommentService.java (신규 생성):**

**주요 메서드:**

1. **댓글 목록 조회** (`getComments`)
   - 카드 ID로 댓글 조회
   - 페이지네이션 적용 (최신순)
   - 작성자 정보 enrichment
   - 권한 검증 (보드 멤버만 조회 가능)

2. **댓글 생성** (`createComment`)
   - 보드 멤버 권한 검증 (VIEWER 이상)
   - HTML sanitization 적용
   - DB 저장
   - Activity 로그 기록 (COMMENT_ADDED)
   - 작성자 정보 enrichment 후 반환

3. **댓글 수정** (`updateComment`)
   - 댓글 작성자 본인 확인
   - HTML sanitization 적용
   - DB 업데이트
   - updatedAt 자동 갱신
   - 작성자 정보 enrichment 후 반환

4. **댓글 삭제** (`deleteComment`)
   - 권한 검증 (작성자 또는 보드 OWNER)
   - Soft delete 처리 (isDeleted = true 설정)
   - Activity 로그 기록 (COMMENT_DELETED)

**서비스 로직 흐름:**

```
createComment:
  1. 카드 존재 확인
  2. 보드 멤버 권한 검증 (VIEWER 이상)
  3. content HTML sanitization
  4. Comment 엔티티 생성 및 저장
  5. Activity 로그 기록 (COMMENT_ADDED)
  6. CommentResponse 반환

updateComment:
  1. 댓글 존재 확인
  2. 작성자 본인 확인 (currentUserId == comment.authorId)
  3. content HTML sanitization
  4. 댓글 업데이트 (updatedAt 자동 갱신)
  5. CommentResponse 반환

deleteComment:
  1. 댓글 존재 확인 (삭제되지 않은 것만)
  2. 권한 확인 (작성자 본인 OR 보드 OWNER)
  3. Soft delete 처리 (comment.setIsDeleted(true))
  4. Activity 로그 기록 (COMMENT_DELETED)
```

**HTML Sanitization:**
- 기존 `HtmlSanitizerConfig`의 `PolicyFactory` 재사용
- 허용 태그: `<p>`, `<br>`, `<a>` (링크)
- 금지 태그: `<script>`, `<iframe>`, `<style>` 등

### Controller

**CommentController.java (신규 생성):**

**API 엔드포인트:**

```
1. GET /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments
   - 댓글 목록 조회 (페이지네이션)
   - Query Params: page (default 0), size (default 20)
   - Response: Page<CommentResponse>

2. POST /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments
   - 댓글 생성
   - Request Body: CreateCommentRequest
   - Response: CommentResponse

3. PUT /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments/{commentId}
   - 댓글 수정
   - Request Body: UpdateCommentRequest
   - Response: CommentResponse

4. DELETE /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments/{commentId}
   - 댓글 삭제
   - Response: 204 No Content
```

**권한 검증:**
- 조회: 보드 멤버 (VIEWER 이상)
- 생성: 보드 멤버 (VIEWER 이상)
- 수정: 댓글 작성자 본인만
- 삭제: 댓글 작성자 또는 보드 OWNER

### Activity 로그 연동

**ActivityService 호출:**

```java
// 댓글 생성 시
activityService.recordActivity(
    ActivityScopeType.CARD,
    cardId,
    ActivityEventType.COMMENT_ADDED,
    currentUserId,
    String.format("%s님이 댓글을 작성했습니다", currentUser.getName()),
    null
);

// 댓글 삭제 시
activityService.recordActivity(
    ActivityScopeType.CARD,
    cardId,
    ActivityEventType.COMMENT_DELETED,
    currentUserId,
    String.format("%s님이 댓글을 삭제했습니다", currentUser.getName()),
    null
);
```

## 7. 보안 처리

### XSS 방지 전략

#### 다층 방어 (Defense in Depth)

1. **프론트엔드 (렌더링 시)**
   - React의 기본 XSS 방어 메커니즘 활용 (자동 escaping)
   - `dangerouslySetInnerHTML` 사용 시 DOMPurify로 sanitization

2. **백엔드 (저장 시)**
   - OWASP Java HTML Sanitizer로 HTML 정제
   - 허용 태그: `<p>`, `<br>`, `<a>`
   - 위험한 태그/속성 제거
   - DB에 안전한 HTML만 저장

3. **데이터베이스**
   - SQL Injection 방지 (JPA 사용으로 자동 방어)
   - Prepared Statement 사용

### HTML Sanitization 정책

**서버 (OWASP HTML Sanitizer):**

```java
// 기존 HtmlSanitizerConfig의 PolicyFactory 재사용
PolicyFactory policy = new HtmlPolicyBuilder()
    .allowElements("p", "br")
    .allowElements("a")
    .allowAttributes("href", "target", "rel").onElements("a")
    .requireRelNofollowOnLinks()
    .toFactory();
```

**허용 태그:**
- `<p>`, `<br>`: 단락 및 줄바꿈
- `<a href="..." target="_blank" rel="noopener noreferrer">`: 링크

**금지 태그:**
- `<script>`, `<iframe>`, `<embed>`, `<object>`, `<style>`, `<img>`

### 권한 기반 접근 제어

**수정 권한:**
- 댓글 작성자 본인만 수정 가능
- `currentUserId == comment.getAuthor().getId()`

**삭제 권한:**
- 댓글 작성자 본인
- 또는 보드 OWNER
- `currentUserId == comment.getAuthor().getId() || isOwner`

**조회/생성 권한:**
- 보드 멤버 (VIEWER 이상)
- `roleValidator.validateRole(boardId, BoardMemberRole.VIEWER)`

### 보안 테스트 케이스

**테스트해야 할 XSS 공격 벡터:**

1. `<script>alert('XSS')</script>` → 제거됨
2. `<img src=x onerror=alert('XSS')>` → 제거됨
3. `<a href="javascript:alert('XSS')">링크</a>` → href 제거됨
4. `<iframe src="evil.com"></iframe>` → 제거됨
5. `<p onclick="alert('XSS')">텍스트</p>` → onclick 제거됨

**허용되어야 할 정상 HTML:**

1. `안녕하세요` → 허용
2. `첫 줄\n둘째 줄` → `첫 줄<br>둘째 줄`로 변환
3. `<a href="https://example.com" target="_blank">링크</a>` → 허용 (rel 속성 추가)
4. `<p>단락 텍스트</p>` → 허용

## 8. 수용 기준

1. 보드 멤버(VIEWER 이상)가 카드에 댓글을 작성할 수 있다.
2. 카드의 모든 댓글이 최신순으로 조회된다.
3. 댓글 작성자 본인만 자신의 댓글을 수정할 수 있다.
4. 댓글 작성자 또는 보드 OWNER가 댓글을 삭제할 수 있다.
5. 각 댓글에 작성자 이름, 이메일, 아바타, 작성/수정 시간이 표시된다.
6. 댓글이 20개 이상일 때 페이지네이션 또는 "더 보기" 버튼이 표시된다.
7. 빈 내용의 댓글은 작성할 수 없으며 에러 메시지가 표시된다.
8. 댓글 작성/삭제 시 Activity 로그에 이벤트가 기록된다.
9. `<script>`, `<iframe>` 등 위험한 태그가 포함된 댓글은 서버에서 제거된다.
10. 카드 상세 모달이 2-column 레이아웃으로 표시되며 우측에 댓글 섹션이 배치된다.
11. 화면 너비 < 1024px에서 단일 컬럼으로 전환되고 댓글이 카드 정보 아래에 표시된다.
12. 댓글 조회 < 300ms, 작성/수정/삭제 < 500ms 성능을 만족한다.
13. 모든 모던 브라우저(Chrome, Firefox, Safari, Edge)에서 정상 작동한다.

## 9. 구현 순서

### Phase 1: 백엔드 기반 구축 (2일)
- [ ] Comment Entity 생성 (`Comment.java`)
- [ ] CommentRepository 생성 (`CommentRepository.java`)
- [ ] DTO 클래스 생성 (`CreateCommentRequest`, `UpdateCommentRequest`, `CommentResponse`)
- [ ] CommentService 생성 (CRUD 로직, HTML sanitization, Activity 로그)
- [ ] CommentController 생성 (REST API 엔드포인트)
- [ ] Unit Tests (CommentService 테스트)
- [ ] Integration Tests (API 엔드포인트 테스트)

### Phase 2: 프론트엔드 타입 및 서비스 (1일)
- [ ] Types 정의 (`comment.ts`)
- [ ] Comment Service 생성 (`commentService.ts`)
- [ ] API 호출 로직 구현 및 테스트

### Phase 3: 프론트엔드 UI 컴포넌트 (2.5일)
- [ ] CommentInput 컴포넌트 생성
- [ ] CommentItem 컴포넌트 생성
- [ ] CommentSection 컴포넌트 생성
- [ ] EmptyComments, CommentSkeleton 컴포넌트 생성
- [ ] 컴포넌트 Unit Tests

### Phase 4: 카드 모달 통합 및 레이아웃 (1.5일)
- [ ] EditCardModal 2-column 레이아웃 변경
- [ ] CommentSection 통합
- [ ] 반응형 디자인 적용 (mobile < 1024px)
- [ ] Pastel 테마 스타일링
- [ ] 권한 기반 UI 표시 로직

### Phase 5: 테스트 및 검증 (1.5일)
- [ ] E2E 테스트 (Playwright)
- [ ] 브라우저 호환성 테스트
- [ ] 성능 테스트 (댓글 조회/작성 속도)
- [ ] 보안 테스트 (XSS 공격 시나리오)
- [ ] 접근성 테스트 (키보드 네비게이션)
- [ ] 버그 수정 및 최종 검증

**총 소요 시간: ~8.5일**

## 10. 위험 요소 및 완화 전략

| 위험 | 영향 | 완화 전략 |
|------|------|----------|
| XSS 공격 | 보안 취약점 | 서버/클라이언트 양측 HTML sanitization, 허용 태그 화이트리스트 |
| 대량 댓글 성능 저하 | 페이지 로딩 지연 | 페이지네이션 적용 (20개/페이지), 인덱스 최적화 |
| 모달 레이아웃 복잡도 증가 | 개발 난이도 상승 | 2-column 그리드 시스템 활용, 반응형 브레이크포인트 명확화 |
| 권한 검증 누락 | 보안 취약점 | Service layer에서 권한 검증 강제, 작성자 ID 비교 |
| 댓글 삭제 후 복구 문제 | 데이터 복구 요청 | Soft delete 정책으로 복구 가능성 보장, 삭제 전 확인 다이얼로그 표시 |
| 모바일 UX 저하 | 사용성 문제 | 단일 컬럼 전환, 터치 영역 충분히 확보, 스크롤 최적화 |
| Activity 로그 누락 | 추적 불가 | Service layer에서 Activity 기록 자동화, 트랜잭션 일관성 보장 |

## 11. 테스트 전략

### Unit Tests

**백엔드:**
- `CommentRepository`:
  - findByCardIdOrderByCreatedAtDesc 페이지네이션 테스트
  - findByIdAndCardId 조회 테스트
  - countByCardId 개수 조회 테스트
- `CommentService`:
  - createComment: 정상 생성, XSS 제거, 권한 검증 실패
  - updateComment: 정상 수정, 작성자 아닌 경우 실패, XSS 제거
  - deleteComment: 정상 삭제, 권한 없는 경우 실패, OWNER 삭제 성공
  - getComments: 페이지네이션, 빈 목록
- `CreateCommentRequest`: validation 테스트 (빈 내용, 10,000자 초과)

**프론트엔드:**
- `CommentInput`:
  - onSubmit 콜백 호출 테스트
  - 빈 내용 제출 시 에러 메시지
  - 길이 제한 테스트
  - disabled 상태 테스트
- `CommentItem`:
  - 본인 댓글 편집/삭제 버튼 표시
  - 다른 사람 댓글 버튼 숨김
  - 편집 모드 전환 테스트
  - 삭제 확인 다이얼로그
- `CommentSection`:
  - 댓글 목록 렌더링
  - 페이지네이션 동작
  - 빈 상태 표시
  - 로딩 상태 표시

### Integration Tests

**백엔드:**
```
1. GET /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments
   - 정상 조회 → 200 OK + Page<CommentResponse>
   - 페이지네이션 파라미터 → 올바른 페이지 반환
   - 권한 없음 → 403 Forbidden
   - 인증 실패 → 401 Unauthorized

2. POST /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments
   - 정상 생성 → 201 Created + CommentResponse
   - XSS 포함 → 200 OK + sanitized content
   - 빈 내용 → 400 Bad Request
   - 10,000자 초과 → 400 Bad Request
   - 권한 없음 → 403 Forbidden

3. PUT /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments/{commentId}
   - 정상 수정 → 200 OK + CommentResponse
   - 작성자 아님 → 403 Forbidden
   - XSS 포함 → 200 OK + sanitized

4. DELETE /api/v1/workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments/{commentId}
   - 작성자 삭제 → 204 No Content
   - OWNER 삭제 → 204 No Content
   - 권한 없음 → 403 Forbidden
```

**프론트엔드:**
```
1. 댓글 작성 플로우
   - CommentInput에 텍스트 입력 → API 호출 → 목록 업데이트 확인
2. 댓글 수정 플로우
   - 편집 버튼 클릭 → 내용 수정 → 저장 → UI 업데이트
3. 댓글 삭제 플로우
   - 삭제 버튼 클릭 → 확인 → 목록에서 제거
4. 페이지네이션
   - 더 보기 버튼 클릭 → 다음 페이지 로드 → 목록에 추가
```

### E2E Tests (Playwright)

```
시나리오 1: 댓글 작성 및 조회
  1. 로그인
  2. 보드 페이지로 이동
  3. 카드 클릭 → EditCardModal 열림
  4. 우측 댓글 섹션 확인
  5. 댓글 입력: "이 작업 내일까지 완료할 수 있을까요?"
  6. 게시 버튼 클릭
  7. 댓글이 목록 상단에 추가되는지 확인
  8. 작성자 이름, 시간 표시 확인

시나리오 2: 댓글 수정
  1. 본인이 작성한 댓글의 편집 버튼 클릭
  2. 내용 수정: "내일까지 완료 가능합니다!"
  3. 저장 버튼 클릭
  4. 댓글 내용 업데이트 확인
  5. "(수정됨)" 표시 확인

시나리오 3: 댓글 삭제
  1. 본인이 작성한 댓글의 삭제 버튼 클릭
  2. 확인 다이얼로그에서 "확인" 클릭
  3. 댓글이 목록에서 제거되는지 확인
  4. 성공 토스트 메시지 표시 확인

시나리오 4: XSS 공격 시도
  1. 댓글 입력: `<script>alert('XSS')</script>`
  2. 게시 버튼 클릭
  3. 댓글 조회 시 스크립트가 실행되지 않음 확인
  4. 댓글 내용에 `<script>` 태그가 없음 확인

시나리오 5: 모바일 반응형
  1. 화면 크기를 < 1024px로 조정
  2. 카드 클릭
  3. 단일 컬럼 레이아웃 확인
  4. 댓글 섹션이 카드 정보 아래에 있는지 확인
  5. 댓글 작성/수정/삭제 정상 작동 확인
```

### 성능 테스트

- **댓글 목록 조회**: 300ms 이내 (20개 댓글 기준)
- **댓글 작성**: 500ms 이내
- **댓글 수정**: 500ms 이내
- **댓글 삭제**: 500ms 이내
- **페이지네이션**: 다음 페이지 로드 300ms 이내

### 접근성 테스트

- 키보드로 댓글 입력 영역 포커스 이동 가능
- Tab 키로 버튼 간 이동 가능
- Enter 키로 게시 버튼 활성화 가능
- 스크린 리더로 댓글 내용 읽기 가능
- ARIA 레이블 확인

## 12. Notes

- **댓글 입력 방식**: ✅ RichTextEditor 사용 (카드 설명과 일관된 편집 경험)
- **실시간 업데이트**: ✅ Phase 1에서 제외, 추후 WebSocket 또는 폴링으로 구현 (수동 새로고침)
- **댓글 개수 표시**: ✅ 카드 목록 아이템에 댓글 개수 뱃지 표시 (💬 아이콘 + 숫자)
- **수정 이력 표시**: ✅ 댓글 수정 시 "(수정됨)" 표시 및 updatedAt 시간 표시
- **Soft delete vs. Hard delete**: ✅ Soft delete 방식 사용 (isDeleted 플래그, GDPR 대응)
- **페이지네이션 방식**: ✅ Offset 기반 사용 (page, size 파라미터)
- **대댓글(스레드)**: Priority-3에서 별도로 구현
- **@mention 기능**: Priority-3에서 별도로 구현
- **댓글 알림**: Priority-3에서 별도로 구현
- **모달 너비 확장**: `max-w-4xl` → `max-w-6xl`로 변경하여 2-column 레이아웃 수용
- **HTML 저장 방식**: RichTextEditor에서 생성된 HTML을 DB에 직접 저장, 렌더링 시 sanitization 적용

## 13. Related Documents

- `../../requirements/Priority-2/apply-comments-to-card.md` - 요구사항 정의
- `../Priority-1/model-cards-001.md` - Card 엔티티 정의
- `../Priority-1/api-spec.md` - API 명세 참조
- `../Priority-1/frontend-design.md` - 프론트엔드 디자인 시스템
- `spec-card-description-html-editor.md` - HTML Editor 참조 (HTML sanitization)
- `CLAUDE.md` - 프로젝트 전체 가이드라인
