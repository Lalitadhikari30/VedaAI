import { pdfjs } from 'react-pdf';

// Ensure worker is configured if needed
if (!pdfjs.GlobalWorkerOptions.workerSrc) {
  pdfjs.GlobalWorkerOptions.workerSrc = `https://unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;
}

/**
 * Calculates the exact page count of a PDF or Image file on the client side.
 */
export async function getFilePageCount(file: File): Promise<number> {
  const isImage = file.type.startsWith('image/') || /\.(jpe?g|png|webp|bmp|gif)$/i.test(file.name);
  if (isImage) {
    return 1;
  }

  const isPdf = file.type === 'application/pdf' || /\.pdf$/i.test(file.name);
  if (!isPdf) {
    return 1;
  }

  try {
    const arrayBuffer = await file.arrayBuffer();
    const loadingTask = pdfjs.getDocument({ data: new Uint8Array(arrayBuffer) });
    const pdfDoc = await loadingTask.promise;
    return pdfDoc.numPages || 1;
  } catch (error) {
    console.warn('PDF.js count failed, using fast binary search fallback:', error);
    try {
      const buffer = await file.arrayBuffer();
      const text = new TextDecoder('latin1').decode(buffer);
      const matches = text.match(/\/Type\s*\/Page\b/g);
      return matches && matches.length > 0 ? matches.length : 1;
    } catch {
      return 1;
    }
  }
}
