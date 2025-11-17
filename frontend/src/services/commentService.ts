/**
 * 댓글 API 서비스
 *
 * Spec § 4. 프론트엔드 규격 - API 호출
 * Spec § 5. API 명세
 */

import axiosInstance from '@/utils/axios';
import type {
  Comment,
  CommentPage,
  CreateCommentRequest,
  UpdateCommentRequest,
} from '../types/comment';

/**
 * 댓글 목록 조회
 *
 * Spec § 5.1: GET /workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments
 * Spec § FR-06b: 댓글 목록 조회
 * Spec § FR-06f: 페이지네이션 (기본 20개/페이지)
 *
 * @param workspaceId 워크스페이스 ID
 * @param boardId 보드 ID
 * @param cardId 카드 ID
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기 (기본 20)
 * @returns 댓글 페이지 응답
 */
export const getComments = async (
  workspaceId: number,
  boardId: number,
  cardId: number,
  page: number = 0,
  size: number = 20
): Promise<CommentPage> => {
  const response = await axiosInstance.get<CommentPage>(
    `/workspaces/${workspaceId}/boards/${boardId}/cards/${cardId}/comments`,
    {
      params: { page, size },
    }
  );
  return response.data;
};

/**
 * 댓글 생성
 *
 * Spec § 5.2: POST /workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments
 * Spec § FR-06a: 댓글 작성
 * Spec § FR-06g: 빈 댓글 방지
 * Spec § FR-06h: Activity 로그 기록
 * Spec § FR-06i: XSS 방지
 *
 * @param workspaceId 워크스페이스 ID
 * @param boardId 보드 ID
 * @param cardId 카드 ID
 * @param request 댓글 생성 요청
 * @returns 생성된 댓글
 */
export const createComment = async (
  workspaceId: number,
  boardId: number,
  cardId: number,
  request: CreateCommentRequest
): Promise<Comment> => {
  const response = await axiosInstance.post<Comment>(
    `/workspaces/${workspaceId}/boards/${boardId}/cards/${cardId}/comments`,
    request
  );
  return response.data;
};

/**
 * 댓글 수정
 *
 * Spec § 5.3: PUT /workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments/{commentId}
 * Spec § FR-06c: 댓글 수정 (작성자 본인만 가능)
 * Spec § FR-06i: XSS 방지
 *
 * @param workspaceId 워크스페이스 ID
 * @param boardId 보드 ID
 * @param cardId 카드 ID
 * @param commentId 댓글 ID
 * @param request 댓글 수정 요청
 * @returns 수정된 댓글
 */
export const updateComment = async (
  workspaceId: number,
  boardId: number,
  cardId: number,
  commentId: number,
  request: UpdateCommentRequest
): Promise<Comment> => {
  const response = await axiosInstance.put<Comment>(
    `/workspaces/${workspaceId}/boards/${boardId}/cards/${cardId}/comments/${commentId}`,
    request
  );
  return response.data;
};

/**
 * 댓글 삭제
 *
 * Spec § 5.4: DELETE /workspaces/{workspaceId}/boards/{boardId}/cards/{cardId}/comments/{commentId}
 * Spec § FR-06d: 댓글 삭제 (작성자 또는 보드 OWNER만 가능)
 * Spec § FR-06h: Activity 로그 기록
 * Spec § FR-06n: Soft Delete 방식 사용
 *
 * @param workspaceId 워크스페이스 ID
 * @param boardId 보드 ID
 * @param cardId 카드 ID
 * @param commentId 댓글 ID
 */
export const deleteComment = async (
  workspaceId: number,
  boardId: number,
  cardId: number,
  commentId: number
): Promise<void> => {
  await axiosInstance.delete(
    `/workspaces/${workspaceId}/boards/${boardId}/cards/${cardId}/comments/${commentId}`
  );
};
