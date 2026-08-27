# VedaAI — AI Assessment Extraction & Answer Mapping

A full-stack, production-grade assessment platform designed for educators. VedaAI extracts questions from printed question papers, accurately segments handwritten student answers, deterministically maps answers to questions using a four-level matching strategy, and renders pixel-exact answer highlights on the student's original answer sheet.

---

## 🌟 Key Features

1. **Dual-Document Ingestion**: Upload Question Paper and Student Answer Sheet (PDF, JPG, PNG up to 15MB).
2. **Hybrid AI Pipeline**:
   - **Google Cloud Vision REST** (`DOCUMENT_TEXT_DETECTION`): Ground-truth OCR geometry with normalized bounding boxes.
   - **Gemini 2.5 Flash**: Text & semantic segmentation and grading reasoning (never invents or guesses coordinates).
3. **Deterministic Geometry Resolution**: Answer regions are computed via mathematical bounding-box union of detected Vision OCR word IDs.
4. **4-Level Mapping Strategy**:
   - Level 1: `EXPLICIT_LABEL` — Exact match (`11(a)` → `11(a)`)
   - Level 2: `NORMALIZED_LABEL` — Format variants (`11-a`, `11 a`, `Q11(a)`, `Ans 5`, `5.`)
   - Level 3: `CONTEXTUAL` — Sequential page ordering & neighbor proximity
   - Level 4: `SEMANTIC_AI` — Semantic fallback for unlabeled / ambiguous answers
5. **Exact Visual Highlighting**: Original document rendering with SVG/HTML overlays that dynamically scale across zoom levels and mobile viewports.
6. **Sub-Part Isolation**: Labelled sub-parts (`11(a)`, `11(b)`) treated as distinct first-class questions.
7. **Edge-Case Resilience**: Out-of-order answers, multi-page answers, unanswered questions, and unmatched answers cleanly surfaced.
8. **Pixel-Exact UI**: Strictly replicates reference design tokens, desktop split-view, mobile segmented tabs, dark charcoal PDF toolbar, and animated starburst extraction screen.

---

## 🏗 Architecture

```
com.vedaai.assessment
├── controller/        REST endpoints (/api/v1/assessments)
├── service/
│   ├── DocumentService.java           PDFBox & image rendering (in-memory only)
│   ├── ExtractionService.java         Orchestrates OCR & LLM word segmentation
│   ├── MappingService.java            4-level deterministic mapping engine
│   ├── GradingService.java            Optional rubric scoring & feedback
│   ├── AssessmentOrchestrator.java    Async pipeline driver
│   └── ResultMapper.java              DTO transformer
├── provider/
│   ├── OcrProvider.java               OCR interface
│   ├── VisionOcrProvider.java         Google Cloud Vision REST implementation
│   ├── LlmProvider.java               LLM reasoning interface
│   └── GeminiProvider.java            Gemini 2.5 Flash implementation
├── store/
│   └── InMemoryAssessmentStore.java   ConcurrentHashMap with TTL eviction
├── dto/                               API contract request/response models
├── model/                             Domain models
└── config/                            Async thread pool, CORS, App properties
```

---

## 🚀 Quick Start & Local Setup

### Prerequisites
- **Java 17+** & **Maven 3.8+**
- **Node.js 18+** & **npm 9+**
- API Keys: `GEMINI_API_KEY` and `GOOGLE_VISION_API_KEY`

### 1. Backend Setup

```bash
cd Backend

# Copy environment template
cp .env.example .env

# Set your API keys in .env or environment variables:
# GEMINI_API_KEY=your_gemini_api_key
# GOOGLE_VISION_API_KEY=your_google_vision_api_key

# Build and run
mvn clean spring-boot:run
```
Backend runs at `http://localhost:8080`.

### 2. Frontend Setup

```bash
cd Frontend

# Install dependencies
npm install

# Start Vite dev server
npm run dev
```
Frontend runs at `http://localhost:5173`.

---

## 🧪 Running Tests

### Backend Unit Tests
```bash
cd Backend
mvn test
```
Tests cover:
- Label normalization across all variant formats (`11(a)`, `11-a`, `11 a`, `Q11(a)`, `Ans 5`, `5.`)
- 4-level mapping paths (Explicit, Normalized, Contextual, Semantic)
- Out-of-order student answers
- Unanswered questions and unmatched answers
- REST controller upload, status polling, and validation

### Frontend Lint & Type Checks
```bash
cd Frontend
npx tsc --noEmit
```

---

## 📦 API Specification

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/assessments` | Multipart upload (`questionPaper`, `answerSheet`) → `202 Accepted` + `{ assessmentId }` |
| `GET` | `/api/v1/assessments/{id}/status` | Status polling → `{ status: QUEUED\|PROCESSING\|COMPLETED\|FAILED, progress, message }` |
| `GET` | `/api/v1/assessments/{id}` | Full assessment result contract (questions, answers, regions, grading) |
| `POST` | `/api/v1/assessments/{id}/retry` | Re-runs a failed processing job |
| `GET` | `/api/v1/assessments/{id}/answer-sheet` | Streams the answer sheet document for the frontend viewer |

---

## 🚢 Deployment

### Frontend (Vercel)
1. Push `Frontend/` repository.
2. Set Build Command: `npm run build`
3. Set Output Directory: `dist`
4. Set Environment Variable: `VITE_API_URL=https://your-backend-url.railway.app`

### Backend (Railway / Render)
1. Deploy `Backend/` as a Java service.
2. Set Environment Variables:
   - `GEMINI_API_KEY=...`
   - `GOOGLE_VISION_API_KEY=...`
   - `FRONTEND_URL=https://your-frontend.vercel.app`

---

## 🛡 Performance & Cost Guardrails
- **In-Memory Image Lifecycle**: PDFBox renders pages in-memory only, passing them directly to Vision OCR and immediately freeing memory via JVM garbage collection.
- **No Coordinate Hallucination**: Coordinates come exclusively from Vision OCR word boxes. Gemini is only sent plain text with token IDs.
- **Deduplication**: Gemini is only invoked for level-4 semantic matching if high-confidence deterministic label matching failed.
