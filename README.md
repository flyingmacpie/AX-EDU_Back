# AX-EDU_Back
AX-EDU 해커톤 BACKEND 소스코드 — 채팅·민원 API 서버 (Spring Boot)

프론트엔드(`LeeJaeChang/AX-EDU_front`)가 기대하는 JSON 형식(`프론트엔드 API 계약서.md`)에 맞춘 채팅·민원 API 서버입니다.
프론트에서 `VITE_USE_MOCK=false` 로 바꾸고 `VITE_API_HOST=http://localhost:8080` 으로 두면 연동됩니다.

- Java 21, Spring Boot 3.5, Spring Data JPA, Spring WebSocket, jjwt
- DB: PostgreSQL 15+ (`AXEDU DB 스키마 설계서 v1.1` 기준), 스키마는 Flyway 마이그레이션으로 관리. 테스트만 H2 메모리 DB
- **AI 기능과 규정 API 는 차후 작성 예정** — 자리표시자로만 동작합니다 (아래 "차후 작성" 참고)

## 실행

1. PostgreSQL 에 빈 데이터베이스를 하나 만듭니다.

```bash
createdb axedu
```

2. `.env.example` 을 `.env` 로 복사하고 DB 계정·비밀번호를 적습니다. (`.env` 는 git 에 올라가지 않습니다)

```bash
cp .env.example .env
```

3. 실행합니다. 처음 실행하면 Flyway 가 테이블 26개를 만들고, `local` 프로필이 데모 시드 데이터를 넣습니다 (`users` 가 비어 있을 때 1회).

```bash
./gradlew bootRun            # 기본 프로필 local (PostgreSQL + 시드 데이터 + /dev/token)
./gradlew test               # 계약 형식 테스트 (H2 메모리 DB 사용, PostgreSQL 불필요)
```

로컬에서 토큰 받기 (local 프로필 전용):

```
GET http://localhost:8080/api/axedu/v1/dev/token?userId=usr_t1
```

받은 `accessToken` 을 브라우저 `localStorage['axedu.accessToken']` 에 넣으면 됩니다.
시드 계정: `usr_t1`(교사 김수학, 3-9 담임) · `usr_t2`(교사 이영어) · `usr_p1`·`usr_p2`(학부모) · `usr_s1`~`usr_s6`(학생) · `usr_rt1`(민원대응팀) — 프론트 목 서버와 같은 사용자·방·메시지·제출된 민원(`cmp_seed1`)입니다.
시드 데이터를 처음부터 다시 넣으려면 DB 를 지우고 다시 만든 뒤 재실행하세요 (`dropdb axedu && createdb axedu`).

## API 문서 (Swagger)

