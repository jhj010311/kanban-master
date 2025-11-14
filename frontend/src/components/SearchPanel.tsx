import { useState, useEffect } from 'react';
import { searchService } from '@/services/searchService';
import { labelService } from '@/services/labelService';
import type { CardSearchRequest, CardSearchResult } from '@/types/search';
import type { Label } from '@/types/label';
import { useModalAnimation } from '@/hooks/useModalAnimation';
import { useAuth } from '@/context/AuthContext';
import {
  modalOverlayClass,
  modalPanelClass,
  modalLabelClass,
  modalInputClass,
  modalSecondaryButtonClass,
  modalPrimaryButtonClass,
} from '@/styles/modalStyles';

interface SearchPanelProps {
  boardId: number;
  onClose: () => void;
  onCardSelect: (result: CardSearchResult) => void;
}

const PRIORITIES = ['HIGH', 'MEDIUM', 'LOW'];

export const SearchPanel: React.FC<SearchPanelProps> = ({ boardId, onClose, onCardSelect }) => {
  const { user } = useAuth();
  const [keyword, setKeyword] = useState('');
  const [selectedPriorities, setSelectedPriorities] = useState<string[]>([]);
  const [selectedLabelIds, setSelectedLabelIds] = useState<number[]>([]);
  const [isCompleted, setIsCompleted] = useState<boolean | undefined>(undefined);
  const [overdue, setOverdue] = useState(false);
  const [assignedToMe, setAssignedToMe] = useState(false);
  const [results, setResults] = useState<CardSearchResult[]>([]);
  const [labels, setLabels] = useState<Label[]>([]);
  const [searching, setSearching] = useState(false);
  const [showFilters, setShowFilters] = useState(false);

  const { stage, close } = useModalAnimation(onClose);

  useEffect(() => {
    loadLabels();
  }, [boardId]);

  const loadLabels = async () => {
    try {
      const data = await labelService.getLabels(boardId);
      setLabels(data);
    } catch (err) {
      console.error('Failed to load labels:', err);
    }
  };

  const handleSearch = async () => {
    try {
      setSearching(true);
      const request: CardSearchRequest = {
        keyword: keyword.trim() || undefined,
        priorities: selectedPriorities.length > 0 ? selectedPriorities : undefined,
        labelIds: selectedLabelIds.length > 0 ? selectedLabelIds : undefined,
        isCompleted,
        overdue: overdue || undefined,
        assignees: assignedToMe && user?.email ? [user.email] : undefined,
      };
      const data = await searchService.searchCardsInBoard(boardId, request);
      setResults(data);
    } catch (err) {
      console.error('Search failed:', err);
    } finally {
      setSearching(false);
    }
  };

  const togglePriority = (priority: string) => {
    setSelectedPriorities(prev =>
      prev.includes(priority)
        ? prev.filter(p => p !== priority)
        : [...prev, priority]
    );
  };

  const toggleLabel = (labelId: number) => {
    setSelectedLabelIds(prev =>
      prev.includes(labelId)
        ? prev.filter(id => id !== labelId)
        : [...prev, labelId]
    );
  };

  const clearFilters = () => {
    setKeyword('');
    setSelectedPriorities([]);
    setSelectedLabelIds([]);
    setIsCompleted(undefined);
    setOverdue(false);
    setAssignedToMe(false);
    setResults([]);
  };

  const priorityActiveClasses: { [key: string]: string } = {
    HIGH: 'bg-pastel-pink-200/80 text-pastel-pink-800 ring-1 ring-pastel-pink-300',
    MEDIUM: 'bg-pastel-yellow-200/80 text-pastel-yellow-800 ring-1 ring-pastel-yellow-300',
    LOW: 'bg-pastel-green-200/80 text-pastel-green-800 ring-1 ring-pastel-green-300',
  };

  const priorityBadgeClasses: { [key: string]: string } = {
    HIGH: 'bg-pastel-pink-100 text-pastel-pink-700',
    MEDIUM: 'bg-pastel-yellow-100 text-pastel-yellow-700',
    LOW: 'bg-pastel-green-100 text-pastel-green-700',
  };

  const labelColorMap: { [key: string]: string } = {
    'pastel-blue-500': '#8fb3ff',
    'pastel-pink-500': '#ffb3e6',
    'pastel-green-500': '#b3ffc4',
    'pastel-purple-500': '#d4a5ff',
    'pastel-yellow-500': '#fff4b3',
    'pastel-orange-500': '#ffd4b3',
    'pastel-red-500': '#ffb3b3',
    'pastel-teal-500': '#b3ffe6',
  };

  const handleResultSelect = (result: CardSearchResult) => {
    onCardSelect(result);
    close();
  };

  return (
    <div
      className={modalOverlayClass(stage)}
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          close();
        }
      }}
    >
      <div
        className={modalPanelClass({
          stage,
          maxWidth: 'max-w-4xl',
          padding: 'p-0',
          extra: 'max-h-[85vh] flex flex-col overflow-hidden bg-gradient-to-b from-pastel-blue-50 via-white to-pastel-blue-100/60',
        })}
      >
        {/* Header */}
        <div className="px-6 py-5 border-b border-white/40 bg-gradient-to-r from-white via-pastel-blue-50 to-white">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-xs uppercase tracking-[0.2em] text-pastel-blue-500 font-semibold">Smart Search</p>
              <h2 className="text-2xl font-bold text-pastel-blue-900 mt-1">카드 검색</h2>
            </div>
            <button
              onClick={close}
              className="w-10 h-10 rounded-full hover:bg-pastel-blue-100 text-xl text-pastel-blue-600 transition flex items-center justify-center"
              aria-label="검색 닫기"
              type="button"
            >
              ✕
            </button>
          </div>
        </div>

        {/* Search Input */}
        <div className="px-6 py-4 border-b border-white/40 bg-white/80 flex-shrink-0">
          <div className="flex gap-3 flex-wrap sm:flex-nowrap">
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              placeholder="카드 제목이나 설명으로 검색..."
              className={`${modalInputClass} flex-1`}
            />
            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`flex items-center justify-center gap-2 ${modalSecondaryButtonClass} !px-5 !py-3 ${
                showFilters ? '!bg-white/50 !text-pastel-blue-900 ring-2 ring-white/50' : ''
              }`}
              type="button"
            >
              🔍 필터
            </button>
            <button
              onClick={handleSearch}
              disabled={searching}
              className={`${modalPrimaryButtonClass} !px-6 !py-3`}
              type="button"
            >
              {searching ? '검색 중...' : '검색'}
            </button>
          </div>
        </div>

        {/* Filters */}
        {showFilters && (
          <div className="px-6 py-4 border-b border-white/30 bg-gradient-to-r from-pastel-blue-50 via-pastel-purple-50 to-pastel-cyan-50 space-y-4 flex-shrink-0">
            {/* Priority Filter */}
            <div>
              <label className={modalLabelClass}>
                우선순위
              </label>
              <div className="flex gap-2">
                {PRIORITIES.map((priority) => (
                  <button
                    key={priority}
                    onClick={() => togglePriority(priority)}
                    className={`px-4 py-1.5 rounded-full text-sm font-semibold transition shadow ${selectedPriorities.includes(priority)
                      ? priorityActiveClasses[priority]
                      : 'bg-white/70 text-pastel-blue-600 border border-white/60 hover:bg-white'
                    }`}
                  >
                    {priority}
                  </button>
                ))}
              </div>
            </div>

            {/* Label Filter */}
            {labels.length > 0 && (
            <div>
              <label className={modalLabelClass}>
                라벨
              </label>
                <div className="flex gap-2 flex-wrap">
                  {labels.map((label) => {
                    const bgColor = labelColorMap[label.colorToken] || '#8fb3ff';
                    const isSelected = selectedLabelIds.includes(label.id);
                    return (
                      <button
                        key={label.id}
                        onClick={() => toggleLabel(label.id)}
                        className={`px-3 py-1 rounded text-sm font-medium border-2 transition ${
                          isSelected
                            ? 'border-pastel-blue-600'
                            : 'border-white/40'
                        }`}
                        style={{ backgroundColor: bgColor }}
                      >
                        {label.name}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Status Filters */}
            <div>
              <label className={modalLabelClass}>
                상태
              </label>
              <div className="flex gap-2 flex-wrap">
                <button
                  onClick={() => setIsCompleted(isCompleted === true ? undefined : true)}
                  className={`px-3.5 py-1.5 rounded-full text-sm font-semibold transition ${
                    isCompleted === true
                      ? 'bg-pastel-green-200 text-pastel-green-900 ring-1 ring-pastel-green-300 shadow-sm'
                      : 'bg-pastel-green-50 text-pastel-green-700 border border-pastel-green-100 hover:bg-pastel-green-100/80'
                  }`}
                  type="button"
                >
                  완료
                </button>
                <button
                  onClick={() => setIsCompleted(isCompleted === false ? undefined : false)}
                  className={`px-3.5 py-1.5 rounded-full text-sm font-semibold transition ${
                    isCompleted === false
                      ? 'bg-pastel-blue-200 text-pastel-blue-900 ring-1 ring-pastel-blue-300 shadow-sm'
                      : 'bg-pastel-blue-50 text-pastel-blue-700 border border-pastel-blue-100 hover:bg-pastel-blue-100/80'
                  }`}
                  type="button"
                >
                  미완료
                </button>
                <button
                  onClick={() => setOverdue(!overdue)}
                  className={`px-3.5 py-1.5 rounded-full text-sm font-semibold transition ${
                    overdue
                      ? 'bg-pastel-pink-200 text-pastel-pink-900 ring-1 ring-pastel-pink-300 shadow-sm'
                      : 'bg-pastel-pink-50 text-pastel-pink-700 border border-pastel-pink-100 hover:bg-pastel-pink-100/80'
                  }`}
                  type="button"
                >
                  지연됨
                </button>
                <button
                  onClick={() => setAssignedToMe(!assignedToMe)}
                  disabled={!user}
                  className={`px-3.5 py-1.5 rounded-full text-sm font-semibold transition ${
                    assignedToMe
                      ? 'bg-pastel-purple-200 text-pastel-purple-900 ring-1 ring-pastel-purple-300 shadow-sm'
                      : 'bg-pastel-purple-50 text-pastel-purple-700 border border-pastel-purple-100 hover:bg-pastel-purple-100/80'
                  } ${!user ? 'opacity-50 cursor-not-allowed' : ''}`}
                  type="button"
                  aria-label="나에게 할당된 카드만 보기"
                >
                  나에게 할당됨
                </button>
              </div>
            </div>

            <div className="text-right">
              <button
                onClick={clearFilters}
                className="inline-flex items-center gap-1 text-sm text-pastel-blue-600 hover:text-pastel-blue-800 font-semibold"
                type="button"
              >
                ♻️ 필터 초기화
              </button>
            </div>
          </div>
        )}

        {/* Results */}
        <div className="flex-1 overflow-y-auto p-6 bg-gradient-to-b from-white/90 via-pastel-blue-50 to-pastel-purple-50">
          {results.length === 0 ? (
            <div className="py-16 text-center text-pastel-blue-600 border-2 border-dashed border-pastel-blue-100 rounded-3xl bg-white/70">
              {searching ? '검색 중...' : '검색 결과가 없습니다'}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {results.map((result) => (
                <button
                  key={result.id}
                  onClick={() => handleResultSelect(result)}
                  className="text-left h-full rounded-2xl border border-pastel-blue-200 bg-white/90 shadow-sm hover:shadow-glass hover:border-pastel-blue-500 transition px-4 py-3 flex flex-col gap-3"
                  type="button"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="font-semibold text-pastel-blue-900 truncate">{result.title}</h3>
                        {result.isCompleted && (
                          <span className="text-xs px-2 py-0.5 rounded-full bg-pastel-green-100 text-pastel-green-700 inline-flex whitespace-nowrap">
                            완료
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-pastel-blue-500 truncate">
                        {result.boardName} → {result.columnName}
                      </p>
                    </div>
                    {result.priority && (
                      <span
                        className={`text-xs px-2 py-1 rounded-full font-semibold whitespace-nowrap ${
                          priorityBadgeClasses[result.priority] || 'bg-pastel-blue-100 text-pastel-blue-700'
                        }`}
                      >
                        {result.priority}
                      </span>
                    )}
                  </div>

                  {result.description && (
                    <p className="text-sm text-pastel-blue-600 line-clamp-2">
                      {result.description}
                    </p>
                  )}

                  {result.labels && result.labels.length > 0 && (
                    <div className="flex gap-1 flex-wrap">
                      {result.labels.slice(0, 3).map((label) => {
                        const bgColor = labelColorMap[label.colorToken] || '#8fb3ff';
                        return (
                          <span
                            key={label.id}
                            className="text-xs px-2 py-1 rounded-full font-medium"
                            style={{ backgroundColor: bgColor }}
                          >
                            {label.name}
                          </span>
                        );
                      })}
                      {result.labels.length > 3 && (
                        <span className="text-xs text-pastel-blue-600">
                          +{result.labels.length - 3}
                        </span>
                      )}
                    </div>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
