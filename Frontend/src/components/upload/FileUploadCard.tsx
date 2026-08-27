import { useCallback, useRef, useState, useEffect } from 'react';
import { Upload, X } from 'lucide-react';
import { getFilePageCount } from '../../services/pdfUtils';

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
  pageCount: defaultPageCount,
}: FileUploadCardProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [actualPageCount, setActualPageCount] = useState<number>(defaultPageCount || 1);

  // Dynamically compute exact page count whenever a new file is attached
  useEffect(() => {
    if (!file) {
      setActualPageCount(1);
      return;
    }

    let isMounted = true;
    getFilePageCount(file).then((count) => {
      if (isMounted) {
        setActualPageCount(count);
      }
    });

    return () => {
      isMounted = false;
    };
  }, [file]);

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
      return `${Math.round(mb * 10) / 10}MB`;
    }
    const kb = Math.round(bytes / 1024);
    return `${kb}KB`;
  };

  // Filled state (With persistent dashed/dotted border & dynamic page count)
  if (file) {
    const isPdf = file.name.toLowerCase().endsWith('.pdf') || file.type === 'application/pdf';
    return (
      <div className="relative bg-white rounded-[20px] border-2 border-dashed border-[#CBCBC8] p-4 md:p-5 shadow-xs flex items-center justify-center min-h-[105px] md:min-h-[115px] transition-all">
        {/* Remove button pinned to top-right corner */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            onFileRemove();
          }}
          title="Remove file"
          className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-[#3D3D3D] text-white flex items-center justify-center hover:bg-[#1C1C1C] transition-transform hover:scale-110 shadow-md z-10 cursor-pointer"
        >
          <X size={13} strokeWidth={2.5} />
        </button>

        {/* Centered Content Block */}
        <div className="flex items-center justify-center gap-4 w-full px-2">
          {/* Red PDF Icon badge */}
          <div className="w-11 h-11 md:w-12 md:h-12 rounded-xl bg-[#E8483B] flex flex-col items-center justify-center text-white flex-shrink-0 shadow-2xs">
            <span className="text-[11px] font-black tracking-wider leading-none">
              {isPdf ? 'PDF' : 'IMG'}
            </span>
          </div>

          <div className="min-w-0 max-w-[220px] text-left">
            <h3 className="text-[14px] md:text-[15px] font-bold text-[#1C1C1C] truncate leading-snug" title={file.name}>
              {file.name}
            </h3>
            <p className="text-xs text-[#6B6B68] font-medium mt-0.5">
              {formatSize(file.size)} • {actualPageCount} {actualPageCount === 1 ? 'Page' : 'Pages'}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Empty state (Centered with dashed border)
  return (
    <div
      onClick={handleClick}
      onDrop={handleDrop}
      onDragOver={(e) => e.preventDefault()}
      className="bg-white rounded-[20px] border-2 border-dashed border-[#CBCBC8] hover:border-[#E8623C] p-4 md:p-5 flex flex-col items-center justify-center text-center cursor-pointer transition-all hover:bg-orange-50/15 group min-h-[105px] md:min-h-[115px]"
    >
      <input
        ref={inputRef}
        type="file"
        className="hidden"
        accept=".pdf,.jpg,.jpeg,.png"
        onChange={handleChange}
      />

      {/* Upward Arrow Icon Box */}
      <div className="w-9 h-9 rounded-xl bg-[#F0F0EE] group-hover:bg-[#FBE4D8] flex items-center justify-center text-[#1C1C1C] group-hover:text-[#E8623C] mb-2 transition-colors shadow-2xs">
        <Upload size={17} strokeWidth={2.4} />
      </div>

      {/* Label with orange highlighted keyword */}
      <div className="text-[14px] md:text-[14.5px] font-bold text-[#1C1C1C]">
        Upload{' '}
        <span className="text-[#E8623C] font-bold">
          {highlightWord}
        </span>
      </div>

      <div className="text-[11px] text-[#8A8A87] font-semibold mt-0.5">
        Max 10MB
      </div>
    </div>
  );
}
