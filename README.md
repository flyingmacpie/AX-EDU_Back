# AX-EDU_Back
AX-EDU 해커톤 BACKEND 소스코드 — 채팅·민원 API 서버 (Spring Boot)

프론트엔드(`LeeJaeChang/AX-EDU_front`)가 기대하는 JSON 형식(`프론트엔드 API 계약서.md`)에 맞춘 채팅·민원 API 서버입니다.
프론트에서 `VITE_USE_MOCK=false` 로 바꾸고 `VITE_API_HOST=http://localhost:8080` 으로 두면 연동됩니다.

- Java 21, Spring Boot 3.5, Spring Data JPA, Spring WebSocket, jjwt
- DB: 로컬은 H2 메모리(PostgreSQL 호환 모드, 데모 시드 자동 입력), 운영은 PostgreSQL 15+ (`AXEDU DB스키마 설계서` 기준)
- **AI 기능과 규정 API 는 차후 작성 예정** — 자리표시자로만 동작합니다 (아래 "차후 작성" 참고)

## 실행

```bash
./gradlew bootRun            # 기본 프로필 local (H2 + 시드 데이터)
./gradlew test               # 계약 형식 테스트
```

로컬에서 토큰 받기 (local 프로필 전용):

```
GET http://localhost:8080/api/axedu/v1/dev/token?userId=usr_t1
```

받은 `accessToken` 을 브라우저 `localStorage['axedu.accessToken']` 에 넣으면 됩니다.
시드 계정: `usr_t1`(교사 김수학, 3-9 담임) · `usr_t2`(교사 이영어) · `usr_p1`·`usr_p2`(학부모) · `usr_s1`~`usr_s6`(학생) · `usr_rt1`(민원대응팀) — 프론트 목 서버와 같은 사용자·방·메시지·제출된 민원(`cmp_seed1`)입니다.
H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:axedu`, 사용자 `sa`)

## API 문서 (Swagger)

서버 실행 후 브라우저에서 열면 모든 API 를 확인·호출해 볼 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs` (Postman 등으로 가져오기 가능)

호출해 보기: **Dev** 그룹 `GET /dev/token` 에 `usr_t1`(교사)·`usr_p1`(학부모) 등을 넣고 실행 → 받은 `accessToken` 을 오른쪽 위 **Authorize** 에 붙여 넣기.
문서 설정은 `config/OpenApiConfig.java`, `prod` 프로필에서는 기본으로 꺼져 있습니다 (`AXEDU_SWAGGER_ENABLED=true` 로 켬).

운영(PostgreSQL):

```bash
SPRING_PROFILES_ACTIVE=prod \
AXEDU_DB_URL='jdbc:postgresql://host:5432/axedu?stringtype=unspecified' AXEDU_DB_USER=... AXEDU_DB_PASSWORD=... \
AXEDU_JWT_SECRET=... AXEDU_CRYPTO_KEY=<Base64 32바이트> AXEDU_FILE_SIGN_KEY=... \
AXEDU_FRONT_ORIGIN=https://front.example.com AXEDU_PUBLIC_BASE_URL=https://api.example.com \
java -jar build/libs/axedu-backend-0.1.0.jar
```

`prod` 는 테이블을 만들지 않습니다(`ddl-auto: none`). 스키마는 DB 설계서의 DDL 로 먼저 만들어 두세요.
`stringtype=unspecified` 는 enum·enum[] 컬럼(`user_role`, `day_of_week[]` 등)에 문자열을 그대로 넣기 위해 필요합니다.

## 구조 (MVC)

```
com.axedu
├─ controller/   REST 컨트롤러 (요청·응답 봉투만 담당)
├─ service/      비즈니스 규칙 (권한·상태 전이·대화 가능 시간·이벤트 발행)
│  ├─ ai/        AiClient 인터페이스 + StubAiClient (차후 실제 AI 로 교체)
│  └─ storage/   FileStorage 인터페이스 + LocalFileStorage (서명 URL)
├─ repository/   Spring Data JPA
├─ entity/       DB 설계서 테이블과 1:1 (enum·JSONB 값 포함)
├─ dto/          계약서 JSON 모양 (record)
├─ websocket/    /ws/axedu/v1 연결·세션·이벤트 전송
├─ config/       Web·CORS·Jackson(KST)·WebSocket·로컬 시드
└─ common/       응답 봉투, 에러 코드, JWT 인증, 원문 암호화, ULID
```

