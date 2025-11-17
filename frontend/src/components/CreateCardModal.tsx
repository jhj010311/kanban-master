import React, { useEffect, useRef, useState } from 'react';
import { useCard } from '@/context/CardContext';
import { ErrorNotification } from '@/components/ErrorNotification';
import { LabelSelector } from '@/components/label/LabelSelector';
import { labelService } from '@/services/labelService';
import { userService } from '@/services/userService';
import cardService from '@/services/cardService';
import type { UserSearchResult } from '@/types/user';
import { useModalAnimation } from '@/hooks/useModalAnimation';
import { Avatar } from '@/components/common/Avatar';
import RichTextEditor from '@/components/RichTextEditor';
import { CollapsibleSection } from '@/components/common/CollapsibleSection';
import type { Card } from '@/types/card';
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

interface CreateCardModalProps {
    workspaceId: number;
    boardId: number;
    columnId: number;
    onClose: () => void;
    onSuccess?: (card: Card) => void;
    parentCardId?: number; // 부모 카드 ID (선택 사항, 하위 카드 생성 시 사용)
}

const cardPriorities = ['HIGH', 'MEDIUM', 'LOW'];

const cardColors = [
    { label: 'Blue', hex: '#e8f1ff' },
    { label: 'Pink', hex: '#ffe8f1' },
    { label: 'Green', hex: '#e8ffe8' },
    { label: 'Yellow', hex: '#ffffee' },
    { label: 'Purple', hex: '#f0e8ff' },
];

