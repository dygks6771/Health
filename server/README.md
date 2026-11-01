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
| GET | `/api/users/{id}` | 사용자 조회 |
| POST | `/api/users/{id}/runs` | **러닝 기록 동기화** → 보상 + 캐릭터 상태 |
| GET | `/api/users/{id}/character` | 캐릭터: 레벨, 경험치, 골드, 행동력, 스탯, 무기, 장신구, 러닝 거리 |
| POST | `/api/users/{id}/stats` | 스탯 포인트 찍기 `{"attack":1,"defense":0,"agility":0,"spirit":0}` |
| POST | `/api/users/{id}/weapon/enhance` | 무기 강화 +1 (강화석 + 골드 소모) |
| GET | `/api/users/{id}/content` | 토벌/던전/레이드 1~20단계 조건과 도전 가능 여부 |
| POST | `/api/users/{id}/content/{HUNT\|DUNGEON\|RAID}/{1~20}` | 도전 → 보상 |
| POST/GET | `/api/users/{id}/steps` | 걸음수 기록 (개발자 탭 확인용, 보상 없음) |

러닝 동기화 예시:

```bash
curl -X POST localhost:8080/api/users/1/runs -H 'Content-Type: application/json' \
  -d '{"runs":[{"externalId":"hc-1","startTime":"2026-11-01T00:00:00Z","endTime":"2026-11-01T00:30:00Z","distanceMeters":5000}]}'
```

## 게임 규칙 (`game/GameRules.kt`, `game/ContentType.kt`)

**러닝 보상** (Health Connect 러닝 기록 1건당 한 번만)
- 1km 당 EXP 50 · 골드 20 · 행동력 10 (행동력 최대 300)
- 하루 최대 20km 까지만 보상, 평균 25km/h 를 넘는 기록은 보상 제외

**레벨·스탯**
- 레벨업 필요 EXP: Lv1→2 는 100, 레벨마다 50씩 증가
- 레벨업마다 스탯 포인트 3 → 공격력/방어력/민첩성/정신력에 자유롭게 (기본값 각 5)
- 무기 강화: +N → +N+1 에 강화석 N+1개, 골드 100×(N+1). 강화 1당 공격력 +2
- 무기 등급: +0~3 일반, +4~7 고급, +8~11 희귀, +12~15 영웅, +16~20 전설

**컨텐츠** (각 1~20단계, 조건을 모두 만족하면 행동력을 쓰고 클리어)

| 컨텐츠 | 행동력 | 보상 | 최소 레벨 | 필요 능력치 (단계 d) |
|---|---|---|---|---|
| 토벌 | 10 | 골드 50×d | 1 + 2(d-1) | 공격력·민첩성 5 + 2(d-1) |
| 던전 | 20 | 강화석 2×d 개 | 3 + 2(d-1) | 공격력·방어력 5 + 2(d-1) |
| 레이드 | 30 | 장신구 1개 | 5 + 2(d-1) | 4가지 모두 5 + (d-1) |

- 최소 무기 등급: 1~4단계 일반, 5~8 고급, 9~12 희귀, 13~16 영웅, 17~20 전설
- 장신구: 레이드 단계 등급으로 반지(공격력)/팔찌(방어력)/귀걸이(민첩성)/목걸이(정신력) 중 하나.
  보너스 일반 3 ~ 전설 15. 종류별로 가장 좋은 것 자동 장착

## 구조

```
src/main/kotlin/com/example/healthrpg
├── common/   헬스체크, 예외 처리
├── user/     사용자(플레이어) — 레벨, 경험치, 골드, 스탯, 장비
├── run/      러닝 기록 동기화와 보상
├── game/     게임 규칙, 캐릭터·스탯·강화, 토벌/던전/레이드
└── steps/    날짜별 걸음수 (기록용)
src/main/resources/db/migration   Flyway SQL (스키마 변경은 V2__*.sql 추가로)
```
