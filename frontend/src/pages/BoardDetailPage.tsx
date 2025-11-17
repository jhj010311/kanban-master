import { useState, useCallback } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useColumn } from "@/context/ColumnContext";
import { useCard } from "@/context/CardContext";
import { usePresenceTransition } from "@/hooks/usePresenceTransition";
import { usePermissions } from "@/hooks/usePermissions";
import type { CardSearchResult } from "@/types/search";
import { BoardHeader } from "./BoardDetailPage/components/BoardHeader";
import { ColumnsSection } from "./BoardDetailPage/components/ColumnsSection";
import { MembersPanel } from "./BoardDetailPage/components/MembersPanel";
import { ActivityPanel } from "./BoardDetailPage/components/ActivityPanel";
import {
  BoardLoadingState,
  BoardErrorState,
} from "./BoardDetailPage/components/BoardStateFallbacks";
import { BoardModals } from "./BoardDetailPage/components/BoardModals";
import {
  useAutoOpenTargets,
  useBoardData,
  useOverdueCardCount,
} from "./BoardDetailPage/hooks";

const BoardDetailPage = () => {
  const navigate = useNavigate();
  const { workspaceId, boardId } = useParams<{
    workspaceId: string;
    boardId: string;
  }>();
  const [searchParams] = useSearchParams();
  const { columns, loading: columnsLoading, loadColumns } = useColumn();
  const { cards } = useCard();

  const [showCreateColumnModal, setShowCreateColumnModal] = useState(false);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [showMembersPanel, setShowMembersPanel] = useState(false);
  const [showActivityPanel, setShowActivityPanel] = useState(false);
  const [showLabelManager, setShowLabelManager] = useState(false);
  const [showSearchPanel, setShowSearchPanel] = useState(false);

  const membersPanelTransition = usePresenceTransition(showMembersPanel);
  const activityPanelTransition = usePresenceTransition(showActivityPanel);

  const { board, loading, error } = useBoardData(
    workspaceId,
    boardId,
    loadColumns,
  );
  const { canEdit, canManage } = usePermissions(board);
  const {
    effectiveAutoOpenCardId,
    effectiveAutoOpenColumnId,
    setInlineCardFocus,
    handleInlineAutoOpenHandled,
  } = useAutoOpenTargets(searchParams);
  const overdueCardCount = useOverdueCardCount(columns, cards);

  const workspaceNumericId = workspaceId ? Number(workspaceId) : Number.NaN;
  const boardNumericId = boardId ? Number(boardId) : Number.NaN;
  const hasValidNumericIds =
    !Number.isNaN(workspaceNumericId) && !Number.isNaN(boardNumericId);

  const refreshColumns = useCallback(async () => {
    if (!hasValidNumericIds) {
      return;
    }
    await loadColumns(workspaceNumericId, boardNumericId);
  }, [hasValidNumericIds, loadColumns, workspaceNumericId, boardNumericId]);

  const handleNavigateBack = () => navigate("/boards");

  const handleCardSelect = useCallback(
    (result: CardSearchResult) => {
      setInlineCardFocus({ cardId: result.id, columnId: result.columnId });
    },
    [setInlineCardFocus],
  );

  if (loading) {
    return <BoardLoadingState />;
  }

  if (error || !board || !hasValidNumericIds) {
    return <BoardErrorState onBack={handleNavigateBack} />;
  }

  return (
    <div className="bg-gradient-pastel flex flex-col h-full">
      <BoardHeader
        boardName={board.name}
        overdueCardCount={overdueCardCount}
        onBack={handleNavigateBack}
        onSearch={() => setShowSearchPanel(true)}
        onLabelManager={() => setShowLabelManager(true)}
        onToggleActivity={() => setShowActivityPanel((prev) => !prev)}
        onToggleMembers={() => setShowMembersPanel((prev) => !prev)}
      />

      <main className="flex-1 overflow-hidden flex flex-col">
        <div className="w-full px-4 sm:px-6 lg:px-8 flex-1 overflow-hidden pt-4 pb-4 flex">
          <div className="w-full max-w-[95vw] mx-auto flex flex-1 relative min-h-0 h-full">
            <div className="flex-1 overflow-auto flex flex-col pr-0 lg:pr-4 h-full">
              <ColumnsSection
                columns={columns}
                columnsLoading={columnsLoading}
                workspaceId={workspaceNumericId}
                boardId={boardNumericId}
                boardOwnerId={board?.ownerId ?? 0}
                canEdit={canEdit}
                onCreateColumn={() => setShowCreateColumnModal(true)}
                autoOpenCardId={effectiveAutoOpenCardId}
                autoOpenColumnId={effectiveAutoOpenColumnId}
                onAutoOpenHandled={handleInlineAutoOpenHandled}
              />
            </div>

            <MembersPanel
              transition={{
                shouldRender: membersPanelTransition.shouldRender,
                stage: membersPanelTransition.stage,
              }}
              boardId={boardNumericId}
              canManage={canManage}
              onInvite={() => setShowInviteModal(true)}
              onClose={() => setShowMembersPanel(false)}
            />

            <ActivityPanel
              transition={{
                shouldRender: activityPanelTransition.shouldRender,
                stage: activityPanelTransition.stage,
              }}
              boardId={boardNumericId}
              onClose={() => setShowActivityPanel(false)}
            />
          </div>
        </div>
      </main>

      <BoardModals
        workspaceId={workspaceNumericId}
        boardId={boardNumericId}
        showCreateColumnModal={showCreateColumnModal}
        onCloseCreateColumn={() => setShowCreateColumnModal(false)}
        refreshColumns={refreshColumns}
        showInviteModal={showInviteModal}
        onCloseInviteModal={() => setShowInviteModal(false)}
        showLabelManager={showLabelManager}
        onCloseLabelManager={() => setShowLabelManager(false)}
        showSearchPanel={showSearchPanel}
        onCloseSearchPanel={() => setShowSearchPanel(false)}
        onCardSelect={handleCardSelect}
      />
    </div>
  );
};

export default BoardDetailPage;
