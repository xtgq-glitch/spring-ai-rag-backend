# Spring AI RAG Demo

这是一个基于 Spring Boot 和 Spring AI 的最小知识库问答（RAG）后端示例。

用于展示：如何在不重构原有 Java 系统的前提下，集成大模型能力，实现企业知识库问答。

## 技术选型

- 后端：Spring Boot 3.x
- AI 框架：Spring AI 1.0
- 向量库：Chroma（本地验证用，生产可切换 PGVector / Redis）
- 模型：Ollama（本地零成本验证，可切换云端 API）

## 当前进度

项目骨架已搭建，正在跑通文档导入 → 向量化 → 检索 → 问答的基础流程。

后续会补充 REST API 接口实现和部署说明。

## 适用场景

- 企业知识库问答
- 内部文档检索
- 智能客服后端
