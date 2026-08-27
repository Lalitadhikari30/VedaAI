interface AIFeedbackProps {
  feedback: string;
}

export default function AIFeedback({ feedback }: AIFeedbackProps) {
  if (!feedback) return null;

  return (
    <div className="mt-3.5 p-3.5 bg-slate-50/90 rounded-xl border border-slate-100 text-left">
      <div className="text-xs font-bold text-slate-800 tracking-tight">
        AI Feedback
      </div>
      <p className="text-[12.5px] text-slate-600 leading-relaxed font-normal mt-1">
        {feedback}
      </p>
    </div>
  );
}
