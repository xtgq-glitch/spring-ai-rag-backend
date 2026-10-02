# Spring AI RAG Demo

A minimal RAG (Retrieval-Augmented Generation) example built with **Spring Boot 3 + Spring AI**: upload a PDF → chunk & embed → store in an in-memory vector store → ask questions grounded in the uploaded documents.

## Tech Stack

| Component | Choice | Notes |
|---|---|---|
| Framework | Spring Boot 3.5 + Spring AI 1.1.8 | |
| LLM | DeepSeek (`deepseek-chat`) | Generates the answers; API key goes in `application-local.yml` (not committed) or env vars |
| Embeddings | Local ONNX model `all-MiniLM-L6-v2` | DeepSeek offers no embedding API, so a local model is used — no extra API key needed |
| Vector store | `SimpleVectorStore` (in-memory) | Cleared on restart; for demo purposes only |
| Document parsing | Apache Tika (`spring-ai-tika-document-reader`) | Supports PDF/DOCX/PPTX and more |

## Quick Start

> **JDK 17+ required.** The default Maven on this machine points to JDK 1.8 (`D:\developer\jdk`), so switch first:
>
> ```powershell
> $env:JAVA_HOME = "D:\developer\jdk_17"   # locally installed JDK 17
> ```

```bash
# 1. Configure your DeepSeek API key (get one at https://platform.deepseek.com/api_keys)
#    Preferred: put it into application-local.yml in the project root (git-ignored, never committed)
#      spring.ai.deepseek.api-key: sk-your-key
#    Or use an environment variable (higher precedence):
# Windows CMD:  set DEEPSEEK_API_KEY=sk-xxxx
# PowerShell:   $env:DEEPSEEK_API_KEY="sk-xxxx"
# Linux/Mac:    export DEEPSEEK_API_KEY=sk-xxxx

# 2. Run
mvn spring-boot:run
```

> Notes:
> - `application-local.yml` was committed once as a placeholder template and then untracked — your local edits (the real key) never enter git.
> - The embedding model (`model.onnx` ~90MB + `tokenizer.json`) is **not committed to git**. After cloning, run `powershell -File download-model.ps1` to fetch it (via the hf-mirror mirror; see the comments in `application.yml` for the remote-URL alternative).

## API

### 1. Upload a PDF

```bash
curl -F "file=@your-document.pdf" http://localhost:8080/upload
# => {"filename":"your-document.pdf","chunks":42}
```

### 2. Ask questions about the uploaded documents

```bash
curl "http://localhost:8080/chat?message=What is this document about?"
# => {"answer":"..."}
```

## Project Layout

```
src/main/java/com/example/ragdemo/
├── RagDemoApplication.java   # Bootstrap + SimpleVectorStore bean
└── RagController.java        # The only controller: /upload and /chat
src/main/resources/
├── application.yml                        # Model config, multipart limits, optional local-config import
└── onnx/all-MiniLM-L6-v2/                 # Embedding model (not committed; fetched by download-model.ps1)
    ├── model.onnx
    └── tokenizer.json
application-local.yml                       # Local secrets — git-ignored; only a placeholder was committed once
download-model.ps1                          # One-click download of the embedding model
```

## Core Flow (RagController)

- `POST /upload`: `TikaDocumentReader` parses the PDF → `TokenTextSplitter` chunks by token count → `vectorStore.add()` embeds and stores
- `GET /chat`: `vectorStore.similaritySearch()` fetches the top-4 relevant chunks → injected into the system prompt → `ChatClient` calls DeepSeek to generate the answer

## Common Adjustments

- **Switch embedding provider**: DeepSeek has no embedding API. To use an OpenAI-compatible cloud embedding (e.g. SiliconFlow), add `spring-ai-starter-model-openai` to the pom and set `spring.ai.model.embedding: openai` plus the matching `base-url/api-key/model` in the yml.
- **Persistence**: `SimpleVectorStore` offers `save(File)` / `load(File)`, or switch to a production vector-store starter (PGvector/Redis, etc.).
- **Chunking granularity**: `new TokenTextSplitter(800, 350, 5, 10000, true, List.of('。', '？', '！'))` to customize Chinese sentence boundaries.
