# Leui RAG Server

Spring AI 기반 RAG(Retrieval-Augmented Generation) 서버

---

## 사전 지식

### 1. RAG (Retrieval-Augmented Generation)

LLM의 한계(학습 데이터 기준일, 사내 문서 미포함)를 보완하는 아키텍처.

**핵심 흐름:**
```
질문 → 질문을 벡터로 변환(Embedding) → 벡터DB에서 유사 문서 검색(Retrieval)
     → 검색된 문서 + 질문을 LLM에 전달(Augmentation) → 답변 생성(Generation)
```

**왜 필요한가?**
- LLM은 학습 시점 이후의 정보를 모름
- 사내 전용 문서, 기밀 데이터를 LLM에 직접 학습시키기 어려움
- RAG는 검색 기반으로 최신/전용 정보를 실시간 주입

---

### 2. Embedding & Vector DB

**Embedding:** 텍스트를 고차원 숫자 벡터로 변환하는 과정.
의미가 유사한 문장은 벡터 공간에서 가깝게 위치함.

```
"사과는 과일이다"  → [0.12, 0.87, -0.34, ...]
"apple is a fruit" → [0.11, 0.85, -0.31, ...]  # 유사한 벡터
"주식 시장 급락"   → [-0.72, 0.03, 0.91, ...]  # 먼 벡터
```

**Vector DB (Qdrant):** 벡터를 저장하고 유사도 기반 검색(ANN)을 제공하는 DB.
일반 RDB의 `SELECT * WHERE` 대신 "이 벡터와 가장 가까운 N개"를 조회함.

- **포트 6333**: REST API + 웹 대시보드 (`http://localhost:6333/dashboard`)
- **포트 6334**: gRPC (Spring AI가 기본으로 사용)
- **Collection**: RDB의 테이블에 해당하는 단위

---

### 3. LangChain4j

Java/Kotlin용 LangChain 구현체. 이 프로젝트에서는 **문서 파싱과 청킹** 역할을 담당.

**Apache Tika 연동 (`langchain4j-document-parser-apache-tika`):**
- PDF, Word(.docx), Excel, PowerPoint, txt 등 다양한 포맷을 텍스트로 추출
- 별도 포맷별 파서 없이 단일 `ApacheTikaDocumentParser`로 처리

**재귀적 청킹 (`DocumentSplitters.recursive`):**
```
recursive(maxChunkSize=500, overlap=50)

단락 → 문장 → 단어 순으로 재귀 분할
겹침(overlap)으로 청크 경계에서 문맥 손실 방지
```

**Spring AI와의 연동 구조:**
```
MultipartFile
  → LangChain4j ApacheTikaDocumentParser  (파싱)
  → LangChain4j DocumentSplitters         (청킹 → TextSegment)
  → Spring AI Document 변환
  → Spring AI VectorStore.add()           (임베딩 + Qdrant 저장)
```

---

### 4. Ollama

로컬에서 LLM을 실행하는 런타임. OpenAI API 없이 오프라인 환경에서 LLM 사용 가능.

```bash
# 모델 다운로드
ollama pull llama3.2          # LLM (채팅/답변 생성)
ollama pull bge-m3            # Embedding 전용 모델

# 실행 확인
ollama list
curl http://localhost:11434/api/tags
```

**LLM vs Embedding 모델 구분:**
| 구분 | 모델 예시 | 용도 |
|------|----------|------|
| LLM | llama3.2, gemma3 | 텍스트 생성, 질의응답 |
| Embedding | bge-m3, nomic-embed-text | 텍스트 → 벡터 변환만 |

---

### 4. Spring AI

Spring 생태계의 AI 통합 프레임워크. LLM, Vector Store, Embedding을 추상화된 인터페이스로 제공.

**Spring AI 2.x (M2) 주의사항:**
- 아직 마일스톤 버전. API가 GA 전에 변경될 수 있음
- `spring-ai-bom`으로 의존성 버전 통일 관리
- 레포지터리에 `repo.spring.io/milestone` 필수

---

### 5. Chunking

문서를 벡터DB에 저장하기 전에 적절한 크기로 분할하는 과정.

**왜 필요한가?**
- LLM 컨텍스트 윈도우 제한
- 너무 큰 청크 → 검색 정확도 하락
- 너무 작은 청크 → 문맥 손실

**주요 전략:**
| 전략 | 특징 |
|------|------|
| Fixed-size | 고정 토큰/문자 수로 분할. 단순하지만 문장 중간에서 잘릴 수 있음 |
| Recursive | 단락 → 문장 → 단어 순으로 재귀 분할. 일반적으로 권장 |
| Semantic | 의미 유사도 기준 분할. 품질 높지만 비용 큼 |

---

### 6. 로컬 개발 환경 구성

**필수 설치:**
- Java 21+
- Docker & Docker Compose

**서비스 기동:**
```bash
# Qdrant + Ollama 컨테이너 실행
docker compose -f docker/docker-compose.yml up -d

# Ollama 모델 다운로드 (최초 1회)
docker exec -it ollama ollama pull llama3.2
docker exec -it ollama ollama pull bge-m3

# 서버 실행
./gradlew bootRun
```

**동작 확인:**
```bash
# Qdrant 대시보드
open http://localhost:6333/dashboard

# Ollama 상태
curl http://localhost:11434/api/tags

# RAG API
curl -X POST http://localhost:8080/api/rag/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "질문 내용"}'
```

---

### 7. 참고 자료

- [Spring AI 공식 문서](https://docs.spring.io/spring-ai/reference/)
- [Qdrant 공식 문서](https://qdrant.tech/documentation/)
- [Ollama 공식 사이트](https://ollama.com/)
- `rag-architecture.html` — 본 프로젝트 아키텍처 다이어그램

### 8. LLM 교체
ChatClient가 추상화되어서 Spring bean에서 주입하기 때문에 LLM 의존성만 변경해주면 된다.

| LLM | Starter |
|------|------|
| Ollama (현재) | spring-ai-starter-model-ollama |
| OpenAI | spring-ai-starter-model-openai |
| Azure OpenAI | spring-ai-starter-model-azure-openai |
| Anthropic Claude | spring-ai-starter-model-anthropic |
| Google Gemini | spring-ai-starter-model-vertex-ai-gemini |

