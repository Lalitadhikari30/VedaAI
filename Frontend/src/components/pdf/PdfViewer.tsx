import { useState, useRef, useCallback, useEffect, useMemo } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import { Minus, Plus, ChevronLeft, ChevronRight } from 'lucide-react';
import type { AnswerRegion } from '../../types/assessment';
import clsx from 'clsx';

// Set up PDF.js worker
pdfjs.GlobalWorkerOptions.workerSrc = `https://unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

interface PdfViewerProps {
  fileUrl: string;
  regions: AnswerRegion[];
  questionLabel: string | null;
  scoreTone?: 'green' | 'amber' | 'rose';
  targetPage?: number;
  isImage?: boolean;
}

export default function PdfViewer({
  fileUrl,
  regions,
  questionLabel,
  scoreTone = 'green',
  targetPage = 1,
  isImage = false,
}: PdfViewerProps) {
  const [numPages, setNumPages] = useState(isImage ? 1 : 0);
  const [currentPage, setCurrentPage] = useState(1);
  const [zoom, setZoom] = useState(100);
  const [isPdfFailed, setIsPdfFailed] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const pageRefs = useRef<Map<number, HTMLDivElement>>(new Map());

  const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
    setNumPages(numPages);
    setIsPdfFailed(false);
  };

  // Scroll to target page when it changes
  useEffect(() => {
    if (targetPage && targetPage >= 1) {
      const pageEl = pageRefs.current.get(targetPage);
      if (pageEl) {
        pageEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
        setCurrentPage(targetPage);
      }
    }
  }, [targetPage]);

  // Track current visible page via scroll
  const handleScroll = useCallback(() => {
    if (!containerRef.current) return;
    const container = containerRef.current;
    const scrollTop = container.scrollTop + 80;

    for (let page = numPages; page >= 1; page--) {
      const pageEl = pageRefs.current.get(page);
      if (pageEl && pageEl.offsetTop <= scrollTop) {
        setCurrentPage(page);
        break;
      }
    }
  }, [numPages]);

  const handleZoomIn = () => setZoom((z) => Math.min(z + 15, 200));
  const handleZoomOut = () => setZoom((z) => Math.max(z - 15, 50));
  
  const goToPrevPage = () => {
    const prev = Math.max(1, currentPage - 1);
    const pageEl = pageRefs.current.get(prev);
    pageEl?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    setCurrentPage(prev);
  };

  const goToNextPage = () => {
    const next = Math.min(numPages || 1, currentPage + 1);
    const pageEl = pageRefs.current.get(next);
    pageEl?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    setCurrentPage(next);
  };

  const pageWidth = useMemo(() => {
    const baseWidth = 640;
    return Math.round((baseWidth * zoom) / 100);
  }, [zoom]);

  // Group regions by page number
  const regionsByPage = useMemo(() => {
    const map = new Map<number, AnswerRegion[]>();
    for (const region of regions) {
      const page = region.page || 1;
      const existing = map.get(page) || [];
      existing.push(region);
      map.set(page, existing);
    }
    return map;
  }, [regions]);

  // Color styles based on score tone
  const highlightStyles = useMemo(() => {
    if (scoreTone === 'amber') {
      return {
        box: 'border-2 border-[#F59E0B] bg-[#F59E0B]/12',
        badge: 'bg-[#F59E0B] text-white',
      };
    }
    if (scoreTone === 'rose') {
      return {
        box: 'border-2 border-[#F43F5E] bg-[#F43F5E]/12',
        badge: 'bg-[#F43F5E] text-white',
      };
    }
    return {
      box: 'border-2 border-[#22C55E] bg-[#22C55E]/12',
      badge: 'bg-[#22C55E] text-white',
    };
  }, [scoreTone]);

  return (
    <div className="flex flex-col h-full bg-[#484C51] overflow-hidden select-none">
      {/* Top Header Bar matching Image 2 */}
      <div className="h-12 px-5 bg-[#383B40] text-white flex items-center justify-between flex-shrink-0 z-10 border-b border-white/5">
        <span className="text-[13.5px] font-bold tracking-tight text-slate-200">
          Answer Sheet
        </span>

        {/* Center/Right: Zoom & Pagination Pill Controls */}
        <div className="flex items-center gap-3">
          {/* Zoom Controls Pill */}
          <div className="flex items-center gap-1 bg-[#282A2E] px-2.5 py-1 rounded-lg border border-white/10 shadow-xs">
            <button 
              onClick={handleZoomOut} 
              title="Zoom Out"
              className="w-5 h-5 rounded flex items-center justify-center text-slate-300 hover:text-white hover:bg-white/10 transition-colors cursor-pointer"
            >
              <Minus size={12} strokeWidth={2.5} />
            </button>
            <span className="text-[11.5px] font-bold w-11 text-center text-slate-200 select-none">
              {zoom}%
            </span>
            <button 
              onClick={handleZoomIn} 
              title="Zoom In"
              className="w-5 h-5 rounded flex items-center justify-center text-slate-300 hover:text-white hover:bg-white/10 transition-colors cursor-pointer"
            >
              <Plus size={12} strokeWidth={2.5} />
            </button>
          </div>

          {/* Pagination Controls Pill */}
          <div className="flex items-center gap-1 bg-[#282A2E] px-2.5 py-1 rounded-lg border border-white/10 shadow-xs">
            <button 
              onClick={goToPrevPage} 
              disabled={currentPage <= 1}
              title="Previous Page"
              className="w-5 h-5 rounded flex items-center justify-center text-slate-300 hover:text-white hover:bg-white/10 transition-colors disabled:opacity-30 cursor-pointer disabled:cursor-not-allowed"
            >
              <ChevronLeft size={14} strokeWidth={2.5} />
            </button>
            <span className="text-[11.5px] font-bold px-1 text-slate-200 whitespace-nowrap select-none">
              Page {currentPage} of {numPages || 1}
            </span>
            <button 
              onClick={goToNextPage} 
              disabled={currentPage >= (numPages || 1)}
              title="Next Page"
              className="w-5 h-5 rounded flex items-center justify-center text-slate-300 hover:text-white hover:bg-white/10 transition-colors disabled:opacity-30 cursor-pointer disabled:cursor-not-allowed"
            >
              <ChevronRight size={14} strokeWidth={2.5} />
            </button>
          </div>
        </div>
      </div>

      {/* Answer Sheet Document Canvas Scroll Area */}
      <div
        ref={containerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-auto p-6 md:p-8 flex flex-col items-center bg-[#484C51]"
      >
        {isPdfFailed || isImage ? (
          // Direct Scanned Image Viewer
          <div
            ref={(el) => {
              if (el) pageRefs.current.set(1, el);
            }}
            className="relative shadow-2xl rounded-2xl overflow-hidden bg-white mb-8 border border-white/10 transition-all"
            style={{ width: `${pageWidth}px` }}
          >
            <img
              src={fileUrl}
              alt="Answer Sheet Page 1"
              className="w-full h-auto block pointer-events-none"
            />

            {/* Highlights for single image */}
            {regionsByPage.get(1)?.map((region, idx) => (
              <div
                key={idx}
                className={clsx(
                  'absolute rounded-xl pointer-events-none transition-all duration-150',
                  highlightStyles.box
                )}
                style={{
                  left: `${region.x * 100}%`,
                  top: `${region.y * 100}%`,
                  width: `${region.width * 100}%`,
                  height: `${region.height * 100}%`,
                }}
              >
                {/* Score-toned Badge pinned to top-left of box */}
                <div
                  className={clsx(
                    'absolute -top-3.5 left-0 px-2 py-0.5 rounded-md text-[11px] font-black tracking-tight shadow-sm leading-tight select-none',
                    highlightStyles.badge
                  )}
                >
                  {questionLabel || 'Q'}
                </div>
              </div>
            ))}
          </div>
        ) : (
          // PDF.js Multi-Page Document Renderer
          <Document
            file={fileUrl}
            onLoadSuccess={onDocumentLoadSuccess}
            onLoadError={() => setIsPdfFailed(true)}
            loading={
              <div className="flex flex-col items-center justify-center py-24 text-slate-300">
                <div className="w-8 h-8 border-3 border-[#F27448] border-t-transparent rounded-full animate-spin mb-3" />
                <span className="text-xs font-semibold">Loading Answer Sheet...</span>
              </div>
            }
            error={
              <div className="text-center py-20 text-rose-300 text-sm">
                Unable to load answer sheet preview.
              </div>
            }
          >
            {Array.from({ length: numPages }, (_, i) => i + 1).map((pageNum) => (
              <div
                key={pageNum}
                ref={(el) => {
                  if (el) pageRefs.current.set(pageNum, el);
                }}
                className="relative mb-8 shadow-2xl rounded-2xl overflow-hidden bg-white mx-auto border border-black/10 transition-all"
                style={{ width: `${pageWidth}px` }}
              >
                <Page
                  pageNumber={pageNum}
                  width={pageWidth}
                  renderTextLayer={false}
                  renderAnnotationLayer={false}
                />

                {/* Highlight Overlays Layer */}
                {regionsByPage.get(pageNum)?.map((region, idx) => (
                  <div
                    key={idx}
                    className={clsx(
                      'absolute rounded-xl pointer-events-none transition-all duration-150',
                      highlightStyles.box
                    )}
                    style={{
                      left: `${region.x * 100}%`,
                      top: `${region.y * 100}%`,
                      width: `${region.width * 100}%`,
                      height: `${region.height * 100}%`,
                    }}
                  >
                    {/* Score-toned Badge on top-left corner */}
                    <div
                      className={clsx(
                        'absolute -top-3.5 left-0 px-2 py-0.5 rounded-md text-[11px] font-black tracking-tight shadow-sm leading-tight select-none',
                        highlightStyles.badge
                      )}
                    >
                      {questionLabel || 'Q'}
                    </div>
                  </div>
                ))}
              </div>
            ))}
          </Document>
        )}
      </div>
    </div>
  );
}
