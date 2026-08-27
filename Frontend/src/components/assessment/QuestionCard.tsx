import { ChevronDown, ChevronUp } from 'lucide-react';
import clsx from 'clsx';
import StatusBadge from './StatusBadge';
import AIFeedback from './AIFeedback';
import type { QuestionResult } from '../../types/assessment';

interface QuestionCardProps {
  question: QuestionResult;
  isSelected: boolean;
  isExpanded: boolean;
  onSelect: () => void;
  onToggleExpand: () => void;
}

export default function QuestionCard({
  question,
  isSelected,
  isExpanded,
  onSelect,
  onToggleExpand,
}: QuestionCardProps) {
  const hasSubPart = Boolean(question.subPart);

  return (
    <div
      onClick={onSelect}
      className={clsx(
        'bg-white rounded-2xl p-4 transition-all duration-150 cursor-pointer border text-left',
        isSelected
          ? 'border-[#F27448] ring-2 ring-[#F27448]/35 shadow-sm'
          : 'border-slate-200/80 shadow-2xs hover:border-slate-300 hover:shadow-xs'
      )}
    >
      <div className="flex items-start gap-3.5">
        {/* Number Badge / Subpart */}
        <div className="flex items-center gap-1.5 flex-shrink-0 pt-0.5 select-none">
          {/* Main Parent Number Circle */}
          <div
            className={clsx(
              'w-7 h-7 rounded-full text-xs font-bold flex items-center justify-center transition-colors shadow-2xs',
              isSelected
                ? 'bg-[#F27448] text-white'
                : 'bg-slate-800 text-white'
            )}
          >
            {question.parentNumber || question.number}
          </div>

          {/* Subpart Letter Pill Badge */}
          {hasSubPart && (
            <div className="h-6 px-1.5 rounded-full bg-slate-100 border border-slate-200 text-slate-700 text-xs font-bold flex items-center justify-center">
              {question.subPart?.toLowerCase().includes('.') ? question.subPart : `${question.subPart}.`}
            </div>
          )}
        </div>

        {/* Middle: Question Text & Expanded AI Feedback */}
        <div className="flex-1 min-w-0 pr-1">
          <p className="text-[13.5px] text-slate-800 font-normal leading-relaxed">
            {question.text}
          </p>

          {/* Expanded AI Feedback Sub-block */}
          {isExpanded && question.grading?.feedback && (
            <AIFeedback feedback={question.grading.feedback} />
          )}
        </div>

        {/* Right: Score Pill Badge + Chevron Toggle */}
        <div className="flex items-center gap-2 flex-shrink-0 pt-0.5">
          <StatusBadge grading={question.grading} maxScore={question.grading?.maxScore || 5} />
          
          <button
            onClick={(e) => {
              e.stopPropagation();
              onToggleExpand();
            }}
            title={isExpanded ? 'Collapse' : 'Expand'}
            className="w-6 h-6 flex items-center justify-center text-slate-400 hover:text-slate-700 transition-colors cursor-pointer"
          >
            {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          </button>
        </div>
      </div>
    </div>
  );
}
