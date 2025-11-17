/**
 * 댓글 타입 정의
 *
 * Spec § 4. 프론트엔드 규격 - 타입 정의
 */

/**
 * 댓글 객체
 *
 * Spec § 3.2: 댓글 데이터 모델
 * Backend CommentResponse의 flat structure와 일치
 */
export interface Comment {
  id: number;
  cardId: number;
  authorId: number;
  authorName: string;
  authorEmail: string;
  authorAvatarUrl?: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 댓글 목록 페이지 응답
 *
 * Spec § FR-06f: 페이지네이션 (기본 20개/페이지)
 */
export interface CommentPage {
  content: Comment[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

/**
 * 댓글 생성 요청
 *
 * Spec § FR-06a: 댓글 작성
 * Spec § FR-06g: 빈 댓글 방지
 * Spec § NFR-06c: 최대 10,000자 제한
 */
export interface CreateCommentRequest {
  content: string;
}

/**
 * 댓글 수정 요청
 *
 * Spec § FR-06c: 댓글 수정
 * Spec § FR-06g: 빈 댓글 방지
 * Spec § NFR-06c: 최대 10,000자 제한
 */
export interface UpdateCommentRequest {
  content: string;
}
