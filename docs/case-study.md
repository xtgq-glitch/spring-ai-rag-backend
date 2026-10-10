# Case Study: Spring AI RAG Backend

**A production-oriented RAG backend for Java teams — upload documents, ask questions, get answers grounded in the sources.**

## The problem

Enterprise knowledge lives in PDFs and Office documents, and most enterprise backends are Java/Spring. Yet nearly every RAG example is Python-first. This project shows how to add document question-answering to an existing Spring Boot service without leaving the JVM stack.

## What it does

- **`POST /api/documents`** — Apache Tika parses the upload (PDF, DOCX, PPTX, TXT, MD, HTML), `TokenTextSplitter` chunks the text, and each chunk is embedded and indexed.
- **`POST /api/chat`** — a top-4 similarity search retrieves the relevant chunks, the LLM (DeepSeek `deepseek-chat`) answers strictly from them, and the response includes the **source snippets** so every claim is checkable.
- Consistent JSON errors (`400` / `413` / `415` / `500`) via a `@RestControllerAdvice`, with Jakarta Bean Validation on request DTOs.

## Key design decisions

- **One API key, not two.** DeepSeek offers no embedding endpoint, so embeddings run **locally through ONNX Runtime** (`all-MiniLM-L6-v2`, 384 dimensions) — no second key, no per-token embedding cost, and document content never leaves the machine except inside the final LLM prompt.
- **Retrieval fail-safe.** If nothing similar enough is indexed, the service answers "no relevant content" explicitly instead of letting the model improvise.
- **Honest about production.** The in-memory `SimpleVectorStore` keeps the demo self-contained; the README documents the production path (persistent vector store, auth, async ingestion, observability, retrieval evaluation).

## Verified end-to-end

`docs/verification.md` keeps real request/response transcripts: upload returns `201 Created` with chunk counts; a question about the hotel budget in the sample travel policy returns a grounded answer plus the exact source snippet; a blank message returns a mapped `400` error. A focused test suite covers ingestion, retrieval behaviour and error mapping.

**Stack:** Spring Boot 3.5 · Spring AI 1.1.8 · DeepSeek · ONNX Runtime · Apache Tika · JUnit — **License:** MIT.
