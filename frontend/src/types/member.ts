export type BoardMemberRole = 'VIEWER' | 'EDITOR' | 'MANAGER';
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED';

export interface BoardMember {
  boardId: number;
  userId: number;
  userEmail: string;
  userName: string;
  avatarUrl?: string | null;
  role: BoardMemberRole;
  invitationStatus: InvitationStatus;
  invitedAt?: string;
  acceptedAt?: string;
  createdAt: string;
  boardName?: string;
  invitationToken?: string;
  invitedByName?: string;
}

export interface InviteMemberRequest {
  userId: number;
  role: BoardMemberRole;
  message?: string;
}

export interface ChangeRoleRequest {
  role: BoardMemberRole;
}
