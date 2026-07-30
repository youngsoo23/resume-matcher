# resume-matcher

이력서와 채용공고(JD)를 넣으면, AI와 대화하며 그 공고에 맞게 이력서를 다듬어주는 개인용 툴.

## 진행 상황

- ✅ Claude API 기반 채팅으로 이력서 첨삭 (구조화된 응답: `reply` + `updatedResume`)
- ✅ PDF/텍스트/마크다운 이력서 업로드 (PDF는 서버에서 텍스트 추출)
- ✅ 버전 관리(적용/복원/전체삭제), 클립보드 복사
- ✅ Docker 컨테이너화 — Java/Gradle 설치 없이 `docker compose up --build`로 실행 가능
- ✅ **Claude/Gemini 멀티 AI 프로바이더 선택** — 채팅창에서 원하는 AI를 골라 사용
- ✅ **AI 기반 PDF 다운로드** — 참고 이력서 템플릿 구조로 AI가 재구성한 뒤 브라우저 인쇄로 PDF 저장
- 🚧 PDF 다운로드 속도 개선 예정 (현재 변환에 몇 초~십수 초 소요, 로딩 모달로 진행 상태만 표시 중)

## 사용자 플로우

1. **이력서 등록**: 이력서 원문(텍스트) 또는 PDF를 업로드해 저장한다.
2. **JD 입력**: 지원하려는 채용공고 JD 텍스트를 붙여넣는다.
3. **AI와 대화**: 채팅창 상단에서 Claude/Gemini 중 원하는 AI를 선택하고, "이 JD에 맞게 강점을 더 부각시켜줘" 같은 요청을 하면 이력서+JD+대화 맥락을 바탕으로 응답하고 필요 시 수정된 이력서 전문을 제시한다.
4. **적용/버전 관리**: 제안된 수정본을 적용해 현재 이력서로 교체하고, 이전 버전을 확인·복원할 수 있다.
5. **내보내기**: 최종본을 복사하거나 PDF로 다운로드한다. (선택한 AI가 이력서를 정해진 템플릿 구조로 재구성한 뒤, 브라우저 인쇄 기능으로 PDF 저장)

자세한 기획 배경은 [PLANNING.md](PLANNING.md), 항목별 상세 기획 문서는
[docs/planning/](docs/planning/00-index.md) 참고.

## 아키텍처

- **프론트엔드**: 별도 빌드 도구 없는 순수 HTML/CSS/JS (`src/main/resources/static/`), 상태는 브라우저 localStorage에 저장.
- **백엔드**: Spring Boot(Kotlin) API 서버. 상태를 갖지 않고 AI API에 대한 프록시 역할만 한다 (DB 없음, 인증 없음).
  Claude(Anthropic)와 Gemini(Google) 두 프로바이더를 지원하며, 요청의 `provider` 값으로 라우팅된다
  (`ChatProvider` 인터페이스 + `ClaudeChatProvider`/`GeminiChatProvider`).

```
[브라우저 localStorage] --fetch(JSON)--> [Spring Boot 백엔드] --> [Anthropic Claude API]
                                                              \-> [Google Gemini API]
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
  ],
  "provider": "claude"
}
```
`provider`는 `"claude"`(기본값) 또는 `"gemini"`.

**Response**
```json
{
  "reply": "AI의 대화형 응답",
  "updatedResume": "수정을 제안하는 경우, 전체 이력서 텍스트 (없으면 null)"
}
```

### `POST /api/resume/extract`

PDF 이력서에서 텍스트를 추출한다. `multipart/form-data`의 `file` 필드로 PDF를 전송하면 `{"text": "..."}` 를 반환한다.

### `POST /api/resume/format`

이력서 텍스트를 PDF 다운로드용 템플릿 구조(HTML)로 재구성한다. AI가 이력서 내용을 고정된
클래스 구조(`.name`, `.entry`, `.skill-tags` 등)에 맞춰 정리해 반환하면, 프론트엔드가 이를
스타일링된 인쇄 미리보기로 렌더링하고 브라우저 인쇄 대화상자를 열어 PDF로 저장할 수 있게 한다.

**Request**
```json
{ "resume": "현재 이력서 전문", "provider": "claude" }
```

**Response**
```json
{ "html": "<div class=\"resume\">...</div>" }
```

## 실행 방법

API 키를 환경변수로 설정한 뒤 서버를 실행한다. `ANTHROPIC_API_KEY`는 Claude를 쓰려면 필수,
`GEMINI_API_KEY`는 Gemini를 쓰려면 필요(없어도 Claude만으로는 실행 가능).

```bash
export ANTHROPIC_API_KEY=sk-ant-...
export GEMINI_API_KEY=...   # 선택
./gradlew bootRun
```

기본적으로 `http://localhost:8080` 에서 정적 프론트엔드와 API가 함께 서빙된다.

### Docker로 실행하기

Java나 Gradle을 로컬에 설치하지 않고도 Docker만으로 실행할 수 있다.

```bash
cp .env.example .env
# .env 파일에 ANTHROPIC_API_KEY(, GEMINI_API_KEY) 값을 채운다

docker compose up --build
```

`http://localhost:8080` 에서 접속하면 된다.

## API 사용 요금

- **Claude Pro/Gemini 앱 구독과는 완전히 별개의 과금**이다. 이 프로젝트는 콘솔에서 발급받는
  API 키(`console.anthropic.com`, `aistudio.google.com`)로 호출하며, 각 API는 **토큰 사용량에
  따른 종량제**로 청구된다. Claude Pro나 Gemini 앱을 구독 중이어도 API 호출 비용은 별도로
  발생한다.
- **Anthropic (Claude Sonnet 5, 기본값)**: 입력 $3 / 출력 $15 (100만 토큰당, 2026-08-31까지
  인트로가 $2/$10 적용). 대화 한 세션(10턴 내외) 기준 대략 몇 백 원 수준.
- **Google Gemini (`gemini-2.5-flash`)**: 무료 티어 제공(요청 수 제한 있음) — 비용 부담 없이
  테스트하고 싶을 때 채팅창에서 Gemini를 선택하면 된다.
- PDF 다운로드 시에도 이력서를 템플릿에 맞게 재구성하기 위해 AI를 한 번 더 호출한다
  (`/api/resume/format`) — 일반 채팅 메시지 한 번과 비슷한 수준의 비용.
- 실제 사용량/잔액은 각 콘솔(Anthropic Console, Google AI Studio)에서 확인 가능.

## 기술 스택

- Kotlin, Spring Boot (WebMVC)
- Anthropic Claude API (`com.anthropic:anthropic-java`)
- Google Gemini API (`com.google.genai:google-genai`)
- Apache PDFBox (PDF 텍스트 추출)
