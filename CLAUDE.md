# CLAUDE.md

## 프로젝트 개요

**RAG Server** - LangChain4j 기반 RAG(Retrieval-Augmented Generation) 서버

- **Java**: 21
- **Framework**: Spring Boot 3.4.3, LangChain4j 0.36.2

## 시스템 아키텍처
*rag-architecture.html 참고

```
[문서 입력] POST /api/documents/ingest (MultipartFile)
    ↓
~~[OCR] PaddleOCR → 추후 Python(FastAPI) 서버로 분리 예정~~
텍스트 추출은 LLM 사용 예정 (인터페이스로 교체 가능한 코드 구조 필요)
    ↓
[파싱] LangChain4j + Apache Tika (PDF, Word, txt 등)
    ↓
[Chunking] LangChain4j DocumentSplitters.recursive(500, 50)
    ↓
[Embedding] Ollama + BAAI/bge-m3 (LangChain4j EmbeddingModel → EmbeddingStore)
    ↓
[VectorDB] Qdrant
    ↑
[LLM] Ollama (개발용)
    ↑
[RAG API Server] Spring Boot (본 프로젝트)
    - POST /api/documents/ingest  문서 수집
    - POST /v1/api/chat           질의응답
```

## 기술 스택

| 구분 | 기술 |
|------|------|
| LLM | Ollama (`langchain4j-ollama` → `OllamaChatModel`) |
| Embedding 모델 | BAAI/bge-m3 (`langchain4j-ollama` → `OllamaEmbeddingModel`) |
| Vector Store | Qdrant (`langchain4j-qdrant` → `QdrantEmbeddingStore`) |
| RAG 파이프라인 | LangChain4j `AiServices` + `EmbeddingStoreContentRetriever` |
| 문서 파싱 | LangChain4j + Apache Tika (`langchain4j-document-parser-apache-tika`) |
| 문서 청킹 | LangChain4j `DocumentSplitters.recursive` |
| Web | Spring MVC (`spring-boot-starter-web`) |
| Boilerplate | Lombok |

## 외부 의존 서비스

| 서비스 | 역할 | 기본 포트 |
|--------|------|-----------|
| Ollama | LLM 및 Embedding 실행 | 11434 |
| Qdrant | 벡터 DB | 6333 |
| PaddleOCR | 문서 OCR (FastAPI, 예정) | - |

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

서버 기본 포트: **8090**

## 프로젝트 구조

```
rag/
├── src/
│   ├── main/
│   │   ├── java/com/leui/rag/
│   │   │   └── RagApplication.java       # 진입점
│   │   └── resources/
│   │       └── application.properties        # 설정
│   └── test/
│       └── java/com/leui/rag/
│           └── RagApplicationTests.java
├── build.gradle
├── settings.gradle
└── CLAUDE.md
```

### 패키지 구조

```
com.leui.rag/
├── domain/
│   ├── chat/
│   │   ├── config/       # ChatConfig (ChatClient Bean)
│   │   ├── controller/   # RagController
│   │   ├── dto/          # ChatRequest, ChatResponse
│   │   └── service/      # ChatService
│   └── rag/
│       ├── config/       # ChunkingProperties
│       ├── controller/   # DocumentController
│       ├── dto/          # IngestionResponse
│       └── service/      # DocumentIngestionService
└── RagApplication.java
```

### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/v1/api/chat` | RAG 기반 질의응답 |
| POST | `/api/documents/ingest` | 문서 파싱 → 청킹 → VectorDB 저장 |

## 개발 규칙

- 패키지 루트: `com.leui.rag`
- 코드 스타일: Lombok 적극 활용 (`@Slf4j`, `@RequiredArgsConstructor` 등)
- 설정은 `application.properties` 사용
- 컨테이너 오케스트레이션: `docker-compose`

## TODO
1. ~~LangChain Java, Spring boot와 연동~~ ✅ LangChain4j 통합 완료 (문서 파싱/청킹 파이프라인)
2. LLM을 활용하여 파일 텍스트 추출
   1. 각 파일 확장자 별로 처리 방식을 따로 가져갈지?

---

## Claude Code 운영 규칙

### 1. Anti-Hallucination (최신 API 검증)
Spring AI, LangChain4j 코드를 작성할 때는 **반드시 공식 문서를 먼저 검색**한다.
- Spring AI 1.x는 GA 버전으로 안정적이나, 2.x 마이그레이션 시 API 변경 주의
- `WebSearch` 또는 `WebFetch`로 아래 문서를 선행 확인한다:
    - Spring AI: `https://docs.spring.io/spring-ai/reference/`
    - LangChain4j: `https://docs.langchain4j.dev/`
    - Ollama API: `https://github.com/ollama/ollama/blob/main/docs/api.md`

### 2. 자동 빌드 및 자가 수정 (gradle-auto-fixer)
코드를 작성하거나 수정한 후에는 **반드시 아래 워크플로우를 스스로 실행**한다.

- **빌드 실행:** `./gradlew build` 실행
- **에러 분석:** 컴파일 에러나 테스트 실패 시 사용자에게 묻지 말고 스스로 로그를 분석한다
- **자동 수정:** 원인을 분석해 직접 코드를 수정한다
- **반복:** `BUILD SUCCESSFUL`이 될 때까지 반복한다
- **최종 보고:** 모든 테스트 통과 후에만 완료를 보고한다

### 3. RAG 파이프라인 설계 원칙
- **인터페이스 우선:** LLM, Embedding, VectorStore는 Spring AI 추상화 인터페이스(`ChatModel`, `EmbeddingModel`, `VectorStore`)에 의존한다. Ollama/Qdrant 구현체에 직접 의존하지 않는다 (교체 가능 구조 유지)
- **청크 설정 상수화:** `CHUNK_SIZE`, `CHUNK_OVERLAP`은 하드코딩하지 않고 `application.properties`로 외부화한다
- **문서 메타데이터 보존:** Ingest 시 `source`, `page`, `timestamp` 등 메타데이터를 `Document`에 함께 저장해 추적 가능하게 한다

### 4. 리소스 안전 규칙
- **임시 파일 반드시 삭제:** `ingest` 처리 중 생성한 temp 파일은 `try-finally`로 `Files.deleteIfExists()` 보장
- **스트리밍 처리:** 대용량 문서를 `byte[]`로 한 번에 메모리에 올리지 않는다. LangChain4j의 `DocumentLoader`를 통해 스트리밍 파싱한다

### 5. 외부 서비스 연동 규칙
- Ollama/Qdrant 주소는 `application.properties`에서만 관리하고 코드에 하드코딩 금지
- Ollama 모델명(`llama3.2`, `bge-m3`)도 설정 파일로 외부화한다 (`spring.ai.ollama.chat.options.model` 등)
- 외부 서비스 연결 실패 시 의미 있는 에러 메시지를 반환한다 (스택 트레이스 그대로 노출 금지)
