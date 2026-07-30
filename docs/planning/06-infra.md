# 06. 인프라 & 실행 환경

## Docker 컨테이너화
- 목적: 다른 사람이 로컬에 Java/Gradle을 설치하지 않고도 이 프로젝트를 실행해볼 수 있게 하기
  위함.
- 구성:
  - `Dockerfile`: 멀티스테이지 빌드. 빌드 스테이지(`eclipse-temurin:25-jdk-jammy`)에서
    `./gradlew bootJar`로 fat jar를 만들고, 실행 스테이지(`eclipse-temurin:25-jre-jammy`,
    non-root `appuser`)에서 `java -jar app.jar`로 구동한다.
  - `docker-compose.yml`: `.env` 파일에서 환경변수를 읽어 8080 포트로 서비스 실행.
  - `.dockerignore`: `build/`, `.gradle/`, `.git` 등 빌드 컨텍스트에서 제외.
  - `.env.example`: 필요한 환경변수 템플릿. 실제 `.env`는 `.gitignore`에 등록되어 커밋되지
    않는다.
- 실행 방법: `cp .env.example .env` 후 키 채우고 `docker compose up --build`.
  자세한 사용법은 루트 `README.md` 참고.

## 설정 파일: `application.properties` → `application.yml`
- 최초에는 `application.properties`로 시작했으나, 계층적 설정(특히 provider별 API
  키/모델 그룹핑)을 더 명확히 표현하기 위해 `application.yml`로 전환했다.
- 현재 구조:
  ```yaml
  spring:
    application:
      name: resume-matcher
    servlet:
      multipart:
        max-file-size: 5MB
        max-request-size: 5MB

  anthropic:
    api-key: "${ANTHROPIC_API_KEY:}"
    model: claude-sonnet-5

  gemini:
    api-key: "${GEMINI_API_KEY:}"
    model: gemini-2.5-flash
  ```

## 환경변수
| 변수 | 필수 여부 | 용도 |
|---|---|---|
| `ANTHROPIC_API_KEY` | 필수 (Claude 사용 시) | `anthropic.api-key`로 주입, `ClaudeChatProvider`가 사용 |
| `GEMINI_API_KEY` | 선택 (Gemini 사용 시) | `gemini.api-key`로 주입, `GeminiChatProvider`가 사용. 없으면 Gemini 선택 시 요청이 인증 오류(403)로 실패 |

## AI 모델 선택 이력 (비용 vs 품질 트레이드오프 실험)
개인 프로젝트 특성상 API 비용이 신경 쓰여, 실사용 중 아래 순서로 모델을 바꿔가며 테스트했다.
1. `claude-opus-5` (초기값) — 품질은 가장 좋지만 비용이 가장 높음.
2. `claude-haiku-4-5` — 가장 저렴하지만, 실제 이력서 첨삭처럼 품질이 중요한 용도엔 아쉬움.
3. `claude-sonnet-5` (현재 기본값) — 비용과 품질의 균형점으로 최종 선택. 세션당(대화 10턴
   기준) 대략 몇 백 원 수준으로 실사용에 부담 없는 수준임을 확인.

이 과정에서 `anthropic.model`을 환경변수가 아닌 `application.yml` 프로퍼티로 노출해두어,
모델 교체가 코드 수정 없이 설정값 변경만으로 가능하도록 되어 있다.

## 발견된 버그: 응답 잘림으로 인한 JSON 파싱 실패
- 실사용(실제 이력서+JD) 중 `com.anthropic.errors.AnthropicInvalidDataException` /
  `JsonEOFException` 발생.
- 원인: `ClaudeChatProvider`(구 `ChatService`)의 `maxTokens`가 4096으로 설정되어 있었는데,
  구조화 응답의 `updatedResume` 필드에 이력서 전문을 통째로 담다 보니 4096 토큰을 넘겨
  응답이 문자열 중간에서 잘렸고, 그 결과 불완전한 JSON을 파싱하려다 에러가 났음.
- 조치: `maxTokens`를 16000으로 상향. 이후 동일 시나리오 재현 테스트로 정상 동작 확인.
- 참고: 짧은 테스트 문자열("test resume", "test jd")로는 재현되지 않고, 실제 분량의
  이력서/JD로만 재현되는 문제였음 — 테스트 시 실데이터로도 검증할 필요가 있음을 보여준 사례.
