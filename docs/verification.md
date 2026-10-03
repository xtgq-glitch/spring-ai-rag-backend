# Verification Log

End-to-end test records of the running service (Spring Boot 3.5.15 + Spring AI 1.1.8, model `deepseek-chat`).

## Test 1: Chat endpoint (no document)

**Request:**
GET /chat?message=你好

**Response:**
{"answer":"我是一个严谨的文档问答助手，只根据参考资料回答问题，没有相关内容时会直接说明。"}

---

## Test 2: Upload PDF

**Request:**
curl -X POST -F "file=@sample.pdf" http://localhost:8080/upload

**Response:**
{"filename":"sample.pdf","chunks":1}

---

## Test 3: RAG query with single document

**Request:**
GET /chat?message=一线城市住宿标准是多少？

**Response:**
{"answer":"一线城市（北京、上海、广州、深圳）住宿标准为不超过 600 元/晚。"}

---

## Test 4: Multi-document conflict detection

**Request:**
GET /chat?message=一线城市住宿标准是多少？

**Response:**
{"answer":"根据参考资料，一线城市住宿标准存在不一致的情况：\n\n- 第一份《公司差旅报销管理制度》规定：一线城市...600 元/晚。\n- 第二份...200 元/晚。\n- 第三份...600 元/晚。\n\n其中两份资料显示为 600 元/晚，一份资料显示为 200 元/晚。"}
