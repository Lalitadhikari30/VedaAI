import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import FileUploadCard from '../components/upload/FileUploadCard';
import TeacherIllustration from '../components/upload/TeacherIllustration';
import AppLayout from '../components/layout/AppLayout';
import { uploadAssessment } from '../services/api';
import clsx from 'clsx';

export default function UploadPage() {
  const navigate = useNavigate();
  const [questionPaper, setQuestionPaper] = useState<File | null>(null);
  const [answerSheet, setAnswerSheet] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = Boolean(questionPaper && answerSheet && !isUploading);

  const handleStartMapping = async () => {
    if (!questionPaper || !answerSheet) return;
    
    setIsUploading(true);
    setError(null);

    try {
      const response = await uploadAssessment(questionPaper, answerSheet);
      navigate(`/processing/${response.assessmentId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed');
      setIsUploading(false);
    }
  };

  return (
    <AppLayout collapsedSidebar={false}>
      {/* Centered Dashboard Workspace */}
      <div className="w-full max-w-[800px] flex flex-col items-center justify-center px-6 py-8 md:py-10 my-auto">
        
        {/* Two-tone Main Title */}
        <div className="text-center mb-2 md:mb-3">
          {/* Desktop Heading with Peach Highlight */}
          <h1 className="hidden md:block text-3xl lg:text-[34px] font-black tracking-tight text-[#1C1C1C]">
            <span>Upload </span>
            <span className="inline-block relative">
              <span className="relative z-10 text-[#E8623C] underline decoration-[#E8623C]/50 decoration-2 underline-offset-4 px-2 py-0.5">
                Question Paper & Answer Sheets
              </span>
              <span 
                className="absolute inset-0 bg-[#FBE4D8] rounded-xl -z-0"
                style={{ top: '1px', bottom: '1px', left: '-2px', right: '-2px' }}
              />
            </span>
          </h1>

          {/* Mobile Heading */}
          <h1 className="md:hidden text-2xl font-black tracking-tight text-[#1C1C1C] leading-snug">
            <div>Upload <span className="text-[#E8623C] underline decoration-[#E8623C]/50 decoration-2 underline-offset-3">Question Paper</span></div>
            <div>& Answer Sheets</div>
          </h1>

          {/* Subtitle */}
          <p className="text-sm md:text-base text-[#6B6B68] font-medium mt-2.5">
            Upload both files to get started
          </p>
        </div>

        {/* Teacher Illustration */}
        <div className="my-1 md:my-2">
          <TeacherIllustration />
        </div>

        {/* Dual Upload Cards Grid - Balanced and Centered */}
        <div className="w-full grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-5 mt-2 md:mt-3 mb-6 md:mb-8">
          <FileUploadCard
            label="Upload Question Paper"
            highlightWord="Question Paper"
            file={questionPaper}
            onFileSelect={setQuestionPaper}
            onFileRemove={() => setQuestionPaper(null)}
            pageCount={2}
          />
          <FileUploadCard
            label="Upload Answer Sheet"
            highlightWord="Answer Sheet"
            file={answerSheet}
            onFileSelect={setAnswerSheet}
            onFileRemove={() => setAnswerSheet(null)}
            pageCount={6}
          />
        </div>

        {/* Error Notification */}
        {error && (
          <div className="mb-5 px-5 py-3 bg-[#FBE1DE] text-[#C4392C] rounded-2xl text-sm font-semibold border border-red-200 shadow-xs max-w-md text-center">
            {error}
          </div>
        )}

        {/* Start Mapping Button */}
        <button
          onClick={handleStartMapping}
          disabled={!canSubmit}
          className={clsx(
            'flex items-center justify-center gap-2.5 px-8 py-3 rounded-full font-bold text-sm md:text-base tracking-wide transition-all shadow-xs select-none',
            canSubmit
              ? 'bg-[#1C1C1C] text-white hover:bg-black hover:scale-[1.02] cursor-pointer active:scale-[0.98]'
              : 'bg-[#B8B8B5] text-[#2E2E2C] cursor-not-allowed opacity-95'
          )}
        >
          <span>{isUploading ? 'Uploading Files...' : 'Start Mapping'}</span>
          <ArrowRight size={17} strokeWidth={2.6} />
        </button>

        {/* Verbatim Helper Text */}
        <p className="text-[#6B6B68] text-xs md:text-sm text-center mt-3 md:mt-3.5 max-w-md font-medium">
          Once both files are uploaded, you'll able to map answers with questions
        </p>

      </div>
    </AppLayout>
  );
}
