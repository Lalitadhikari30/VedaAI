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
      <div className="w-full max-w-[760px] mx-auto flex flex-col items-center justify-center px-6 py-6 md:py-10 my-auto">
        
        {/* Main Title Section */}
        <div className="text-center mb-2" style={{ marginTop: '16px' }}>
          <h1 className="text-2xl md:text-3xl lg:text-[34px] font-black tracking-tight text-[#1C1C1C] flex items-center justify-center flex-wrap gap-2.5 leading-tight">
            <span>Upload</span>
            <span className="inline-flex items-center px-4 py-1.5 rounded-2xl bg-[#FBE4D8] text-[#E8623C] font-black underline decoration-[#E8623C]/40 decoration-2 underline-offset-4 shadow-2xs">
              Question Paper & Answer Sheets
            </span>
          </h1>

          {/* Subtitle */}
          <p className="text-sm md:text-[15px] text-[#6B6B68] font-medium mt-2.5">
            Upload both files to get started
          </p>
        </div>

        {/* Teacher Illustration */}
        <div className="my-2 md:my-3">
          <TeacherIllustration />
        </div>

        {/* Dual Upload Cards Grid (Shifted slightly down from illustration) */}
        <div className="w-full grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-5" style={{ marginTop: '20px' }}>
          <FileUploadCard
            label="Upload Question Paper"
            highlightWord="Question Paper"
            file={questionPaper}
            onFileSelect={setQuestionPaper}
            onFileRemove={() => setQuestionPaper(null)}
          />
          <FileUploadCard
            label="Upload Answer Sheet"
            highlightWord="Answer Sheet"
            file={answerSheet}
            onFileSelect={setAnswerSheet}
            onFileRemove={() => setAnswerSheet(null)}
          />
        </div>

        {/* Error Notification */}
        {error && (
          <div className="mt-5 mb-2 px-5 py-3 bg-[#FBE1DE] text-[#C4392C] rounded-2xl text-sm font-semibold border border-red-200 shadow-xs max-w-md text-center">
            {error}
          </div>
        )}

        {/* Start Mapping Action Section */}
        <div className="flex flex-col items-center w-full" style={{ marginTop: '34px', marginBottom: '20px' }}>
          <button
            onClick={handleStartMapping}
            disabled={!canSubmit}
            className={clsx(
              'h-[50px] px-8 min-w-[210px] rounded-full font-semibold text-[15.5px] tracking-tight transition-all flex items-center justify-center gap-3 select-none shadow-md group',
              canSubmit
                ? 'bg-[#2B2C30] text-white border border-[#484950] hover:bg-[#202124] hover:scale-[1.02] active:scale-[0.98] cursor-pointer shadow-black/20'
                : 'bg-[#E5E6EB] text-[#8C90A0] border border-[#D5D7E0] cursor-not-allowed shadow-none'
            )}
          >
            <span>{isUploading ? 'Uploading Files...' : 'Start Mapping'}</span>
            <ArrowRight size={19} strokeWidth={2.2} className="text-white transition-transform group-hover:translate-x-0.5" />
          </button>

          {/* Helper Text with subtle 10px spacing */}
          <p 
            className="text-[#6B6B68] text-xs md:text-[13px] text-center font-medium max-w-md"
            style={{ marginTop: '10px' }}
          >
            Once both files are uploaded, you'll able to map answers with questions
          </p>
        </div>

      </div>
    </AppLayout>
  );
}
