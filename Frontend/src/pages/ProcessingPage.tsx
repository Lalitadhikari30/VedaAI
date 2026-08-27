import { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import SparkleLoader from '../components/processing/SparkleLoader';
import AppLayout from '../components/layout/AppLayout';
import { useProcessingStatus } from '../hooks/useProcessingStatus';
import { retryAssessment } from '../services/api';
import { AlertCircle, RefreshCw } from 'lucide-react';

export default function ProcessingPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: status, error: fetchError, refetch } = useProcessingStatus(id || null);

  useEffect(() => {
    if (status?.status === 'COMPLETED') {
      navigate(`/assessment/${id}`, { replace: true });
    }
  }, [status, id, navigate]);

  const handleRetry = async () => {
    if (!id) return;
    try {
      await retryAssessment(id);
      refetch();
    } catch {
      refetch();
    }
  };

  return (
    <AppLayout collapsedSidebar={true}>
      {/* Seamless Full Canvas Centered Layout (No enclosing card) */}
      <div className="flex-1 w-full flex flex-col items-center justify-center px-6 py-12 select-none my-auto">
        
        {status?.status === 'FAILED' || fetchError ? (
          <div className="flex flex-col items-center text-center px-6 py-8 max-w-md bg-white rounded-3xl border border-[#E5E5E3] shadow-xs">
            <div className="w-16 h-16 rounded-full bg-[#FBE1DE] text-[#C4392C] flex items-center justify-center mb-4">
              <AlertCircle size={32} />
            </div>
            <h2 className="text-xl md:text-2xl font-bold text-[#1C1C1C] mb-2">
              Processing Failed
            </h2>
            <p className="text-sm text-[#6B6B68] mb-6">
              {status?.message || (fetchError instanceof Error ? fetchError.message : 'An error occurred during extraction. Please retry or check your API keys.')}
            </p>
            <div className="flex items-center gap-3">
              <button
                onClick={handleRetry}
                className="flex items-center gap-2 px-5 py-2.5 rounded-full bg-[#26282E] text-white text-sm font-semibold hover:bg-black transition-all cursor-pointer"
              >
                <RefreshCw size={16} />
                <span>Retry Extraction</span>
              </button>
              <button
                onClick={() => navigate('/')}
                className="px-5 py-2.5 rounded-full border border-[#E5E5E3] text-[#1C1C1C] text-sm font-semibold hover:bg-gray-50 transition-all cursor-pointer"
              >
                Upload New Files
              </button>
            </div>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center">
            <SparkleLoader />
            {status?.progress && (
              <div className="mt-4 text-xs font-semibold text-[#E8623C] bg-[#FBE4D8] px-3.5 py-1 rounded-full animate-pulse">
                {status.progress}
              </div>
            )}
          </div>
        )}

      </div>
    </AppLayout>
  );
}
