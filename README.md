# Spring AI RAG Demo

基于 **Spring Boot 3 + Spring AI** 的最小 RAG（检索增强生成）示例：上传 PDF → 切块向量化 → 存入内存向量库 → 基于 PDF 内容问答。

## 技术选型

| 组件 | 选择 | 说明 |
|---|---|---|
| 框架 | Spring Boot 3.5 + Spring AI 1.1.8 | |
| 大模型 | DeepSeek（`deepseek-chat`） | 只负责对话/回答，配置 `DEEPSEEK_API_KEY` 即可 |
| Embedding | 本地 ONNX 模型 `all-MiniLM-L6-v2` | **DeepSeek 官方不提供 embedding 接口**，故用本地模型，无需额外 API Key；模型文件已内置在 `src/main/resources/onnx/` |
| 向量库 | `SimpleVectorStore`（内存） | 重启即清空，仅用于演示 |
| 文档解析 | Apache Tika（`spring-ai-tika-document-reader`） | 支持 PDF/DOCX/PPTX 等 |

## 快速开始

> **需要 JDK 17+**。本机默认 Maven 使用的是 JDK 1.8（`D:\developer\jdk`），需先切换：
>
> ```powershell
> $env:JAVA_HOME = "D:\developer\jdk_17"   # 本机已装好的 JDK 17
> ```

```bash
# 1. 配置 DeepSeek API Key（https://platform.deepseek.com/api_keys 申请）
# Windows CMD:
set DEEPSEEK_API_KEY=sk-xxxx
# PowerShell:
$env:DEEPSEEK_API_KEY="sk-xxxx"
# Linux/Mac:
export DEEPSEEK_API_KEY=sk-xxxx

# 2. 启动
mvn spring-boot:run
```

> 说明：Embedding 模型（`model.onnx` 约 90MB + `tokenizer.json`）**不提交到 git**，本地文件通过 classpath 加载，
> **启动和运行都不需要访问外网**。克隆仓库后先执行 `powershell -File download-model.ps1` 下载模型
> （国内走 hf-mirror 镜像）；也可按 `application.yml` 中的注释改为远程 URL 直接下载。

## 接口

### 1. 上传 PDF

```bash
curl -F "file=@你的文档.pdf" http://localhost:8080/upload
# => {"filename":"你的文档.pdf","chunks":42}
```

### 2. 基于文档问答

```bash
curl "http://localhost:8080/chat?message=这份文档主要讲了什么？"
# => {"answer":"..."}
```

## 项目结构

```
src/main/java/com/example/ragdemo/
├── RagDemoApplication.java   # 启动类 + SimpleVectorStore Bean
└── RagController.java        # 唯一的 Controller：/upload 与 /chat
src/main/resources/
├── application.yml                        # DeepSeek Key、模型、multipart 上限
└── onnx/all-MiniLM-L6-v2/                 # 向量化模型（不入库，由 download-model.ps1 下载）
    ├── model.onnx
    └── tokenizer.json
download-model.ps1                         # 一键下载向量化模型
```

## 核心流程（RagController）

- `POST /upload`：`TikaDocumentReader` 解析 PDF → `TokenTextSplitter` 按 token 切块 → `vectorStore.add()` 向量化入库
- `GET /chat`：`vectorStore.similaritySearch()` 取最相关的 4 个片段 → 拼入 System Prompt → `ChatClient` 调 DeepSeek 生成回答

## 常见调整

- **换 Embedding 服务**：DeepSeek 无 embedding API。若想改为 OpenAI 兼容的云端 embedding（如硅基流动等），在 pom 中加 `spring-ai-starter-model-openai`，并在 yml 中设 `spring.ai.model.embedding: openai` 与对应 `base-url/api-key/model`。
- **持久化**：`SimpleVectorStore` 提供 `save(File)` / `load(File)`，或生产环境切换 PGVector/Redis 等向量库 starter。
- **切块粒度**：`new TokenTextSplitter(800, 350, 5, 10000, true, List.of('。', '？', '！'))` 可自定义中文分句。
