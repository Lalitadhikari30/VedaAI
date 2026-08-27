import type { AssessmentResult, AssessmentStatus, UploadResponse } from '../types/assessment';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const errorText = await response.text().catch(() => 'Unknown error');
    throw new Error(`API Error (${response.status}): ${errorText}`);
  }
  return response.json();
}

export async function uploadAssessment(
  questionPaper: File,
  answerSheet: File
): Promise<UploadResponse> {
  const formData = new FormData();
  formData.append('questionPaper', questionPaper);
  formData.append('answerSheet', answerSheet);

  const response = await fetch(`${API_URL}/api/v1/assessments`, {
    method: 'POST',
    body: formData,
  });

  return handleResponse<UploadResponse>(response);
}

export async function getAssessmentStatus(id: string): Promise<AssessmentStatus> {
  const response = await fetch(`${API_URL}/api/v1/assessments/${id}/status`);
  return handleResponse<AssessmentStatus>(response);
}

export async function getAssessmentResult(id: string): Promise<AssessmentResult> {
  const response = await fetch(`${API_URL}/api/v1/assessments/${id}`);
  return handleResponse<AssessmentResult>(response);
}

export async function retryAssessment(id: string): Promise<void> {
  const response = await fetch(`${API_URL}/api/v1/assessments/${id}/retry`, {
    method: 'POST',
  });
  if (!response.ok) {
    throw new Error(`Retry failed: ${response.status}`);
  }
}

export function getAnswerSheetUrl(id: string): string {
  return `${API_URL}/api/v1/assessments/${id}/answer-sheet`;
}
