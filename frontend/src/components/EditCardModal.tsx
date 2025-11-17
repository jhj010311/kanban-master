import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Card } from '@/types/card';
import { useCard } from '@/context/CardContext';
import { useAuth } from '@/context/AuthContext';
import { ErrorNotification } from '@/components/ErrorNotification';
import { LabelSelector } from '@/components/label/LabelSelector';
import { userService } from '@/services/userService';
import { labelService } from '@/services/labelService';
import cardService from '@/services/cardService';
import type { UserSearchResult } from '@/types/user';
import { useModalAnimation } from '@/hooks/useModalAnimation';
import { Avatar } from '@/components/common/Avatar';
import RichTextEditor from '@/components/RichTextEditor';
import { CommentSection } from '@/components/CommentSection';
import ParentCardLink from '@/components/ParentCardLink';
import ChildCardList from '@/components/ChildCardList';
import { CreateCardModal } from '@/components/CreateCardModal';
import {
    modalOverlayClass,
    modalPanelClass,
    modalLabelClass,
    modalInputClass,
    modalSelectClass,
    modalSecondaryButtonClass,
    modalPrimaryButtonClass,
    modalErrorClass,
    modalColorButtonClass,
} from '@/styles/modalStyles';

interface EditCardModalProps {
    card: Card;
    workspaceId: number;
    boardId: number;
    boardOwnerId: number;
    columnId: number;
    canEdit: boolean;
    onClose: () => void;
}

const cardPriorities = ['HIGH', 'MEDIUM', 'LOW'];

const cardColors = [
    { label: 'Blue', hex: '#e8f1ff' },
    { label: 'Pink', hex: '#ffe8f1' },
    { label: 'Green', hex: '#e8ffe8' },
    { label: 'Yellow', hex: '#ffffee' },
    { label: 'Purple', hex: '#f0e8ff' },
];

