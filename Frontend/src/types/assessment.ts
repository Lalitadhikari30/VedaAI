// Assessment types matching the backend contract (section 8)

export interface AssessmentResult {
  assessmentId: string;
  status: string;
  summary: AssessmentSummary;
  questions: QuestionResult[];
  unansweredQuestions: UnansweredQuestion[];
  unmatchedAnswers: UnmatchedAnswer[];
}

export interface AssessmentSummary {
  totalQuestions: number;
  answered: number;
  unanswered: number;
  unmatchedAnswers: number;
  totalMarks: number;
  obtainedMarks: number;
}

export interface QuestionResult {
  id: string;
  number: string;
  parentNumber: string;
  subPart: string | null;
  displayLabel: string;
  text: string;
  displayOrder: number;
  answer: AnswerResult | null;
  grading: GradingResult | null;
}

export interface AnswerResult {
  id: string;
  text: string;
  confidence: number;
  mappingMethod: MappingMethod;
  regions: AnswerRegion[];
}

export interface AnswerRegion {
  page: number;
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface GradingResult {
  score: number;
  maxScore: number;
  status: GradingStatus;
  feedback: string;
}

export interface UnansweredQuestion {
  id: string;
  number: string;
  text: string;
  displayOrder: number;
}

export interface UnmatchedAnswer {
  id: string;
  detectedLabel: string | null;
  text: string;
  regions: AnswerRegion[];
}

export interface AssessmentStatus {
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  progress: string;
  message: string | null;
}

export type MappingMethod = 'EXPLICIT_LABEL' | 'NORMALIZED_LABEL' | 'CONTEXTUAL' | 'SEMANTIC_AI';

export type GradingStatus = 'CORRECT' | 'PARTIALLY_CORRECT' | 'INCORRECT' | 'REVIEW' | 'GRADING_UNAVAILABLE';

export interface UploadResponse {
  assessmentId: string;
}
