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

> **JDK 17+ required.** Make sure your `JAVA_HOME` points to JDK 17 before running.

### 1. Configure your DeepSeek API key

Get one at [platform.deepseek.com](https://platform.deepseek.com/api_keys). Preferred: put it into `application-local.yml` in the project root (git-ignored — it was committed once as a placeholder template and then untracked, so your real key never enters git):

```yaml
spring:
  ai:
    deepseek:
      api-key: sk-your-key
```

Or use an environment variable (higher precedence): `DEEPSEEK_API_KEY=sk-xxxx`.

### 2. Download the embedding model (first time only)

The embedding model (~90MB) is not committed to git. Download it with the platform-specific script:

- **Windows**: `powershell -File download-model.ps1`
- **Mac / Linux**: `bash download-model.sh`

Both fetch from the hf-mirror.com mirror; see the comments in `application.yml` for the remote-URL alternative.

### 3. Run

```bash
mvn spring-boot:run
```

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
└── onnx/all-MiniLM-L6-v2/                 # Embedding model (not committed; fetched by the download scripts)
    ├── model.onnx
    └── tokenizer.json
application-local.yml                       # Local secrets — git-ignored; only a placeholder was committed once
download-model.ps1 / download-model.sh      # One-click download of the embedding model
LICENSE
```

## Core Flow (RagController)

- `POST /upload`: `TikaDocumentReader` parses the PDF → `TokenTextSplitter` chunks by token count → `vectorStore.add()` embeds and stores
- `GET /chat`: `vectorStore.similaritySearch()` fetches the top-4 relevant chunks → injected into the system prompt → `ChatClient` calls DeepSeek to generate the answer

## Common Adjustments

- **Switch embedding provider**: DeepSeek has no embedding API. To use an OpenAI-compatible cloud embedding (e.g. SiliconFlow), add `spring-ai-starter-model-openai` to the pom and set `spring.ai.model.embedding: openai` plus the matching `base-url/api-key/model` in the yml.
- **Persistence**: `SimpleVectorStore` offers `save(File)` / `load(File)`, or switch to a production vector-store starter (PGvector/Redis, etc.).
- **Chunking granularity**: `new TokenTextSplitter(800, 350, 5, 10000, true, List.of('.', '?', '!', '\n', ';'))` to customize sentence-boundary punctuation.

## Author

GitHub: [@xtgq-glitch](https://github.com/xtgq-glitch)

## License

MIT — see [LICENSE](LICENSE).
