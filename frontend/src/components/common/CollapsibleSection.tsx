import React, { useId, useState } from 'react';

interface CollapsibleSectionProps {
    title: string;
    children: React.ReactNode;
    summary?: React.ReactNode;
    defaultOpen?: boolean;
    className?: string;
}

const ChevronIcon: React.FC<{ isOpen: boolean }> = ({ isOpen }) => (
    <svg
        className={`w-4 h-4 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`}
        viewBox="0 0 20 20"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
    >
        <path
            d="M5 7.5L10 12.5L15 7.5"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        />
    </svg>
);

export const CollapsibleSection: React.FC<CollapsibleSectionProps> = ({
    title,
    summary,
    children,
    defaultOpen = false,
    className = '',
}) => {
    const [isOpen, setIsOpen] = useState(defaultOpen);
    const contentId = useId();

    return (
        <div className={className}>
            <button
                type="button"
                onClick={() => setIsOpen((prev) => !prev)}
                aria-expanded={isOpen}
                aria-controls={contentId}
                className="w-full flex items-center justify-between gap-4 rounded-2xl border border-white/30 bg-white/50 px-4 py-3 text-left text-pastel-blue-900 shadow-sm transition-colors hover:bg-white/70 focus:outline-none focus-visible:ring-2 focus-visible:ring-pastel-blue-300"
            >
                <div className="flex-1">
                    <div className="text-sm font-semibold">{title}</div>
                    {summary && <div className="text-xs text-pastel-blue-500 mt-0.5">{summary}</div>}
                </div>
                <div className="flex items-center gap-2 text-xs font-medium text-pastel-blue-500">
                    <span>{isOpen ? '접기' : '펼치기'}</span>
                    <ChevronIcon isOpen={isOpen} />
                </div>
            </button>
            <div
                id={contentId}
                aria-hidden={!isOpen}
                className={`overflow-hidden transition-[max-height] duration-300 ease-in-out ${
                    isOpen ? 'max-h-[720px]' : 'max-h-0'
                }`}
            >
                <div
                    className={`pt-4 transition-all duration-300 ease-in-out ${
                        isOpen ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-1'
                    }`}
                >
                    {children}
                </div>
            </div>
        </div>
    );
};
