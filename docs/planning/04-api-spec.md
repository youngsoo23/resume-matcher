# 04. API 명세 (현재 코드 기준)

## `POST /api/chat`
이력서 코칭 대화 한 턴을 처리한다. 상태를 저장하지 않으므로, 매 요청마다 이력서/JD/전체
대화 이력을 함께 보내야 한다.

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
`provider`는 `"claude"`(기본값) 또는 `"gemini"`. 프론트엔드 채팅 패널 상단의 드롭다운에서
선택한 값이 그대로 전달된다.

**Response** `200 OK`
```json
{
  "reply": "AI의 대화형 응답 (설명/제안 코멘트)",
  "updatedResume": "수정된 이력서 전문 (수정 제안이 없으면 null)"
}
```

**동작 방식**
- 서버는 `resume`, `jobDescription`을 시스템 프롬프트(`ChatPrompts.systemPrompt`)에 삽입하고,
  `messages`를 user/assistant(Gemini는 user/model) 턴으로 그대로 각 AI의 Messages API에
  전달한다.
- `ChatController`가 `provider` 값으로 `ChatProvider` 구현체를 선택해 라우팅한다
  (`ClaudeChatProvider` / `GeminiChatProvider`).
- 두 프로바이더 모두 응답을 `ChatReplyResult`(reply, updatedResume) 구조로 강제한다
  — Claude는 `outputConfig`, Gemini는 `responseSchema` + `responseMimeType: application/json`
  후 Jackson으로 파싱.
- `updatedResume`을 반환할 때는 항상 이력서 전체 텍스트여야 하며, 부분 diff는 허용하지 않는다
  (시스템 프롬프트에 명시).
- 모델은 `anthropic.model`/`gemini.model` 설정으로 바뀔 수 있으며 기본값은 각각
  `claude-sonnet-5`, `gemini-2.5-flash`.
- Claude 쪽 `maxTokens`는 16000 (이력서 전문을 통째로 반환해야 해서 4096으로는 응답이
  잘려 JSON 파싱 에러가 났던 이력이 있음 — [06-infra.md](06-infra.md) 참고).

**에러 케이스**
- AI가 구조화된 컨텐츠를 반환하지 않으면 `IllegalStateException` → 500.
  (프론트는 이를 `서버 오류 (500)`으로 표시)
- 알 수 없는 `provider` 값이면 `IllegalArgumentException` → 500.
- `gemini.api-key`가 비어있는 상태로 Gemini를 선택하면 Gemini API가 403을 반환하고,
  이는 그대로 500으로 전파된다.

## `POST /api/resume/extract`
PDF 이력서에서 텍스트를 추출한다.

**Request**: `multipart/form-data`, 필드명 `file` (PDF 바이너리)

**Response** `200 OK`
```json
{ "text": "추출된 이력서 텍스트" }
```

**에러 응답**
| 상황 | 상태 코드 | 응답 |
|---|---|---|
| 빈 파일 | 400 | `{"message": "빈 파일입니다."}` |
| PDF 파싱 자체 실패(손상/미지원 형식) | 400 | `{"message": "PDF에서 텍스트를 추출하지 못했습니다..."}` |
| 텍스트 레이어 없음(스캔 이미지 PDF) | 422 | `{"message": "PDF에서 텍스트를 찾을 수 없습니다..."}` |

구현: `ResumeExtractionService.kt` (Apache PDFBox `PDFTextStripper` 사용).

## 인증 / 보안
- 엔드포인트에 인증 없음 (로컬 개인용 전제).
- `ANTHROPIC_API_KEY`는 서버 환경변수로만 존재하며 API 응답에 포함되지 않는다.
