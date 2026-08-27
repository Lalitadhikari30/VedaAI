import clsx from 'clsx';
import type { GradingResult } from '../../types/assessment';

interface StatusBadgeProps {
  grading?: GradingResult | null;
  score?: number;
  maxScore?: number;
}

export default function StatusBadge({ grading, score, maxScore }: StatusBadgeProps) {
  const currentScore = grading?.score ?? score ?? 0;
  const currentMax = grading?.maxScore ?? maxScore ?? 5;

  const ratio = currentMax > 0 ? currentScore / currentMax : 0;
  
  let colorStyle = 'bg-[#E8F8EE] text-[#16A34A] border border-[#BBF7D0]/80'; // Green (>= 80%)
  
  if (currentScore === 0 && currentMax > 0) {
    colorStyle = 'bg-[#FFF1F2] text-[#E11D48] border border-[#FECDD3]/80'; // Rose (0)
  } else if (ratio < 0.8) {
    colorStyle = 'bg-[#FFF7ED] text-[#EA580C] border border-[#FED7AA]/80'; // Amber (< 80%)
  }

  return (
    <span
      className={clsx(
        'inline-flex items-center justify-center px-2.5 py-0.5 rounded-full text-[12px] font-bold tracking-tight whitespace-nowrap select-none shadow-2xs',
        colorStyle
      )}
    >
      {currentScore}/{currentMax}
    </span>
  );
}
