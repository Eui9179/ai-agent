# RAG Server

LangChain4j + Spring Boot 기반 RAG(Retrieval-Augmented Generation) 서버 학습 프로젝트

---

## 프로젝트 목표

- **PDF/Word/txt 등 문서를 업로드해 로컬 지식 베이스 구축**
- **업로드된 문서를 기반으로 LLM과 질의응답** (RAG 파이프라인)
- **완전 로컬 실행**: 외부 API 없이 Ollama + Qdrant만으로 동작
- 향후 AI Agent, Multi-turn 대화, OCR 파이프라인 확장 예정

### 시스템 흐름

```
[문서 업로드] POST /v1/api/documents/ingest
    ↓
[파싱]     LangChain4j + Apache Tika  (PDF, Word, txt 등)
    ↓
[청킹]     DocumentSplitters.recursive(500, 50)
    ↓
[임베딩]   Ollama BAAI/bge-m3  →  EmbeddingStore
    ↓
[저장]     Qdrant (Vector DB)

[질의응답] POST /v1/api/chat
    ↓
[검색]     질문 임베딩 → Qdrant 유사도 검색 (top 5)
    ↓
[생성]     검색 결과 + 질문 → Ollama llama3.2 → 답변 반환
```

> 자세한 다이어그램: `rag-architecture.html`

---

## 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.4.3 |
| AI 프레임워크 | LangChain4j | 1.12.2 |
| LLM 런타임 | Ollama | latest |
| LLM 모델 | llama3.2 | - |
| Embedding 모델 | BAAI/bge-m3 | - |
| Vector DB | Qdrant | latest |
| 문서 파싱 | Apache Tika (via LangChain4j) | - |
| 빌드 도구 | Gradle | 9.3.1 |
| Boilerplate | Lombok | - |

### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/v1/api/chat` | RAG 기반 질의응답 |
| POST | `/v1/api/documents/ingest` | 문서 파싱 → 청킹 → VectorDB 저장 |

---

## 공부한 내용

### 1. RAG (Retrieval-Augmented Generation)

LLM의 두 가지 한계를 보완하는 아키텍처:
- 학습 시점 이후 정보를 알지 못함
- 사내 전용 문서, 기밀 데이터를 직접 학습시키기 어려움

**핵심 흐름:**
```
질문 → 질문을 벡터로 변환(Embedding) → 벡터DB에서 유사 문서 검색(Retrieval)
     → 검색된 문서 + 질문을 LLM에 전달(Augmentation) → 답변 생성(Generation)
```

---

### 2. Embedding & Vector DB

**Embedding:** 텍스트를 고차원 숫자 벡터로 변환. 의미가 유사한 문장은 벡터 공간에서 가깝게 위치함.

```
"사과는 과일이다"  → [0.12, 0.87, -0.34, ...]
"apple is a fruit" → [0.11, 0.85, -0.31, ...]  # 유사한 벡터
"주식 시장 급락"   → [-0.72, 0.03, 0.91, ...]  # 먼 벡터
```

**Qdrant (Vector DB):** 벡터를 저장하고 유사도 기반 ANN(Approximate Nearest Neighbor) 검색 제공.

| 포트 | 용도 |
|------|------|
| 6333 | REST API + 웹 대시보드 (`http://localhost:6333/dashboard`) |
| 6334 | gRPC (LangChain4j 기본 사용) |

---

### 3. LangChain4j

Java/Kotlin용 LLM 통합 프레임워크. 이 프로젝트에서는 문서 파싱, 청킹, RAG 파이프라인 전체를 담당.

**Apache Tika 연동 (`langchain4j-document-parser-apache-tika`):**
- PDF, Word(.docx), Excel, PowerPoint, txt 등을 단일 `ApacheTikaDocumentParser`로 처리

**재귀적 청킹 (`DocumentSplitters.recursive`):**
```
단락 → 문장 → 단어 순으로 재귀 분할
overlap으로 청크 경계에서 문맥 손실 방지
```

