import { useQuery } from '@tanstack/react-query';
import { getAssessmentResult } from '../services/api';

export function useAssessment(assessmentId: string | null) {
  return useQuery({
    queryKey: ['assessment-result', assessmentId],
    queryFn: () => getAssessmentResult(assessmentId!),
    enabled: !!assessmentId,
    staleTime: Infinity,
  });
}