서버 실행 후 브라우저에서 열면 모든 API 를 확인·호출해 볼 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs` (Postman 등으로 가져오기 가능)

호출해 보기: **Dev** 그룹 `GET /dev/token` 에 `usr_t1`(교사)·`usr_p1`(학부모) 등을 넣고 실행 → 받은 `accessToken` 을 오른쪽 위 **Authorize** 에 붙여 넣기.
문서 설정은 `config/OpenApiConfig.java`, `prod` 프로필에서는 기본으로 꺼져 있습니다 (`AXEDU_SWAGGER_ENABLED=true` 로 켬).

## 법령 조회 (AI_DATA, 키워드 기반)

민원 처리 흐름 중 **키워드 → 법령 조회** 단계입니다.

```
학부모 민원 → (AI) 법령 조회용 키워드 추출 → [법령 조회] → (AI) 조회 결과 + 민원으로 요약 카드 JSON → 프론트
```

- 데이터: `AI_DATA/chunks.jsonl` (법령·행정규칙·자치법규 조문, 판례·인권위 결정례·법령해석례 단락 3,762개, `AI_DATA/collect_laws.py` 로 수집)
- 검색: `ai/law_search.py` — 표준 라이브러리만 사용 (pip 설치 불필요)
  - 한글 2글자 단위 BM25 순위 → 조사가 붙은 표현('휴대전화를')도 검색됨
  - 키워드가 **단어 시작**에서 일치해야 인정 ('교체'가 '학교체육'에 걸리지 않음), 제목·조문 제목 일치는 가산점
  - 키워드를 많이 맞춘 결과가 위로, 같은 문서는 최대 2개, `perType` 으로 자료 종류를 골고루
- 서버: `ai/law_search_server.py` (기본 `127.0.0.1:8090`) ← Spring Boot `LawSearchClient` 가 호출
  - 주소는 `.env` 의 `AXEDU_LAW_SEARCH_URL` 로 바꿀 수 있음

```bash
python3 ai/law_search_server.py                  # 법령 조회 서버 (백엔드와 함께 실행)
python3 ai/law_search.py 휴대전화 압수 생활지도     # 터미널에서 바로 검색해 보기
python3 -m unittest discover -s ai -v            # 파이썬 테스트
```

API (Swagger **법령 조회** 그룹): `POST /api/axedu/v1/laws/search`

```json
{ "keywords": ["휴대전화", "압수", "생활지도"], "limit": 10, "perType": 4, "types": ["법령", "행정규칙"] }
```

응답 `data` 는 관련도 순 배열: `type`(법령·판례 등) · `title` · `article` · `articleTitle` · `text`(전문, AI 입력용) · `snippet`(미리보기) · `matchedKeywords` · `score` · `sourceUrl`.
파이썬 서버가 꺼져 있으면 500 `INTERNAL_ERROR`. 민원 흐름에서는 서비스가 `LawSearchClient` 를 직접 호출합니다.

## DB 스키마 (Flyway)

- 스키마 정의: `src/main/resources/db/migration/V1__init.sql` (설계서의 enum 21종·테이블 26개·인덱스·`updated_at` 트리거·`v_complaints_teacher` 뷰)
- 앱이 시작할 때 Flyway 가 아직 적용 안 된 버전을 순서대로 실행하고, 적용 이력은 `flyway_schema_history` 테이블에 남깁니다.
- Hibernate 는 테이블을 만들지 않고 `ddl-auto: validate` 로 **엔티티와 DB 가 맞는지 검사만** 합니다. 안 맞으면 서버가 뜨지 않고 어느 컬럼이 다른지 알려 줍니다.

스키마를 바꿀 때 (컬럼 추가 등):

1. **이미 적용된 파일(V1 등)은 절대 고치지 않습니다.** 고치면 체크섬이 달라져 서버가 뜨지 않습니다.
2. 다음 번호로 새 파일을 만듭니다. 예: `V2__add_messages_deliver_at.sql`
   ```sql
   ALTER TABLE messages ADD COLUMN deliver_at TIMESTAMPTZ(3);
   ```
   enum 값 추가는 `ALTER TYPE message_type ADD VALUE 'POLL';` (값 삭제·이름 변경은 하지 않음)
3. 엔티티(`entity/`)에 같은 필드를 추가하고 서버를 재시작합니다.

빈 DB 가 아닌 곳(이미 다른 테이블이 있는 스키마)에 처음 연결하면 Flyway 가 시작을 거부합니다. AXEDU 전용 빈 DB 를 쓰세요.

운영(PostgreSQL):

```bash
SPRING_PROFILES_ACTIVE=prod \
AXEDU_DB_HOST=... AXEDU_DB_NAME=axedu AXEDU_DB_USER=... AXEDU_DB_PASSWORD=... \
AXEDU_JWT_SECRET=... AXEDU_CRYPTO_KEY=<Base64 32바이트> AXEDU_FILE_SIGN_KEY=... \
AXEDU_FRONT_ORIGIN=https://front.example.com AXEDU_PUBLIC_BASE_URL=https://api.example.com \
java -jar build/libs/axedu-backend-0.1.0.jar
```

`prod` 도 시작 시 Flyway 로 마이그레이션합니다. 시드 데이터와 `/dev/token` 은 `local` 프로필에서만 켜집니다.
`AXEDU_DB_URL` 로 URL 을 직접 줄 때는 `?stringtype=unspecified` 를 꼭 붙이세요. enum·enum[] 컬럼(`user_role`, `day_of_week[]` 등)에 문자열을 그대로 넣기 위해 필요합니다. (HOST/PORT/NAME 으로 주면 자동으로 붙습니다)

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
- 그 밖: 슈퍼스쿨 안심전화·면담 예약 연동(`ReservationClient`), 푸시 알림(`notifications`), 오브젝트 스토리지·썸네일, TEMP 파일·멱등 키 정리 배치, 다중 서버용 WebSocket 브로커(Redis 등), RLS 정책(별도 Flyway 마이그레이션) + 요청마다 `SET LOCAL app.school_id`, 이관 관리 API(`/escalations…`).
