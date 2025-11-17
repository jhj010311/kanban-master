import axiosInstance from '@/utils/axios';
import type { Card, CreateCardRequest, UpdateCardRequest } from '@/types/card';

class CardService {
  /**
   * 칼럼의 모든 카드 조회
   */
  async listCards(workspaceId: number, boardId: number, columnId: number): Promise<Card[]> {
    const response = await axiosInstance.get<Card[]>(
      `/workspaces/${workspaceId}/boards/${boardId}/columns/${columnId}/cards`
    );
    return response.data;
  }

  /**
   * 특정 카드 조회
   * Spec § 5. 프론트엔드 규격 - API 서비스
   * @param includeRelations - true일 경우 부모/자식 카드 정보 포함 (FR-06b, FR-06d)
   */
  async getCard(
    workspaceId: number,
    boardId: number,
    columnId: number,
    cardId: number,
    includeRelations: boolean = false
  ): Promise<Card> {
    const response = await axiosInstance.get<Card>(
      `/workspaces/${workspaceId}/boards/${boardId}/columns/${columnId}/cards/${cardId}`,
      {
        params: { includeRelations },
      }
    );
    return response.data;
  }

  /**
   * 카드 생성
   */
  async createCard(
    workspaceId: number,
    boardId: number,
    columnId: number,
    request: CreateCardRequest
  ): Promise<Card> {
    const response = await axiosInstance.post<Card>(
      `/workspaces/${workspaceId}/boards/${boardId}/columns/${columnId}/cards`,
      request
    );
    return response.data;
  }

  /**
   * 하위 카드 생성
   * Spec § 5. 프론트엔드 규격 - FR-06f: 하위 카드 생성
   * @param parentCardId - 부모 카드 ID
   */
  async createChildCard(
    workspaceId: number,
    boardId: number,
    columnId: number,
    parentCardId: number,
    request: Omit<CreateCardRequest, 'parentCardId'>
  ): Promise<Card> {
    return this.createCard(workspaceId, boardId, columnId, {
      ...request,
      parentCardId,
    });
  }

  /**
   * 카드 수정
   */
  async updateCard(
    workspaceId: number,
    boardId: number,
    columnId: number,
    cardId: number,
    request: UpdateCardRequest
  ): Promise<Card> {
    const response = await axiosInstance.put<Card>(
      `/workspaces/${workspaceId}/boards/${boardId}/columns/${columnId}/cards/${cardId}`,
      request
    );
    return response.data;
  }

  /**
   * 카드 삭제
   */
  async deleteCard(workspaceId: number, boardId: number, columnId: number, cardId: number): Promise<void> {
    await axiosInstance.delete(
      `/workspaces/${workspaceId}/boards/${boardId}/columns/${columnId}/cards/${cardId}`
    );
  }

  /**
   * 카드 위치 업데이트
   */
  async updateCardPosition(
    workspaceId: number,
    boardId: number,
    columnId: number,
    cardId: number,
    newPosition: number
  ): Promise<Card> {
    const response = await axiosInstance.put<Card>(
      `/workspaces/${workspaceId}/boards/${boardId}/columns/${columnId}/cards/${cardId}`,
      { position: newPosition }
    );
    return response.data;
  }

  /**
   * 부모 카드 후보 조회 (부모 카드가 없는 카드만 반환)
   * 1-레벨 계층 유지를 위해 이미 부모 카드인 카드는 제외
   */
  async getAvailableParentCards(_workspaceId: number, boardId: number): Promise<Card[]> {
    const response = await axiosInstance.get<Card[]>(
      `/search/boards/${boardId}/cards`,
      {
        params: {
          parentCardIdIsNull: true, // 부모 카드가 없는 카드만
        },
      }
    );
    return response.data;
  }
}

export default new CardService();
