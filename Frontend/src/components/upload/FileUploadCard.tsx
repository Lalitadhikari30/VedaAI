import { useCallback, useRef } from 'react';
import { Upload, X } from 'lucide-react';

interface FileUploadCardProps {
  label: string;
  highlightWord: string;
  file: File | null;
  onFileSelect: (file: File) => void;
  onFileRemove: () => void;
  pageCount?: number;
}

export default function FileUploadCard({
  highlightWord,
  file,
  onFileSelect,
  onFileRemove,
  pageCount = 2,
}: FileUploadCardProps) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      const droppedFile = e.dataTransfer.files[0];
      if (droppedFile && isValidFile(droppedFile)) {
        onFileSelect(droppedFile);
      }
    },
    [onFileSelect]
  );

  const handleClick = () => {
    inputRef.current?.click();
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile && isValidFile(selectedFile)) {
      onFileSelect(selectedFile);
    }
  };

  const isValidFile = (f: File) => {
    const validTypes = ['application/pdf', 'image/jpeg', 'image/png', 'image/jpg'];
    return (validTypes.includes(f.type) || f.name.endsWith('.pdf') || f.name.endsWith('.png') || f.name.endsWith('.jpg')) && f.size <= 15 * 1024 * 1024;
  };

  const formatSize = (bytes: number) => {
    const mb = bytes / (1024 * 1024);
    if (mb >= 1) {
      return `${Math.round(mb)}MB`;
    }
    const kb = Math.round(bytes / 1024);
    return `${kb}KB`;
  };

  // Filled state (matches Screenshot 8 & 5 exactly)
  if (file) {
    const isPdf = file.name.toLowerCase().endsWith('.pdf') || file.type === 'application/pdf';
    return (
      <div className="relative bg-white rounded-[22px] p-6 md:p-7 shadow-xs border border-[#E5E5E3] flex items-center justify-between min-h-[150px] md:min-h-[170px]">
        {/* Remove button pinned to top-right corner, slightly overlapping */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            onFileRemove();
          }}
          title="Remove file"
          className="absolute -top-2.5 -right-2.5 w-7 h-7 rounded-full bg-[#3D3D3D] text-white flex items-center justify-center hover:bg-[#1C1C1C] transition-transform hover:scale-110 shadow-md z-10 cursor-pointer"
        >
          <X size={15} strokeWidth={2.5} />
        </button>

        <div className="flex items-center gap-4.5 min-w-0 pr-4">
          {/* Red PDF Icon badge */}
          <div className="w-13 h-13 rounded-xl bg-[#E8483B] flex flex-col items-center justify-center text-white flex-shrink-0 shadow-xs">
            <span className="text-[12px] font-black tracking-wider leading-none">
              {isPdf ? 'PDF' : 'IMG'}
            </span>
          </div>

          <div className="min-w-0">
            <h3 className="text-sm md:text-base font-bold text-[#1C1C1C] truncate max-w-[220px] md:max-w-[280px]">
              {file.name}
            </h3>
            <p className="text-xs text-[#6B6B68] font-medium mt-1">
              {formatSize(file.size)} • {pageCount} {pageCount === 1 ? 'Page' : 'Pages'}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Empty state (matches Screenshot 9 & 4 exactly)
  return (
    <div
      onClick={handleClick}
      onDrop={handleDrop}
      onDragOver={(e) => e.preventDefault()}
      className="bg-white rounded-[22px] border-2 border-dashed border-[#CCCCCA] hover:border-[#E8623C] p-7 md:p-9 flex flex-col items-center justify-center text-center cursor-pointer transition-all hover:bg-orange-50/20 group min-h-[150px] md:min-h-[175px]"
    >
      <input
        ref={inputRef}
        type="file"
        className="hidden"
        accept=".pdf,.jpg,.jpeg,.png"
        onChange={handleChange}
      />

      {/* Upward Arrow Icon Box */}
      <div className="w-10 h-10 rounded-xl bg-[#F0F0EE] group-hover:bg-[#FBE4D8] flex items-center justify-center text-[#1C1C1C] group-hover:text-[#E8623C] mb-3.5 transition-colors">
        <Upload size={18} strokeWidth={2.4} />
      </div>

      {/* Label with orange highlighted keyword */}
      <div className="text-sm md:text-base font-bold text-[#1C1C1C]">
        Upload{' '}
        <span className="text-[#E8623C] font-bold">
          {highlightWord}
        </span>
      </div>

      <div className="text-xs text-[#6B6B68] font-medium mt-1.5">
        Max 10MB
      </div>
    </div>
  );
}