**AiServices (RAG Assistant):**
```java
// 인터페이스 선언만으로 RAG Q&A 서비스 생성
RagAssistant assistant = AiServices.builder(RagAssistant.class)
    .chatModel(chatModel)
    .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
    .build();
```

---

### 4. Ollama

로컬에서 LLM을 실행하는 런타임. 외부 API 없이 오프라인 환경에서 LLM 사용 가능.

```bash
ollama pull llama3.2   # LLM (채팅/답변 생성)
ollama pull bge-m3     # Embedding 전용 모델
```

**LLM vs Embedding 모델 구분:**

| 구분 | 모델 | 용도 |
|------|------|------|
| LLM | llama3.2, gemma3 | 텍스트 생성, 질의응답 |
| Embedding | bge-m3, nomic-embed-text | 텍스트 → 벡터 변환 |

---

### 5. Chunking 전략

| 전략 | 특징 |
|------|------|
| Fixed-size | 고정 토큰/문자 수로 분할. 단순하지만 문장 중간에서 잘릴 수 있음 |
| **Recursive** (현재) | 단락 → 문장 → 단어 순으로 재귀 분할. 일반적으로 권장 |
| Semantic | 의미 유사도 기준 분할. 품질 높지만 비용 큼 |

---

### 6. Spring AI → LangChain4j 마이그레이션

초기에는 Spring AI로 구현했으나, Spring AI 2.x가 아직 마일스톤 버전이라 API 안정성 이슈로 LangChain4j로 전환.

**LangChain4j 장점:**
- 안정적인 릴리즈 사이클
- `AiServices`로 인터페이스 기반 AI 서비스 선언 가능
- 문서 파싱/청킹/RAG 파이프라인이 하나의 라이브러리로 통합

---

## TODO

- [ ] LLM을 활용한 이미지/스캔 문서 텍스트 추출 (OCR 대체)
- [ ] 파일 확장자별 파서 분기 처리 (`DocumentParserFactory` 활용)
- [ ] Multi-turn 대화 (채팅 히스토리 유지)
- [ ] AI Agent 구현 (Tool-calling 기반)
- [ ] 문서 관리 API (목록 조회, 삭제)
- [ ] PaddleOCR FastAPI 서버 연동

---

## 최초 환경 설정

### 1. 필수 설치

- Java 21+
- Docker & Docker Compose

### 2. `application-secret.properties` 생성

`src/main/resources/`에 `application-secret.properties` 파일을 생성한다. (`.gitignore`에 포함됨)

```properties
# Ollama 서버 주소
langchain4j.ollama.chat-model.base-url=http://localhost:11434
langchain4j.ollama.embedding-model.base-url=http://localhost:11434

# Qdrant 서버 주소
qdrant.host=localhost
```

> 원격 서버에서 Ollama/Qdrant를 실행하는 경우 `localhost` 대신 해당 서버 IP를 입력한다.

### 3. Docker 서비스 실행

```bash
# Qdrant + Ollama 컨테이너 실행
docker compose -f docker/docker-compose.yml up -d

# Ollama 모델 다운로드 (최초 1회)
docker exec -it ollama ollama pull llama3.2
docker exec -it ollama ollama pull bge-m3
```

### 4. 서버 실행

```bash
./gradlew bootRun
```

서버: `http://localhost:8090`

### 5. 동작 확인

```bash
# Qdrant 대시보드
open http://localhost:6333/dashboard

# Ollama 상태 확인
curl http://localhost:11434/api/tags

# 문서 업로드
curl -X POST http://localhost:8090/v1/api/documents/ingest \
  -F "file=@/path/to/document.pdf" \
  -F 'request={"fileName":"document.pdf","client":"test","fileId":"doc-001"};type=application/json'

# 질의응답
curl -X POST http://localhost:8090/v1/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "업로드한 문서에 대한 질문"}'
```

---

## 참고 자료

- [LangChain4j 공식 문서](https://docs.langchain4j.dev/)
- [Qdrant 공식 문서](https://qdrant.tech/documentation/)
- [Ollama 공식 사이트](https://ollama.com/)
- `rag-architecture.html` — 프로젝트 아키텍처 다이어그램
