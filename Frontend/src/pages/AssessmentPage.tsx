import { useState, useMemo, useEffect, useRef, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import QuestionList from '../components/assessment/QuestionList';
import PdfViewer from '../components/pdf/PdfViewer';
import { useAssessment } from '../hooks/useAssessment';
import { getAnswerSheetUrl } from '../services/api';
import clsx from 'clsx';

export default function AssessmentPage() {
  const { id } = useParams<{ id: string }>();
  const { data: assessment, isLoading, error } = useAssessment(id || null);
  const [selectedQuestionId, setSelectedQuestionId] = useState<string | null>(null);
  const [mobileTab, setMobileTab] = useState<'questions' | 'answers'>('questions');

  // Resizable Divider State
  const [leftWidthPercent, setLeftWidthPercent] = useState<number>(45);
  const [isDragging, setIsDragging] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // Auto-select Q2 or first question with answer
  useEffect(() => {
    if (assessment?.questions && assessment.questions.length > 0 && !selectedQuestionId) {
      const q2 = assessment.questions.find((q) => q.number === '2' || q.number === 'Q2');
      const firstWithAnswer = assessment.questions.find((q) => q.answer && q.answer.regions && q.answer.regions.length > 0);
      setSelectedQuestionId(q2?.id || firstWithAnswer?.id || assessment.questions[0].id);
    }
  }, [assessment, selectedQuestionId]);

  const selectedQuestion = useMemo(() => {
    if (!assessment || !selectedQuestionId) return null;
    return assessment.questions.find((q) => q.id === selectedQuestionId) || null;
  }, [assessment, selectedQuestionId]);

  const highlightRegions = useMemo(() => {
    if (!selectedQuestion?.answer?.regions) return [];
    return selectedQuestion.answer.regions;
  }, [selectedQuestion]);

  const targetPage = useMemo(() => {
    if (highlightRegions.length === 0) return 1;
    return highlightRegions[0].page;
  }, [highlightRegions]);

  const questionLabel = useMemo(() => {
    if (!selectedQuestion) return null;
    const num = selectedQuestion.parentNumber || selectedQuestion.number;
    const sub = selectedQuestion.subPart ? `(${selectedQuestion.subPart})` : '';
    return `Q${num}${sub}`;
  }, [selectedQuestion]);

  // Compute score tone for highlight overlay
  const scoreTone = useMemo((): 'green' | 'amber' | 'rose' => {
    if (!selectedQuestion?.grading) return 'green';
    const score = selectedQuestion.grading.score;
    const max = selectedQuestion.grading.maxScore || 5;
    if (score === 0 && max > 0) return 'rose';
    const ratio = max > 0 ? score / max : 0;
    if (ratio < 0.8) return 'amber';
    return 'green';
  }, [selectedQuestion]);

  const answerSheetUrl = id ? getAnswerSheetUrl(id) : '';

  // Resizer Drag Handlers
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  useEffect(() => {
    if (!isDragging) return;

    const handleMouseMove = (e: MouseEvent) => {
      if (!containerRef.current) return;
      const rect = containerRef.current.getBoundingClientRect();
      const newLeftPx = e.clientX - rect.left;
      const containerWidth = rect.width;

      // Min width constraints (left min 360px, right min 400px)
      const minLeftPercent = (360 / containerWidth) * 100;
      const maxLeftPercent = ((containerWidth - 400) / containerWidth) * 100;

      const currentPercent = (newLeftPx / containerWidth) * 100;
      const clamped = Math.min(Math.max(currentPercent, minLeftPercent), maxLeftPercent);
      setLeftWidthPercent(clamped);
    };

    const handleMouseUp = () => {
      setIsDragging(false);
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isDragging]);

  if (isLoading) {
    return (
      <AppLayout collapsedSidebar={true}>
        <div className="flex-1 flex flex-col items-center justify-center p-8 text-slate-500">
          <div className="w-10 h-10 border-3 border-[#F27448] border-t-transparent rounded-full animate-spin mb-4" />
          <p className="text-sm font-semibold">Loading Assessment Results...</p>
        </div>
      </AppLayout>
    );
  }

  if (error || !assessment) {
    return (
      <AppLayout collapsedSidebar={true}>
        <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
          <div className="w-14 h-14 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mb-4 text-2xl font-bold">
            ✕
          </div>
          <h2 className="text-xl font-bold text-slate-800 mb-2">Failed to Load Assessment</h2>
          <p className="text-sm text-slate-500 max-w-md">
            {error instanceof Error ? error.message : 'The assessment session could not be found or has expired.'}
          </p>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout collapsedSidebar={true}>
      {/* Desktop Split View with Resizable Divider */}
      <div 
        ref={containerRef}
        className={clsx(
          'hidden lg:flex flex-1 h-[calc(100vh-3.5rem)] overflow-hidden relative',
          isDragging && 'select-none cursor-col-resize'
        )}
      >
        {/* Left Extracted Questions Panel */}
        <div 
          style={{ width: `${leftWidthPercent}%` }}
          className="flex flex-col bg-[#F4F5F8] overflow-hidden flex-shrink-0"
        >
          <QuestionList
            questions={assessment.questions}
            selectedQuestionId={selectedQuestionId}
            onSelectQuestion={setSelectedQuestionId}
          />
        </div>

        {/* Resizable Divider Handle (Section 3.5) */}
        <div
          onMouseDown={handleMouseDown}
          className="w-2 hover:w-2.5 bg-[#E8EAEE] hover:bg-[#D5D8DF] transition-all flex items-center justify-center cursor-col-resize z-20 group flex-shrink-0 relative select-none"
          title="Drag to resize panels"
        >
          {/* Centered Pill Grip Indicator */}
          <div className="w-1 h-11 rounded-full bg-slate-400 group-hover:bg-slate-600 transition-colors shadow-2xs" />
        </div>

        {/* Right Answer Sheet Panel */}
        <div 
          style={{ width: `${100 - leftWidthPercent}%` }}
          className="flex flex-col bg-[#484C51] overflow-hidden flex-1"
        >
          <PdfViewer
            fileUrl={answerSheetUrl}
            regions={highlightRegions}
            questionLabel={questionLabel}
            scoreTone={scoreTone}
            targetPage={targetPage}
          />
        </div>
      </div>

      {/* Mobile Segmented View */}
      <div className="lg:hidden flex flex-col flex-1 h-[calc(100vh-3.5rem)] bg-[#F4F5F8]">
        {/* Top Segmented Control */}
        <div className="p-3 bg-white border-b border-slate-200 flex-shrink-0">
          <div className="flex bg-slate-100 p-1 rounded-full gap-1 max-w-sm mx-auto">
            <button
              onClick={() => setMobileTab('questions')}
              className={clsx(
                'flex-1 py-2 px-4 rounded-full text-xs font-bold transition-all text-center cursor-pointer',
                mobileTab === 'questions'
                  ? 'bg-slate-900 text-white shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              )}
            >
              Questions
            </button>
            <button
              onClick={() => setMobileTab('answers')}
              className={clsx(
                'flex-1 py-2 px-4 rounded-full text-xs font-bold transition-all text-center cursor-pointer',
                mobileTab === 'answers'
                  ? 'bg-slate-900 text-white shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              )}
            >
              Answer Sheet
            </button>
          </div>
        </div>

        {/* Active Tab Content */}
        <div className="flex-1 overflow-hidden">
          {mobileTab === 'questions' ? (
            <div className="h-full overflow-y-auto">
              <QuestionList
                questions={assessment.questions}
                selectedQuestionId={selectedQuestionId}
                onSelectQuestion={(qId) => {
                  setSelectedQuestionId(qId);
                  setMobileTab('answers');
                }}
              />
            </div>
          ) : (
            <div className="h-full">
              <PdfViewer
                fileUrl={answerSheetUrl}
                regions={highlightRegions}
                questionLabel={questionLabel}
                scoreTone={scoreTone}
                targetPage={targetPage}
              />
            </div>
          )}
        </div>
      </div>
    </AppLayout>
  );
}
