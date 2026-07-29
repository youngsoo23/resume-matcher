# 02. 요구사항

상태 표기: ✅ 구현 완료 · 🚧 부분 구현 · ⬜ 미구현(계획만 있음)

## 기능 요구사항

### 이력서 입력
- ✅ 텍스트/마크다운 붙여넣기 (`textarea`)
- ✅ 파일 업로드 (`.txt`, `.md`는 브라우저에서 바로 읽고, `.pdf`는 서버 `/api/resume/extract`로
  텍스트 추출)
- ✅ 입력 시 자동 저장(디바운스) + "저장됨" 인디케이터 표시
- ⬜ 스캔 이미지 PDF의 OCR 지원 (현재는 텍스트 레이어 없는 PDF는 에러 반환)

### JD 입력
- ✅ 텍스트 붙여넣기, localStorage 자동 저장
- ⬜ 여러 JD를 동시에 관리 (현재는 JD 슬롯 1개, 새 JD 붙여넣으면 이전 것 덮어씀)

### AI 대화
- ✅ `POST /api/chat`으로 이력서 + JD + 대화 이력을 매 요청마다 함께 전송
- ✅ Claude가 구조화된 응답(`reply`, `updatedResume`)을 반환하도록 강제 (`outputConfig`)
- ✅ 답변 대기 중 "AI가 답변을 작성 중입니다..." 표시, 전송 중 입력 비활성화
- ✅ Enter로 전송, Shift+Enter로 줄바꿈
- 🚧 에러 처리: 네트워크/서버 오류 시 채팅창에 오류 메시지를 시스템 메시지로 표시하지만,
  재시도 버튼은 없음(사용자가 다시 입력해야 함)

### 수정본 적용 / 버전 관리
- ✅ AI 응답에 `updatedResume`이 포함되면 "이 수정본 적용" 버튼 표시
- ✅ 적용 시: 현재 이력서 텍스트 교체 + 새 버전으로 히스토리에 추가 + 해당 채팅 메시지에
  "✓ 이 수정본을 적용했습니다" 라벨 고정
- ✅ 버전 목록에서 각 버전을 클릭해 복원 가능 (시간 + 앞부분 미리보기 텍스트로 식별)
- ✅ 버전 전체 삭제 (확인 다이얼로그 포함)
- ⬜ 두 버전 간 diff(변경점) 시각화

### 내보내기
- ✅ 클립보드 복사 (`navigator.clipboard`)
- ✅ `.txt` 파일 다운로드 (파일명에 타임스탬프 포함)

## 비기능 요구사항
- **데이터 저장**: 모든 상태(이력서, JD, 버전, 채팅 기록)는 브라우저 `localStorage`에만 저장.
  서버는 DB 없이 무상태(stateless) 프록시로만 동작 (`ChatService`, `ResumeExtractionService`
  모두 요청 단위로만 동작).
- **인증**: 없음. 로컬에서 단일 사용자가 쓰는 것을 전제로 함.
- **비밀키 보호**: `ANTHROPIC_API_KEY`는 서버 환경변수(`application.properties`의
  `anthropic.api-key`)로만 주입되고 프론트엔드에는 절대 노출되지 않음.
- **모델 설정**: `anthropic.model` 프로퍼티로 교체 가능 (기본값 `claude-opus-5`).
- **프론트엔드**: 별도 빌드 도구 없는 순수 HTML/CSS/JS. `src/main/resources/static/`에서
  Spring Boot가 정적 파일로 서빙.
- **에러 처리 정책**: PDF 추출 실패 시 원인을 구분해 반환
  (`PdfUnreadableException` → 400, `PdfNoTextException` → 422).
