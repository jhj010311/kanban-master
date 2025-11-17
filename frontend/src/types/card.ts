/**
 * 카드 타입 정의
 */

import type { Label } from './label';

/**
 * 부모 카드 요약 정보
 * Spec § 5. 프론트엔드 규격 - FR-06b: 부모 카드 정보 표시
 */
export interface ParentCardSummary {
  id: number;
  title: string;
  bgColor?: string;
  priority?: string;
  assignee?: string;
}

/**
 * 자식 카드 요약 정보
 * Spec § 5. 프론트엔드 규격 - FR-06d: 자식 카드 목록 표시
 */
export interface ChildCardSummary {
  id: number;
  title: string;
  bgColor?: string;
  priority?: string;
  isCompleted: boolean;
}

export interface Card {
  id: number;
  columnId: number;
  title: string;
  description?: string;
  position: number;
  bgColor?: string;
  priority?: string;
  assignee?: string;
  assigneeAvatarUrl?: string;
  dueDate?: string;
  isCompleted: boolean;
  labels?: Label[];
  commentCount?: number;
  createdAt: string;
  updatedAt: string;

  // Spec § 5. 프론트엔드 규격 - 타입 정의 확장
  // FR-06b, FR-06d: 부모/자식 카드 정보
  parentCardId?: number;
  parentCard?: ParentCardSummary;
  childCards?: ChildCardSummary[];
  // FR-06g: 자식 카드 개수 (CardItem에서 배지 표시용)
  childCount?: number;
}

export interface CreateCardRequest {
  title: string;
  description?: string;
  bgColor?: string;
  priority?: string;
  assignee?: string;
  dueDate?: string;

  // Spec § 5. 프론트엔드 규격 - FR-06f: 하위 카드 생성
  parentCardId?: number;
}

export interface UpdateCardRequest {
  title?: string;
  description?: string;
  bgColor?: string;
  priority?: string;
  assignee?: string;
  position?: number;
  dueDate?: string;
  isCompleted?: boolean;
  columnId?: number;
}