export const EditCardModal: React.FC<EditCardModalProps> = ({
    card,
    workspaceId,
    boardId,
    boardOwnerId,
    columnId,
    canEdit,
    onClose,
}) => {
    const { updateCard, loadCards } = useCard();
    const { user } = useAuth();
    const { stage, close } = useModalAnimation(onClose);

    // 현재 표시 중인 카드 상태 (부모/자식 네비게이션용)
    const [currentCard, setCurrentCard] = useState<Card>(card);
    const [isNavigating, setIsNavigating] = useState(false);

    // 자식 카드 생성 모달 상태
    const [showCreateChildModal, setShowCreateChildModal] = useState(false);

    const [title, setTitle] = useState(card.title);
    const [description, setDescription] = useState(card.description || '');
    const [selectedColor, setSelectedColor] = useState(card.bgColor || cardColors[0].hex);
    const [priority, setPriority] = useState(card.priority || '');
    const [dueDate, setDueDate] = useState(card.dueDate || '');
    const [isCompleted, setIsCompleted] = useState(card.isCompleted);
    const [selectedLabelIds, setSelectedLabelIds] = useState<number[]>(card.labels?.map((l) => l.id) || []);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [assigneeSearchInput, setAssigneeSearchInput] = useState('');
    const [assigneeResults, setAssigneeResults] = useState<UserSearchResult[]>([]);
    const [selectedAssignee, setSelectedAssignee] = useState<UserSearchResult | null>(
        card.assignee
            ? {
                  id: -1,
                  name: card.assignee,
                  email: '',
              }
            : null
    );
    const [assigneeSearching, setAssigneeSearching] = useState(false);
    const [assigneeDropdownOpen, setAssigneeDropdownOpen] = useState(false);
    const [descriptionSaving, setDescriptionSaving] = useState(false);
    const titleInputRef = useRef<HTMLInputElement>(null);
    const assigneeInputRef = useRef<HTMLInputElement>(null);
    const assigneeInputContainerRef = useRef<HTMLDivElement>(null);
    const assigneeDropdownRef = useRef<HTMLDivElement>(null);
    const assigneeDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const descriptionDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const performAssigneeSearch = async (keyword: string) => {
        const trimmedKeyword = keyword.trim();
        if (!trimmedKeyword) {
            setAssigneeResults([]);
            setAssigneeDropdownOpen(false);
            return;
        }

        try {
            setAssigneeSearching(true);
            const results = await userService.searchUsers(trimmedKeyword);
            setAssigneeResults(results);
            setAssigneeDropdownOpen(results.length > 0);
        } catch (err) {
            console.error('Failed to search users:', err);
            setAssigneeResults([]);
            setAssigneeDropdownOpen(false);
        } finally {
            setAssigneeSearching(false);
        }
    };

    const handleAssigneeInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value;
        setAssigneeSearchInput(value);

        if (assigneeDebounceRef.current) {
            clearTimeout(assigneeDebounceRef.current);
        }

        assigneeDebounceRef.current = setTimeout(() => {
            performAssigneeSearch(value);
        }, 300);
    };

    const handleSelectAssignee = (user: UserSearchResult) => {
        setSelectedAssignee(user);
        setAssigneeSearchInput('');
        setAssigneeResults([]);
        setAssigneeDropdownOpen(false);
    };

    const handleRemoveAssignee = () => {
        setSelectedAssignee(null);
        setAssigneeSearchInput('');
        setAssigneeResults([]);
        setAssigneeDropdownOpen(false);
    };

    // 부모/자식 카드 네비게이션 핸들러
    const handleNavigateToCard = async (targetCardId: number) => {
        try {
            setIsNavigating(true);
            setError(null);

            // includeRelations=true로 계층 정보 포함하여 카드 조회
            const targetCard = await cardService.getCard(
                workspaceId,
                boardId,
                columnId,
                targetCardId,
                true
            );

            // 현재 카드 상태 업데이트
            setCurrentCard(targetCard);

            // 폼 필드 초기화
            setTitle(targetCard.title);
            setDescription(targetCard.description || '');
            setSelectedColor(targetCard.bgColor || cardColors[0].hex);
            setPriority(targetCard.priority || '');
            setDueDate(targetCard.dueDate || '');
            setIsCompleted(targetCard.isCompleted);
            setSelectedLabelIds(targetCard.labels?.map((l) => l.id) || []);
            setSelectedAssignee(
                targetCard.assignee
                    ? {
                          id: -1,
                          name: targetCard.assignee,
                          email: '',
                      }
                    : null
            );

            // 담당자 검색 필드 초기화
            setAssigneeSearchInput('');
            setAssigneeResults([]);
            setAssigneeDropdownOpen(false);
        } catch (err) {
            console.error('Failed to navigate to card:', err);
            setError(err instanceof Error ? err.message : '카드 정보를 불러오는데 실패했습니다');
        } finally {
            setIsNavigating(false);
        }
    };

    // 설명 자동 저장 (debounced)
    const saveDescriptionOnly = useCallback(async (newDescription: string) => {
        if (!canEdit) return;

        try {
            setDescriptionSaving(true);
            await updateCard(workspaceId, boardId, columnId, currentCard.id, {
                description: newDescription,
            });
        } catch (err) {
            console.error('Failed to save description:', err);
            setError(err instanceof Error ? err.message : '설명 저장에 실패했습니다');
        } finally {
            setDescriptionSaving(false);
        }
    }, [canEdit, updateCard, workspaceId, boardId, columnId, currentCard.id]);

    const handleDescriptionChange = (newDescription: string) => {
        setDescription(newDescription);

        // debounce 타이머 초기화
        if (descriptionDebounceRef.current) {
            clearTimeout(descriptionDebounceRef.current);
        }

        // 2초 후 자동 저장
        descriptionDebounceRef.current = setTimeout(() => {
            saveDescriptionOnly(newDescription);
        }, 2000);
    };

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            const target = event.target as Node;
            const clickedInsideDropdown = assigneeDropdownRef.current && assigneeDropdownRef.current.contains(target);
            const clickedInsideInput =
                assigneeInputContainerRef.current && assigneeInputContainerRef.current.contains(target);

            if (!clickedInsideDropdown && !clickedInsideInput) {
                setAssigneeDropdownOpen(false);
            }
        };

        if (assigneeDropdownOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [assigneeDropdownOpen]);

    // 모달이 열릴 때 제목 필드에 자동 포커스
    useEffect(() => {
        const timer = setTimeout(() => {
            titleInputRef.current?.focus();
        }, 100);
        return () => clearTimeout(timer);
    }, []);

    useEffect(() => {
        return () => {
            if (assigneeDebounceRef.current) {
                clearTimeout(assigneeDebounceRef.current);
            }
            if (descriptionDebounceRef.current) {
                clearTimeout(descriptionDebounceRef.current);
            }
        };
    }, []);

    useEffect(() => {
        if (currentCard.assignee) {
            setSelectedAssignee({
                id: -1,
                name: currentCard.assignee,
                email: '',
            });
        } else {
            setSelectedAssignee(null);
        }
    }, [currentCard.assignee]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!title.trim()) {
            setError('카드 제목을 입력해주세요');
            return;
        }

        try {
            setLoading(true);
            setError(null);

            // 라벨 업데이트 먼저 수행 (변경이 있을 경우에만)
            const currentLabelIds = currentCard.labels?.map((l) => l.id).sort() || [];
            const newLabelIds = [...selectedLabelIds].sort();
            if (JSON.stringify(currentLabelIds) !== JSON.stringify(newLabelIds)) {
                await labelService.assignLabelsToCard(currentCard.id, selectedLabelIds);
            }

            // 기본 정보 업데이트 후 전체 데이터 새로고침
            await updateCard(workspaceId, boardId, columnId, currentCard.id, {
                title: title.trim(),
                description: description.trim() || undefined,
                bgColor: selectedColor || undefined,
                priority: priority || undefined,
                assignee: selectedAssignee?.name,
                dueDate: dueDate || undefined,
                isCompleted,
            });

            // 모든 변경사항이 반영된 최신 데이터를 가져옴
            await loadCards(workspaceId, boardId, columnId);

            close();
        } catch (err) {
            const message = err instanceof Error ? err.message : '카드 수정에 실패했습니다';
            setError(message);
            console.error('Failed to update card:', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <div
                className={modalOverlayClass(stage)}
                onClick={(e) => {
                    if (e.target === e.currentTarget) {
                        close();
                    }
                }}
            >
                <div
                    className={modalPanelClass({
                        stage,
                        maxWidth: 'max-w-6xl',
                        scrollable: false,
                    })}
                    style={{ maxHeight: '90vh' }}
                >
                    {/* 2열 레이아웃 */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 h-full">
                        {/* 왼쪽 컬럼: 카드 메타데이터 */}
                        <div className="overflow-y-auto pr-4 flex flex-col" style={{ maxHeight: '80vh' }}>
                            {/* 헤더 */}
                            <h2 className="text-2xl font-bold text-pastel-blue-900 mb-1">카드 수정</h2>
                            <p className="text-sm text-pastel-blue-600 mb-6">카드 정보를 수정하세요</p>

                            {/* 부모 카드 링크 (있는 경우에만 표시) */}
                            {currentCard.parentCard && (
                                <ParentCardLink
                                    parentCard={currentCard.parentCard}
                                    onNavigate={handleNavigateToCard}
                                    disabled={isNavigating}
                                />
                            )}

                    <form onSubmit={canEdit ? handleSubmit : (e) => e.preventDefault()}>
                        {/* Read-Only Notice */}
                        {!canEdit && (
                            <div className="mb-4 px-4 py-3 rounded-xl bg-pastel-yellow-100 border border-pastel-yellow-300 text-pastel-yellow-800 text-sm font-medium">
                                🔒 읽기 전용 모드 - 이 카드를 수정할 권한이 없습니다
                            </div>
                        )}

                        {/* 제목 입력 */}
                        <div className="mb-4">
                            <label className={modalLabelClass}>카드 제목 *</label>
                            <input
                                ref={titleInputRef}
                                type="text"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter' && !e.shiftKey && canEdit) {
                                        e.preventDefault();
                                        handleSubmit(e as unknown as React.FormEvent);
                                    }
                                }}
                                placeholder="예: 로그인 기능 구현"
                                className={modalInputClass}
                                disabled={loading || !canEdit}
                                readOnly={!canEdit}
                            />
                        </div>

                        {/* 우선순위 + 마감일 (2열 그리드) */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
                            {/* 우선순위 선택 */}
                            <div>
                                <label className={modalLabelClass}>우선순위</label>
                                <select
                                    value={priority}
                                    onChange={(e) => setPriority(e.target.value)}
                                    className={modalSelectClass}
                                    disabled={loading || !canEdit}
                                >
                                    <option value="">우선순위 선택 (선택사항)</option>
                                    {cardPriorities.map((p) => (
                                        <option key={p} value={p}>
                                            {p}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {/* 마감 날짜 입력 */}
                            <div>
                                <label className={modalLabelClass}>마감일</label>
                                <input
                                    type="date"
                                    value={dueDate}
                                    onChange={(e) => setDueDate(e.target.value)}
                                    className={modalInputClass}
                                    disabled={loading || !canEdit}
                                    readOnly={!canEdit}
                                />
                            </div>
                        </div>

                        {/* 담당자 입력 */}
                        <div className="mb-4">
                            <label className={modalLabelClass}>담당자</label>
                            <div className="relative">
                                <div
                                    ref={assigneeInputContainerRef}
                                    className="flex flex-wrap items-center gap-2 px-4 py-2 rounded-xl border border-white/40 bg-white/40 backdrop-blur-sm focus-within:ring-2 focus-within:ring-pastel-blue-300/60"
                                >
                                    {selectedAssignee && (
                                        <div className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-gradient-to-br from-pastel-blue-400 to-pastel-cyan-400 border-2 border-pastel-blue-500 text-white text-sm font-bold shadow-md">
                                            <span>👤 {selectedAssignee.name}</span>
                                            {canEdit && (
                                                <button
                                                    type="button"
                                                    onClick={handleRemoveAssignee}
                                                    disabled={loading}
                                                    className="text-white hover:text-pastel-pink-200 disabled:opacity-50 transition-colors"
                                                    aria-label="담당자 제거"
                                                >
                                                    ✕
                                                </button>
                                            )}
                                        </div>
                                    )}

                                    <input
                                        ref={assigneeInputRef}
                                        type="text"
                                        value={assigneeSearchInput}
                                        onChange={handleAssigneeInputChange}
                                        onFocus={() =>
                                            canEdit && assigneeResults.length > 0 && setAssigneeDropdownOpen(true)
                                        }
                                        placeholder={selectedAssignee ? '' : '이름 또는 이메일로 검색 (선택사항)'}
                                        className="borderless-input flex-1 min-w-0 bg-transparent text-pastel-blue-900 placeholder-pastel-blue-500 focus:outline-none"
                                        disabled={loading || !canEdit}
                                        readOnly={!canEdit}
                                    />

                                    {assigneeSearching && (
                                        <div className="h-4 w-4 border-2 border-pastel-blue-400 border-t-transparent rounded-full animate-spin" />
                                    )}
                                </div>

                                {assigneeDropdownOpen && assigneeResults.length > 0 && (
                                    <div
                                        ref={assigneeDropdownRef}
                                        className="absolute top-full left-0 right-0 mt-2 rounded-2xl border border-white/40 bg-white/80 backdrop-blur-lg shadow-glass max-h-48 overflow-y-auto z-10"
                                    >
                                        {assigneeResults.map((user) => (
                                            <button
                                                key={user.id}
                                                type="button"
                                                onClick={() => handleSelectAssignee(user)}
                                                className="w-full px-4 py-2 text-left hover:bg-white/70 border-b border-white/30 last:border-b-0 transition-colors flex items-center gap-3"
                                            >
                                                <Avatar avatarUrl={user.avatarUrl} userName={user.name} size="sm" />
                                                <div className="flex-1 min-w-0">
                                                    <div className="font-medium text-pastel-blue-900">{user.name}</div>
                                                    <div className="text-xs text-pastel-blue-500">{user.email}</div>
                                                </div>
                                            </button>
                                        ))}
                                    </div>
                                )}
                            </div>
                            {assigneeSearchInput && assigneeResults.length === 0 && !assigneeSearching && (
                                <p className="text-xs text-pastel-blue-500 mt-1">검색 결과가 없습니다</p>
                            )}
                        </div>

                        {/* 라벨 */}
                        {canEdit && (
                            <div className="mb-4">
                                <label className={modalLabelClass}>라벨</label>
                                <div className="max-h-32 overflow-y-auto rounded-2xl border border-white/30 bg-white/30 p-2">
                                    <LabelSelector
                                        boardId={boardId}
                                        cardId={currentCard.id}
                                        selectedLabelIds={selectedLabelIds}
                                        onChange={setSelectedLabelIds}
                                    />
                                </div>
                            </div>
                        )}

                        {/* 색상 선택 */}
                        <div className="mb-4">
                            <label className={modalLabelClass}>색상</label>
                            <div className="grid grid-cols-5 gap-2">
                                {cardColors.map((color) => (
                                    <button
                                        key={color.hex}
                                        type="button"
                                        onClick={canEdit ? () => setSelectedColor(color.hex) : undefined}
                                        style={{ backgroundColor: color.hex }}
                                        className={`w-full h-10 ${modalColorButtonClass(
                                            selectedColor === color.hex
                                        )}`}
                                        title={color.label}
                                        disabled={loading || !canEdit}
                                    />
                                ))}
                            </div>
                        </div>

                        {/* 에러 메시지 */}
                        {error && <div className={`mb-4 ${modalErrorClass}`}>{error}</div>}

                        {/* Spacer to push content to bottom */}
                        <div className="flex-1"></div>

                        {/* 자식 카드 목록 (항상 표시) */}
                        <ChildCardList
                            childCards={currentCard.childCards || []}
                            onNavigate={handleNavigateToCard}
                            onCreateChild={() => setShowCreateChildModal(true)}
                            disabled={isNavigating || !canEdit}
                        />

                        {/* 완료 상태 */}
                        <div className="mb-4">
                            <div className="flex items-center gap-3 rounded-2xl border border-white/30 bg-white/30 px-4 py-3">
                                <input
                                    type="checkbox"
                                    checked={isCompleted}
                                    onChange={(e) => setIsCompleted(e.target.checked)}
                                    disabled={loading || !canEdit}
                                    className="w-5 h-5 rounded border-2 border-pastel-blue-200 cursor-pointer accent-pastel-green-500 bg-white disabled:cursor-not-allowed"
                                />
                                <span className="text-sm font-semibold text-pastel-blue-900">카드를 완료로 표시</span>
                            </div>
                        </div>

                        {/* 버튼 */}
                        <div className="flex gap-3">
                            <button
                                type="button"
                                onClick={close}
                                disabled={loading}
                                className={`flex-1 ${modalSecondaryButtonClass}`}
                            >
                                {canEdit ? '취소' : '닫기'}
                            </button>
                            {canEdit && (
                                <button
                                    type="submit"
                                    disabled={loading}
                                    className={`flex-1 ${modalPrimaryButtonClass}`}
                                >
                                    {loading ? '수정 중...' : '수정'}
                                </button>
                            )}
                        </div>
                    </form>
                        </div>

                        {/* 오른쪽 컬럼: 설명 + 댓글 섹션 */}
                        <div className="border-l border-gray-200 pl-6 overflow-y-auto" style={{ maxHeight: '80vh' }}>
                            {/* 설명 */}
                            <div className="mb-6">
                                <div className="flex items-center justify-between mb-2">
                                    <label className={modalLabelClass}>설명</label>
                                    {descriptionSaving && (
                                        <span className="text-xs text-pastel-blue-500 flex items-center gap-1">
                                            <div className="h-3 w-3 border-2 border-pastel-blue-400 border-t-transparent rounded-full animate-spin" />
                                            저장 중...
                                        </span>
                                    )}
                                </div>
                                <RichTextEditor
                                    value={description}
                                    onChange={handleDescriptionChange}
                                    placeholder="카드에 대한 설명을 입력하세요 (선택사항)"
                                    readOnly={!canEdit}
                                    disabled={loading || !canEdit}
                                    maxLength={50000}
                                />
                            </div>

                            {/* 댓글 섹션 */}
                            {user && (
                                <CommentSection
                                    workspaceId={workspaceId}
                                    boardId={boardId}
                                    cardId={currentCard.id}
                                    currentUserId={user.id}
                                    isOwner={user.id === boardOwnerId}
                                />
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* 자식 카드 생성 모달 */}
            {showCreateChildModal && (
                <CreateCardModal
                    workspaceId={workspaceId}
                    boardId={boardId}
                    columnId={columnId}
                    parentCardId={currentCard.id}
                    onClose={() => setShowCreateChildModal(false)}
                    onSuccess={async (_newCard) => {
                        setShowCreateChildModal(false);
                        try {
                            // 현재 카드를 다시 불러와서 새로 생성된 자식 카드를 포함시킴
                            const refreshedCard = await cardService.getCard(
                                workspaceId,
                                boardId,
                                columnId,
                                currentCard.id,
                                true
                            );
                            setCurrentCard(refreshedCard);
                            // 카드 목록도 새로고침하여 childCount 업데이트
                            await loadCards(workspaceId, boardId, columnId);
                        } catch (err) {
                            console.error('Failed to refresh card after child creation:', err);
                            setError(err instanceof Error ? err.message : '카드 정보를 새로고침하는데 실패했습니다');
                        }
                    }}
                />
            )}

            {/* 에러 알림 (모달 외부 표시) */}
            {error && <ErrorNotification message={error} onClose={() => setError(null)} duration={5000} />}
        </>
    );
};
