# Health RPG Server

Spring Boot 4 + Kotlin + JPA + PostgreSQL (Flyway 마이그레이션)

## 실행

```bash
# 1. DB 띄우기 (Docker 필요)
docker compose up -d

# 2. 서버 실행 (기본 포트 8080)
./gradlew bootRun

# 3. 확인
curl localhost:8080/api/health   # {"status":"UP","database":"UP"}
```

DB 접속 정보는 환경변수로 바꿀 수 있음: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`

## 테스트

```bash
./gradlew test   # H2(PostgreSQL 모드) 사용, Docker 불필요
```

## API

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/health` | 서버/DB 상태 |
| POST | `/api/users` | 사용자 등록 `{"deviceId","nickname"}` — 같은 deviceId면 기존 사용자 반환 |
| GET | `/api/users/{id}` | 사용자 조회 (레벨/경험치 포함) |
| POST | `/api/users/{id}/steps` | 날짜별 걸음수 동기화 (같은 날짜는 덮어씀). 응답에 이번 보상과 캐릭터 상태 포함 |
| GET | `/api/users/{id}/character` | 캐릭터: 레벨, 경험치, 골드, 오늘 걸음수, 퀘스트 진행 |
| GET | `/api/users/{id}/steps?from=2026-10-01&to=2026-10-07` | 기간별 걸음수 + 합계 |

걸음수 동기화 예시:

```bash
curl -X POST localhost:8080/api/users/1/steps -H 'Content-Type: application/json' \
  -d '{"entries":[{"date":"2026-10-09","steps":4500,"source":"com.sec.android.app.shealth"}]}'
```

## 게임 규칙 (`game/GameRules.kt`, `game/Quest.kt`)

- 100걸음 = 1 EXP, 걸음으로 얻는 EXP 는 하루 최대 300 (30,000보)
- 레벨업 필요 EXP: Lv1→2 는 100, 레벨마다 50씩 증가
- 같은 날을 여러 번 동기화해도 늘어난 걸음수만큼만 EXP 지급 (`daily_steps.exp_awarded`)
- 퀘스트 (기간마다 1번, 동기화할 때 자동 완료):

| 퀘스트 | 목표 | 보상 |
|---|---|---|
| 일일 | 오늘 5,000보 | EXP 20, 50 G |
| 일일 | 오늘 10,000보 | EXP 30, 100 G |
| 주간 | 이번 주(월~일) 50,000보 | EXP 100, 300 G |

## 구조

```
src/main/kotlin/com/example/healthrpg
├── common/   헬스체크, 예외 처리
├── user/     사용자(플레이어) — 레벨, 경험치, 골드
├── game/     경험치·레벨 규칙, 퀘스트, 캐릭터 API
└── steps/    날짜별 걸음수 (user_id + date 유니크)
src/main/resources/db/migration   Flyway SQL (스키마 변경은 V2__*.sql 추가로)
```
