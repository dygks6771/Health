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
| POST | `/api/users/{id}/steps` | 날짜별 걸음수 동기화 (같은 날짜는 덮어씀) |
| GET | `/api/users/{id}/steps?from=2026-10-01&to=2026-10-07` | 기간별 걸음수 + 합계 |

걸음수 동기화 예시:

```bash
curl -X POST localhost:8080/api/users/1/steps -H 'Content-Type: application/json' \
  -d '{"entries":[{"date":"2026-10-09","steps":4500,"source":"com.sec.android.app.shealth"}]}'
```

## 구조

```
src/main/kotlin/com/example/healthrpg
├── common/   헬스체크, 예외 처리
├── user/     사용자(플레이어) — level, exp 컬럼은 RPG 시스템용
└── steps/    날짜별 걸음수 (user_id + date 유니크)
src/main/resources/db/migration   Flyway SQL (스키마 변경은 V2__*.sql 추가로)
```
