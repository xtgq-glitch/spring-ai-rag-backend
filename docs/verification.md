# Verification Log

End-to-end records of the running service (Spring Boot 3.5.15 + Spring AI 1.1.8, model `deepseek-chat`).

## Current API (POST)

### Test 1: Upload a document — `POST /api/documents`

**Request:**

```bash
curl -X POST -F "file=@docs/samples/travel-policy.pdf" http://localhost:8080/api/documents
```

**Response:** `HTTP/1.1 201 Created`

```json
{"filename":"travel-policy.pdf","chunks":1,"totalChunks":1}
```

### Test 2: RAG query — `POST /api/chat`

**Request:**

```bash
curl -X POST -H "Content-Type: application/json" \
     -d '{"message":"What is the hotel budget for first-tier cities?"}' \
     http://localhost:8080/api/chat
```

**Response:** `HTTP/1.1 200 OK`

```json
{"answer":"The hotel budget for first-tier cities (Beijing, Shanghai, Guangzhou, Shenzhen) is capped at CNY 600 per night.","sources":["Company Travel & Expense Policy 1. Scope: all employees travelling on company business. 2. Hotel budget: first-tier cities (Beijing, Shanghai, Guangzhou, Shenzhen) are capped at CNY 600 per night. Other cities are capped at CNY 400 per night. 3. Flights: economy class only for trips shorter than 6 h..."]}
```

### Test 3: Input validation — `POST /api/chat` with a blank message

**Request:**

```json
{"message":""}
```

**Response:** `HTTP/1.1 400 Bad Request`

```json
{"status":400,"error":"Bad Request","message":"message must not be blank"}
```

---

## Earlier records (legacy `GET /chat?message=...` API)

A Chinese-language session recorded before the chat endpoint moved to `POST /api/chat`.

### Test 4: Chat endpoint (no document)

**Request:** `GET /chat?message=你好`

**Response:**

```json
{"answer":"我是一个严谨的文档问答助手，只根据参考资料回答问题，没有相关内容时会直接说明。"}
```

### Test 5: Upload PDF

**Request:** `curl -X POST -F "file=@sample.pdf" http://localhost:8080/upload`

**Response:**

```json
{"filename":"sample.pdf","chunks":1}
```

### Test 6: RAG query with a single document

**Request:** `GET /chat?message=一线城市住宿标准是多少？`

**Response:**

```json
{"answer":"一线城市（北京、上海、广州、深圳）住宿标准为不超过 600 元/晚。"}
```

### Test 7: Multi-document conflict detection

**Request:** `GET /chat?message=一线城市住宿标准是多少？`

**Response:**

```json
{"answer":"根据参考资料，一线城市住宿标准存在不一致的情况：\n\n- 第一份《公司差旅报销管理制度》规定：一线城市...600 元/晚。\n- 第二份...200 元/晚。\n- 第三份...600 元/晚。\n\n其中两份资料显示为 600 元/晚，一份资料显示为 200 元/晚。"}
```
