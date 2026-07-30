# resume-matcher

이력서와 채용공고(JD)를 넣으면, AI와 대화하며 그 공고에 맞게 이력서를 다듬어주는 개인용 툴.

## 사용자 플로우

1. **이력서 등록**: 이력서 원문(텍스트) 또는 PDF를 업로드해 저장한다.
2. **JD 입력**: 지원하려는 채용공고 JD 텍스트를 붙여넣는다.
3. **AI와 대화**: 채팅으로 "이 JD에 맞게 강점을 더 부각시켜줘" 같은 요청을 하면, AI가 이력서+JD+대화 맥락을 바탕으로 응답하고 필요 시 수정된 이력서 전문을 제시한다.
4. **적용/버전 관리**: 제안된 수정본을 적용해 현재 이력서로 교체하고, 이전 버전을 확인·복원할 수 있다.
5. **내보내기**: 최종본을 복사하거나 다운로드한다.

자세한 기획 배경은 [PLANNING.md](PLANNING.md), 항목별 상세 기획 문서는
[docs/planning/](docs/planning/00-index.md) 참고.

## 아키텍처

- **프론트엔드**: 별도 빌드 도구 없는 순수 HTML/CSS/JS (`src/main/resources/static/`), 상태는 브라우저 localStorage에 저장.
- **백엔드**: Spring Boot(Kotlin) API 서버. 상태를 갖지 않고 Claude API에 대한 프록시 역할만 한다 (DB 없음, 인증 없음).

```
[브라우저 localStorage] --fetch(JSON)--> [Spring Boot 백엔드] --> [Anthropic Claude API]
```

## API

### `POST /api/chat`

**Request**
```json
{
  "resume": "현재 이력서 전문",
  "jobDescription": "JD 전문",
  "messages": [
    { "role": "user", "content": "이전 사용자 메시지" },
    { "role": "assistant", "content": "이전 AI 응답" }
  ]
}
```

**Response**
```json
{
  "reply": "AI의 대화형 응답",
  "updatedResume": "수정을 제안하는 경우, 전체 이력서 텍스트 (없으면 null)"
}
```

### `POST /api/resume/extract`

PDF 이력서에서 텍스트를 추출한다. `multipart/form-data`의 `file` 필드로 PDF를 전송하면 `{"text": "..."}` 를 반환한다.

## 실행 방법

Anthropic API 키를 환경변수로 설정한 뒤 서버를 실행한다.

```bash
export ANTHROPIC_API_KEY=sk-ant-...
./gradlew bootRun
```

기본적으로 `http://localhost:8080` 에서 정적 프론트엔드와 API가 함께 서빙된다.

### Docker로 실행하기

Java나 Gradle을 로컬에 설치하지 않고도 Docker만으로 실행할 수 있다.

```bash
cp .env.example .env
# .env 파일에 ANTHROPIC_API_KEY 값을 채운다

docker compose up --build
```

`http://localhost:8080` 에서 접속하면 된다.

## 기술 스택

- Kotlin, Spring Boot (WebMVC)
- Anthropic Claude API (`com.anthropic:anthropic-java`)
- Apache PDFBox (PDF 텍스트 추출)
