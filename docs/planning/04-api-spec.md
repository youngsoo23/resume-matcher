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
  ]
}
```

**Response** `200 OK`
```json
{
  "reply": "AI의 대화형 응답 (설명/제안 코멘트)",
  "updatedResume": "수정된 이력서 전문 (수정 제안이 없으면 null)"
}
```

**동작 방식**
- 서버는 `resume`, `jobDescription`을 시스템 프롬프트에 삽입하고, `messages`를
  user/assistant 턴으로 그대로 Claude Messages API에 전달한다 (`ChatService.kt`).
- Claude 응답은 `ChatReplyResult`(reply, updatedResume) 구조로 강제되어(`outputConfig`),
  프론트가 별도 파싱 없이 바로 사용할 수 있다.
- `updatedResume`을 반환할 때는 항상 이력서 전체 텍스트여야 하며, 부분 diff는 허용하지 않는다
  (시스템 프롬프트에 명시).
- 모델은 `anthropic.model` 설정으로 바뀔 수 있으며 기본값은 `claude-opus-5`.

**에러 케이스**
- Claude가 구조화된 컨텐츠를 반환하지 않으면 `IllegalStateException` → 500.
  (프론트는 이를 `서버 오류 (500)`으로 표시)

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
