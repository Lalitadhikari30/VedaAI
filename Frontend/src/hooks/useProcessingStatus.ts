import { useQuery } from '@tanstack/react-query';
import { getAssessmentStatus } from '../services/api';

export function useProcessingStatus(assessmentId: string | null) {
  return useQuery({
    queryKey: ['assessment-status', assessmentId],
    queryFn: () => getAssessmentStatus(assessmentId!),
    enabled: !!assessmentId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === 'COMPLETED' || status === 'FAILED') {
        return false; // Stop polling
      }
      return 3000; // Poll every 3 seconds
    },
  });
}
