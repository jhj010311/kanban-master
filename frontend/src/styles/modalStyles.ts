import type { ModalAnimationStage } from '@/hooks/useModalAnimation';

type ClassValue = string | undefined | null | false;

const joinClasses = (...classes: ClassValue[]) => classes.filter(Boolean).join(' ');

interface ModalPanelOptions {
    stage?: ModalAnimationStage;
    maxWidth?: string;
    padding?: string;
    extra?: string;
    fullHeight?: boolean;
    scrollable?: boolean;
}

export const modalOverlayClass = (stage?: ModalAnimationStage, extra?: string) =>
    joinClasses('modal-overlay', stage && `modal-overlay-${stage}`, 'bg-gradient-pastel/90', 'backdrop-blur-sm', extra);

export const modalPanelClass = ({
    stage,
    maxWidth = 'max-w-md',
    padding = 'p-8',
    extra,
    fullHeight,
    scrollable,
}: ModalPanelOptions = {}) =>
    joinClasses(
        'modal-panel',
        stage && `modal-panel-${stage}`,
        'glass-light',
        'rounded-3xl',
        'shadow-glass-lg',
        'border',
        'border-white/30',
        'w-full',
        fullHeight ? 'h-full' : null,
        scrollable ? 'max-h-[90vh]' : null,
        scrollable ? 'overflow-y-auto' : null,
        'mx-4',
        padding,
        maxWidth,
        extra
    );

export const modalLabelClass = 'block text-sm font-semibold text-pastel-blue-900 mb-2';

export const modalInputClass =
    'w-full px-4 py-3 rounded-xl border border-white/40 bg-white/40 backdrop-blur-sm text-pastel-blue-900 placeholder-pastel-blue-500 focus:outline-none focus:border-pastel-blue-400 focus:ring-2 focus:ring-pastel-blue-300/50 transition';

export const modalTextareaClass = `${modalInputClass} resize-none`;

export const modalSelectClass = modalInputClass;

export const modalSecondaryButtonClass =
    'px-4 py-3 rounded-xl bg-white/30 hover:bg-white/40 backdrop-blur-sm text-pastel-blue-700 font-semibold border border-white/40 transition disabled:opacity-50';

export const modalPrimaryButtonClass =
    'px-4 py-3 rounded-xl bg-gradient-to-r from-pastel-blue-500 to-pastel-cyan-400 text-white font-semibold hover:shadow-lg transition disabled:opacity-50 shadow-glass-sm';

export const modalErrorClass =
    'p-4 rounded-xl bg-pastel-pink-100/70 text-pastel-pink-700 text-sm border border-pastel-pink-200';

export const modalColorButtonClass = (isActive: boolean) =>
    joinClasses(
        'rounded-xl transition-all shadow-glass-sm border border-transparent',
        isActive ? 'ring-3 ring-white/60 scale-105 shadow-lg border-white/60' : 'opacity-70 hover:opacity-100'
    );
