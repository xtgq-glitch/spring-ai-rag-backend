# Service Scope & Deliverables

## What this project is

A working **reference implementation of a RAG backend** — document upload, indexing, and grounded question-answering with source snippets — built with Spring Boot 3.5 + Spring AI 1.1.8. It can be used as a demo, as a starting point for an internal feature, or as the base for an integration into an existing Java backend.

## Included deliverables (already in this repository)

| Deliverable | Location |
|---|---|
| Full source code (Java 17, Maven project) | `src/` |
| Architecture diagram | `docs/images/architecture.svg` |
| Quick-start README (JDK/Maven prerequisites, API-key setup, model download incl. China mirror) | `README.md` |
| End-to-end verification log with real request/response transcripts | `docs/verification.md` |
| Real API screenshots (upload, grounded query) | `docs/images/` |
| Sample document used in the demo | `docs/samples/travel-policy.pdf` |
| Embedding-model download scripts (Windows / macOS / Linux, Hugging Face + mirror) | `download-model.ps1`, `download-model.sh` |
| Test suite (ingestion, retrieval behaviour, error mapping) | `src/test/` |
| One-shot end-to-end verification runner | `verify-e2e.ps1` |

## Customization work available on request

- **Swap or pluggable LLM** — OpenAI, Anthropic, Qwen, or any Spring AI-supported model instead of DeepSeek.
- **Persistent vector store** — PGVector, Redis or Milvus replacing the in-memory store, so indexes survive restarts.
- **Authentication & multi-tenant isolation** — securing both endpoints; per-user/team document namespaces via metadata filters.
- **Ingestion hardening** — asynchronous processing with a progress endpoint, content-type sniffing, deduplication, document registry with re-indexing.
- **Operations** — metrics/tracing/structured logging around retrieval and LLM calls; rate limiting and cost control; a retrieval evaluation harness (recall/precision, chunk-size tuning).
- **Frontend** — a small chat UI on top of the two endpoints.

## Explicitly out of scope in the current repository

Auth, persistence, quotas and virus scanning are intentionally **not** implemented — they are documented under *Production Considerations* in the README and are available as the add-ons listed above.

## What is needed to run it

- JDK 17+ and Maven 3.6+
- One LLM API key (e.g. DeepSeek) — kept in a git-ignored local config or an environment variable
- Network access once to download the ~90 MB local embedding model (mirror provided)