## 구현한 API (Base `/api/axedu/v1`)

| 구분 | API |
| --- | --- |
| 채팅 | `GET /chat/rooms` · `GET /chat/contacts` · `POST /chat/rooms` · `GET /chat/rooms/{id}` · `PATCH /chat/rooms/{id}/settings` · `DELETE /chat/rooms/{id}/members/me` · `GET·POST /chat/rooms/{id}/messages` · `POST /chat/rooms/{id}/read` · `GET·PUT /me/chat-settings` |
| 파일 | `POST /files` (multipart `file`) · `GET /files/{id}/content?expires&sig` (서명 URL) |
| 민원 | `POST /complaints` · `/explanation/retry` · `/close` · `POST /summary-card` · `/summary-card/revisions` · `PATCH /summary-card` · `/submit` · `GET /complaints` · `GET /complaints/{id}` · `/acknowledge` · `/reply-drafts` · `/reply` · `/escalations` · `/resolve` |
| 사용자 | `GET /me` (**추가**) |
| 규정 | `GET /regulations` (스텁, 빈 목록) |
| WebSocket | `ws://{host}/ws/axedu/v1?token=` — ping/pong + `message.created` `message.read` `room.created` `room.updated` `room.unread_changed` `complaint.status_changed` `complaint.card_updated` `escalation.created` `escalation.updated` |

계약서 0장 규칙 반영:
- 민원 상태 변경 API 는 전체 `Complaint`, 카드 API 는 카드 자체(`SummaryCardResult`), 도움 요청은 전체 `Escalation`
- 메시지 목록은 `before`·`after`·기본 모두 최신순 + `meta.hasMore`, 방·민원 목록은 `meta.nextCursor`(끝이면 null)
- 첨부만 있는 메시지는 서버가 `IMAGE`/`FILE` 결정, 같은 `clientMessageId` 재전송은 기존 메시지 반환(200)
- 교사에게는 `originalText`·`aiExplanation` 키 생략 + 제출된 카드 버전, 학부모 원문은 AES-256-GCM 암호화 저장
- 422 `AI_PROCESSING_FAILED` 는 `details.complaintId`, 카드 버전 충돌은 409 + `details.latestVersion`, 검증 실패는 `details.fields`
- 모든 시각은 `+09:00` ISO 8601, 이벤트는 커밋 후 전송

## 계약서에 없던 것 / 프론트 쪽 확인 필요

1. **`GET /me` 추가** — 학부모 `childStudentIds` 를 서버에서 내려 줍니다. 프론트 `getMe` 가 이 API 를 부르도록 바꾸는 것을 권장합니다.
   그 전까지도 동작하도록, `POST /complaints` 에서 `studentId` 가 비어 있고 자녀가 한 명이면 그 자녀로 처리합니다.
2. `GET /dev/token` — local 프로필에서만 켜지는 개발용 토큰 발급. 운영 토큰은 슈퍼스쿨 발급 토큰을 같은 키로 검증합니다 (`userId` 클레임, 없으면 `sub`).
3. 나갔던 1:1 방을 다시 만들려 하면 두 사람을 다시 참여시킨 뒤 409 `ROOM_ALREADY_EXISTS`(기존 roomId)로 안내합니다.

## 차후 작성 (TODO)

- **AI** (`service/ai/AiClient`): 규정 설명, 요약 카드 생성·수정, 감정 표현 순화 제안, 답변 초안. 지금은 `StubAiClient` 가 규칙 기반 자리표시 결과를 줍니다(원문에 `[AI실패]` 를 넣으면 첫 시도 실패 재현). 실제 구현체를 `@Primary` 빈으로 등록하면 서비스 코드는 그대로 동작합니다. 가명처리(`ComplaintService.maskedText`)도 보강 필요.
- **규정 API** (`RegulationService.search`, `POST·DELETE /regulations`): 지금은 빈 목록. 인용 조항을 응답 모양으로 바꾸는 부분(`toResponses`)은 구현됨.
- 그 밖: 슈퍼스쿨 안심전화·면담 예약 연동(`ReservationClient`), 푸시 알림(`notifications`), 오브젝트 스토리지·썸네일, TEMP 파일·멱등 키 정리 배치, 다중 서버용 WebSocket 브로커(Redis 등), RLS 용 `SET LOCAL app.school_id`, 이관 관리 API(`/escalations…`).