export const CreateCardModal: React.FC<CreateCardModalProps> = ({
    workspaceId,
    boardId,
    columnId,
    onClose,
    onSuccess,
    parentCardId
}) => {
    const { createCard } = useCard();
    const { stage, close } = useModalAnimation(onClose);
    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [selectedColor, setSelectedColor] = useState(cardColors[0].hex);
    const [priority, setPriority] = useState<string>('');
    const [dueDate, setDueDate] = useState('');
    const [selectedLabelIds, setSelectedLabelIds] = useState<number[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [assigneeSearchInput, setAssigneeSearchInput] = useState('');
    const [assigneeResults, setAssigneeResults] = useState<UserSearchResult[]>([]);
    const [selectedAssignee, setSelectedAssignee] = useState<UserSearchResult | null>(null);
    const [assigneeSearching, setAssigneeSearching] = useState(false);
    const [assigneeDropdownOpen, setAssigneeDropdownOpen] = useState(false);
    const titleInputRef = useRef<HTMLInputElement>(null);
    const assigneeInputRef = useRef<HTMLInputElement>(null);
    const assigneeInputContainerRef = useRef<HTMLDivElement>(null);
    const assigneeDropdownRef = useRef<HTMLDivElement>(null);
    const assigneeDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const selectedColorInfo = cardColors.find((color) => color.hex === selectedColor);

    // 부모 카드 선택 관련 상태
    const [parentCardSearchInput, setParentCardSearchInput] = useState('');
    const [parentCardResults, setParentCardResults] = useState<Card[]>([]);
    const [selectedParentCard, setSelectedParentCard] = useState<Card | null>(null);
    const [parentCardSearching, setParentCardSearching] = useState(false);
    const [parentCardDropdownOpen, setParentCardDropdownOpen] = useState(false);
    const parentCardInputRef = useRef<HTMLInputElement>(null);
    const parentCardInputContainerRef = useRef<HTMLDivElement>(null);
    const parentCardDropdownRef = useRef<HTMLDivElement>(null);
    const parentCardDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

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

    // 부모 카드 검색 함수
    const performParentCardSearch = async (keyword: string) => {
        const trimmedKeyword = keyword.trim();
        if (!trimmedKeyword) {
            setParentCardResults([]);
            setParentCardDropdownOpen(false);
            return;
        }

        try {
            setParentCardSearching(true);
            const allCards = await cardService.getAvailableParentCards(workspaceId, boardId);
            const filtered = allCards.filter(card =>
                card.title.toLowerCase().includes(trimmedKeyword.toLowerCase())
            );
            setParentCardResults(filtered);
            setParentCardDropdownOpen(filtered.length > 0);
        } catch (err) {
            console.error('Failed to search parent cards:', err);
            setParentCardResults([]);
            setParentCardDropdownOpen(false);
        } finally {
            setParentCardSearching(false);
        }
    };

    const handleParentCardInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value;
        setParentCardSearchInput(value);

        if (parentCardDebounceRef.current) {
            clearTimeout(parentCardDebounceRef.current);
        }

        parentCardDebounceRef.current = setTimeout(() => {
            performParentCardSearch(value);
        }, 300);
    };

    const handleSelectParentCard = (card: Card) => {
        setSelectedParentCard(card);
        setParentCardSearchInput('');
        setParentCardResults([]);
        setParentCardDropdownOpen(false);
    };

    const handleRemoveParentCard = () => {
        setSelectedParentCard(null);
        setParentCardSearchInput('');
        setParentCardResults([]);
        setParentCardDropdownOpen(false);
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

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            const target = event.target as Node;
            const clickedInsideDropdown = parentCardDropdownRef.current && parentCardDropdownRef.current.contains(target);
            const clickedInsideInput =
                parentCardInputContainerRef.current && parentCardInputContainerRef.current.contains(target);

            if (!clickedInsideDropdown && !clickedInsideInput) {
                setParentCardDropdownOpen(false);
            }
        };

        if (parentCardDropdownOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [parentCardDropdownOpen]);

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
            if (parentCardDebounceRef.current) {
                clearTimeout(parentCardDebounceRef.current);
            }
        };
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!title.trim()) {
            setError('카드 제목을 입력해주세요');
            return;
        }

        try {
            setLoading(true);
            setError(null);

            // parentCardId prop이 있으면 우선 사용, 없으면 선택된 부모 카드 ID 사용
            const finalParentCardId = parentCardId ?? selectedParentCard?.id;

            const newCard = await createCard(workspaceId, boardId, columnId, {
                title: title.trim(),
                description: description.trim() || undefined,
                bgColor: selectedColor,
                priority: priority || undefined,
                assignee: selectedAssignee?.name,
                dueDate: dueDate || undefined,
                parentCardId: finalParentCardId, // 부모 카드 ID 포함 (있을 경우)
            });

            // 라벨 할당 (선택된 라벨이 있을 경우)
            if (selectedLabelIds.length > 0 && newCard?.id) {
                await labelService.assignLabelsToCard(newCard.id, selectedLabelIds);
            }

            onSuccess?.(newCard);
            close();
        } catch (err) {
            const message = err instanceof Error ? err.message : '카드 생성에 실패했습니다';
            setError(message);
            console.error('Failed to create card:', err);
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
                        maxWidth: 'max-w-lg',
                        scrollable: true,
                    })}
                >
                    {/* 헤더 */}
                    <h2 className="text-2xl font-bold text-pastel-blue-900 mb-1">
                        {parentCardId ? '🔗 하위 카드 생성' : '카드 생성'}
                    </h2>
                    <p className="text-sm text-pastel-blue-600 mb-6">
                        {parentCardId ? '부모 카드의 하위 카드를 생성하세요' : '새로운 카드를 생성하세요'}
                    </p>

                    <form onSubmit={handleSubmit}>
                        {/* 제목 입력 */}
                        <div className="mb-4">
                            <label className={modalLabelClass}>카드 제목 *</label>
                            <input
                                ref={titleInputRef}
                                type="text"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter' && !e.shiftKey) {
                                        e.preventDefault();
                                        handleSubmit(e as unknown as React.FormEvent);
                                    }
                                }}
                                placeholder="예: 로그인 기능 구현"
                                className={modalInputClass}
                                disabled={loading}
                            />
                        </div>

                        {/* 설명 입력 */}
                        <div className="mb-4">
                            <label className={modalLabelClass}>설명</label>
                            <RichTextEditor
                                value={description}
                                onChange={setDescription}
                                placeholder="카드에 대한 설명을 입력하세요 (선택사항)"
                                disabled={loading}
                                maxLength={50000}
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
                                    disabled={loading}
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
                                    disabled={loading}
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
                                            <button
                                                type="button"
                                                onClick={handleRemoveAssignee}
                                                disabled={loading}
                                                className="text-white hover:text-pastel-pink-200 disabled:opacity-50 transition-colors"
                                                aria-label="담당자 제거"
                                            >
                                                ✕
                                            </button>
                                        </div>
                                    )}

                                    <input
                                        ref={assigneeInputRef}
                                        type="text"
                                        value={assigneeSearchInput}
                                        onChange={handleAssigneeInputChange}
                                        onFocus={() => assigneeResults.length > 0 && setAssigneeDropdownOpen(true)}
                                        placeholder={selectedAssignee ? '' : '이름 또는 이메일로 검색 (선택사항)'}
                                        className="borderless-input flex-1 min-w-0 bg-transparent text-pastel-blue-900 placeholder-pastel-blue-500 focus:outline-none"
                                        disabled={loading}
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
                                                className="w-full px-4 py-2 text-left hover:bg-white/70 transition-colors border-b border-white/30 last:border-b-0 flex items-center gap-3"
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

                        {/* 부모 카드 선택 (parentCardId prop이 없을 때만 표시) */}
                        {!parentCardId && (
                            <div className="mb-4">
                                <label className={modalLabelClass}>부모 카드 (선택사항)</label>
                                <div className="relative">
                                    <div
                                        ref={parentCardInputContainerRef}
                                        className="flex flex-wrap items-center gap-2 px-4 py-2 rounded-xl border border-white/40 bg-white/40 backdrop-blur-sm focus-within:ring-2 focus-within:ring-pastel-blue-300/60"
                                    >
                                        {selectedParentCard && (
                                            <div className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-gradient-to-br from-pastel-purple-400 to-pastel-pink-400 border-2 border-pastel-purple-500 text-white text-sm font-bold shadow-md">
                                                <span>🔗 {selectedParentCard.title}</span>
                                                <button
                                                    type="button"
                                                    onClick={handleRemoveParentCard}
                                                    disabled={loading}
                                                    className="text-white hover:text-pastel-pink-200 disabled:opacity-50 transition-colors"
                                                    aria-label="부모 카드 제거"
                                                >
                                                    ✕
                                                </button>
                                            </div>
                                        )}

                                        <input
                                            ref={parentCardInputRef}
                                            type="text"
                                            value={parentCardSearchInput}
                                            onChange={handleParentCardInputChange}
                                            onFocus={() => parentCardResults.length > 0 && setParentCardDropdownOpen(true)}
                                            placeholder={selectedParentCard ? '' : '부모 카드 검색 (선택사항)'}
                                            className="borderless-input flex-1 min-w-0 bg-transparent text-pastel-blue-900 placeholder-pastel-blue-500 focus:outline-none"
                                            disabled={loading}
                                        />

                                        {parentCardSearching && (
                                            <div className="h-4 w-4 border-2 border-pastel-blue-400 border-t-transparent rounded-full animate-spin" />
                                        )}
                                    </div>

                                    {parentCardDropdownOpen && parentCardResults.length > 0 && (
                                        <div
                                            ref={parentCardDropdownRef}
                                            className="absolute top-full left-0 right-0 mt-2 rounded-2xl border border-white/40 bg-white/80 backdrop-blur-lg shadow-glass max-h-48 overflow-y-auto z-10"
                                        >
                                            {parentCardResults.map((card) => (
                                                <button
                                                    key={card.id}
                                                    type="button"
                                                    onClick={() => handleSelectParentCard(card)}
                                                    className="w-full px-4 py-2 text-left hover:bg-white/70 transition-colors border-b border-white/30 last:border-b-0 flex items-center gap-3"
                                                >
                                                    <div className="flex-1 min-w-0">
                                                        <div className="font-medium text-pastel-blue-900 truncate">{card.title}</div>
                                                        {card.description && (
                                                            <div className="text-xs text-pastel-blue-500 truncate">
                                                                {card.description.replace(/<[^>]+>/g, '').substring(0, 50)}
                                                            </div>
                                                        )}
                                                    </div>
                                                    {card.priority && (
                                                        <span className="text-xs px-2 py-0.5 rounded bg-pastel-blue-100 text-pastel-blue-700">
                                                            {card.priority}
                                                        </span>
                                                    )}
                                                </button>
                                            ))}
                                        </div>
                                    )}
                                </div>
                                {parentCardSearchInput && parentCardResults.length === 0 && !parentCardSearching && (
                                    <p className="text-xs text-pastel-blue-500 mt-1">검색 결과가 없습니다</p>
                                )}
                            </div>
                        )}

                        {/* 상세 정보 (라벨 + 색상) */}
                        <CollapsibleSection
                            className="mb-6"
                            title="상세 정보"
                            summary={
                                <div className="flex items-center gap-2 text-xs text-pastel-blue-500">
                                    <span>
                                        {selectedLabelIds.length > 0
                                            ? `라벨 ${selectedLabelIds.length}개`
                                            : '라벨 미선택'}
                                    </span>
                                    <span className="text-pastel-blue-200">•</span>
                                    <span
                                        className="inline-flex h-4 w-4 rounded-full border border-white/70 shadow-inner"
                                        style={{ backgroundColor: selectedColor }}
                                    />
                                    <span>{selectedColorInfo?.label ?? '사용자 정의 색상'}</span>
                                </div>
                            }
                        >
                            <div className="mb-4">
                                <label className={`${modalLabelClass} !mb-2`}>라벨</label>
                                <div className="max-h-32 overflow-y-auto rounded-2xl border border-white/30 bg-white/30 p-2">
                                    <LabelSelector
                                        boardId={boardId}
                                        selectedLabelIds={selectedLabelIds}
                                        onChange={setSelectedLabelIds}
                                    />
                                </div>
                            </div>

                            <div>
                                <label className={`${modalLabelClass} !mb-2`}>색상 선택</label>
                                <div className="grid grid-cols-5 gap-2">
                                    {cardColors.map((color) => (
                                        <button
                                            key={color.hex}
                                            type="button"
                                            onClick={() => setSelectedColor(color.hex)}
                                            style={{ backgroundColor: color.hex }}
                                            className={`w-full h-10 ${modalColorButtonClass(
                                                selectedColor === color.hex
                                            )}`}
                                            title={color.label}
                                            disabled={loading}
                                        />
                                    ))}
                                </div>
                            </div>
                        </CollapsibleSection>

                        {/* 에러 메시지 */}
                        {error && <div className={`mb-4 ${modalErrorClass}`}>{error}</div>}

                        {/* 버튼 */}
                        <div className="flex gap-3">
                            <button
                                type="button"
                                onClick={close}
                                disabled={loading}
                                className={`flex-1 ${modalSecondaryButtonClass}`}
                            >
                                취소
                            </button>
                            <button type="submit" disabled={loading} className={`flex-1 ${modalPrimaryButtonClass}`}>
                                {loading ? '생성 중...' : '생성'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            {/* 에러 알림 (모달 외부 표시) */}
            {error && <ErrorNotification message={error} onClose={() => setError(null)} duration={5000} />}
        </>
    );
};
