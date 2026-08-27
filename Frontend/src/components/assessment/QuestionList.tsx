import { useState } from 'react';
import QuestionCard from './QuestionCard';
import type { QuestionResult } from '../../types/assessment';

interface QuestionListProps {
  questions: QuestionResult[];
  selectedQuestionId: string | null;
  onSelectQuestion: (questionId: string) => void;
}

export default function QuestionList({
  questions,
  selectedQuestionId,
  onSelectQuestion,
}: QuestionListProps) {
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [allExpanded, setAllExpanded] = useState(false);

  const toggleExpand = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const handleExpandAll = () => {
    if (allExpanded) {
      setExpandedIds(new Set());
    } else {
      setExpandedIds(new Set(questions.map((q) => q.id)));
    }
    setAllExpanded(!allExpanded);
  };

  return (
    <div className="flex flex-col h-full bg-[#F4F5F8] overflow-hidden">
      {/* Top Header Row */}
      <div className="flex items-center justify-between px-5 py-4 bg-[#F4F5F8] sticky top-0 z-10 select-none">
        <h2 className="text-[15px] font-bold text-slate-800 tracking-tight">
          Extracted Questions (from question paper)
        </h2>
        <button
          onClick={handleExpandAll}
          className="px-3.5 py-1 rounded-full bg-white border border-slate-200 hover:border-slate-300 text-xs font-semibold text-slate-700 shadow-2xs transition-all cursor-pointer hover:bg-slate-50"
        >
          {allExpanded ? 'Collapse All' : 'Expand All'}
        </button>
      </div>

      {/* Scrollable Question Cards List */}
      <div className="flex-1 overflow-y-auto px-5 pb-6 space-y-3">
        {questions.map((question) => (
          <QuestionCard
            key={question.id}
            question={question}
            isSelected={selectedQuestionId === question.id}
            isExpanded={expandedIds.has(question.id) || selectedQuestionId === question.id}
            onSelect={() => onSelectQuestion(question.id)}
            onToggleExpand={() => toggleExpand(question.id)}
          />
        ))}

        {questions.length === 0 && (
          <div className="text-center py-16 text-slate-500 text-sm">
            No questions extracted.
          </div>
        )}
      </div>
    </div>
  );
}
