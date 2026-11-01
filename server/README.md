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

로그인 후 받은 토큰을 `Authorization: Bearer <토큰>` 헤더로 보냄. `/api/me/**` 는 모두 로그인 필요.

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/health` | 서버/DB 상태 |
| POST | `/api/auth/signup` · `/api/auth/login` | `{"loginId","password"}` → `{"token"}` |
| POST | `/api/auth/logout` | 토큰 폐기 |
| GET | `/api/game/meta` | 직업·외형 목록 (로그인 불필요) |
| GET / POST | `/api/me/hero` | 내 영웅 조회 (없으면 404 `HERO_NOT_FOUND`) / 생성 `{"name","job","appearance"}` |
| POST | `/api/me/hero/stats` | 능력치 찍기 `{"attack":1}` |
| POST / GET | `/api/me/runs` | 러닝 기록 동기화 → 보상 / 최근 20건과 보상 제외 이유 |
| GET | `/api/me/items` | 가방 |
| POST | `/api/me/items/{id}/equip` · `unequip` · `sell` | 장착 / 해제 / 판매 |
| POST | `/api/me/items/sell-bulk?maxGrade=COMMON` | 해당 등급 이하 미장착 장비 일괄 판매 |
| POST | `/api/me/steps` | 날짜별 걸음수 → 행동력 `{"days":[{"date","steps"}]}` |
| POST | `/api/me/smith/{id}/enhance` | 대장간 강화 |
| GET / POST | `/api/me/guild/hunts` · `/{slot}` | 모험자 길드 오늘의 토벌 5개 / 토벌 |
| GET / POST | `/api/me/world` · `/{1~10}/dungeon` | 10개 지역·던전 / 던전 공략 |
| GET / POST / DELETE | `/api/me/friends` · `/{id}/accept` · `/{id}` | 친구 목록·요청 `{"name"}` / 수락 / 삭제 |
| GET / POST | `/api/me/raid` | 레이드 정보 / 공격 `{"friendHeroIds":[4명]}` (주 1회) |
| GET / POST | `/api/me/shop` · `/api/me/shop/{WEAPON\|ARMOR}/draw` | 상점 / 장비 뽑기 |
| GET | `/api/ranking` | 전체 랭킹 + 내 순위 |
| GET | `/api/game/names/available?name=` | 영웅 이름 중복 확인 |
| GET | `/api/me/auction?slot=` · `/api/me/auction/mine` | 경매장 판매 목록 / 내 판매 내역·등록 가능 장비 |
| POST | `/api/me/auction` · `/{id}/buy` · `/{id}/cancel` | 등록 `{"itemId","price"}` / 구매 / 등록 취소 |
| POST | `/api/dev/friend-bots` · `/api/dev/raid-reset` | **개발용** 친구 봇 4명 / 레이드 주간 제한 초기화 (`GAME_DEV_TOOLS=false` 면 비활성) |

## 게임 규칙 (`game/` 패키지)

**러닝 보상** — 1km 당 EXP 50 · 골드 20, **상한 없음** (평균 시속 25km 초과 기록만 제외)

**걸음 보상** — 100보 = 행동력 1, 하루 최대 150 (15,000보), 보유 최대 300. `POST /api/me/steps`

**영웅** — 직업 5종(기사·전사·궁수·마법사·성직자). 생성 시 주 능력치 +3, 보조 +2.
레벨업마다 포인트 3 + 주 능력치 +1. 전투력 = 능력치 합 × 3 + 레벨 × 10

**장비** — 무기(공격력)·투구(방어력)·갑옷(방어력)·장화(민첩성)·목걸이(정신력). 등급 일반~전설, 수치 = (3 + 3×티어) × 등급배율.
희귀 이상은 보조 능력치 추가. 대장간 강화 **+25** 까지 (1단계당 +10%, 성공률 100% → 3%).
실패하면 +0~4 는 유지, +5 이상은 1단계 하락

**전투 승패** — 토벌 승률 = 내 전투력 / 권장 전투력 (0.5배 10% → 1배 70% → 1.5배 100%, 0.5배 미만 도전 불가).
던전 승률 = 능력치 합 / 최소치 (1배 60% → 1.5배 100%). 패배하면 행동력만 소모

**보물 상자** — 승리하면 나무(70%, ×1) / 은(25%, ×1.5, 강화석 +1) / 황금(5%, ×2.5, 강화석 +3) 상자. 던전 장비 확률 50 / 75 / 100%.
레이드는 등급별 (S·A 황금, B·C 은, D·E 나무, F 는 패배). 출석 보상 상자 하루 1회 (`/api/me/daily`)

**모험자 길드** — 매일 토벌 의뢰 5개: 쉬움 2 (레벨-2, -3) · 적정 2 (레벨) · 도전 1 (레벨+1). 권장 전투력 이상이면 토벌, 골드 보상

**던전** (`game/Region.kt`) — 초보자 마을(Lv1) ~ 마왕성 외곽(Lv46) 10개 지역. 최소 레벨 1 + 5(티어-1), 최소 능력치 합 20 + 24(티어-1).
보상: 강화석 2×티어, 골드 40×티어, 60% 확률로 지역 티어 장비 (고티어일수록 높은 등급 확률)

**상점** — 골드로 무기 / 방어구(투구·갑옷·장화) 뽑기. 가격 150 × 내 레벨 지역 티어, 등급 확률 60 / 28 / 9 / 2.5 / 0.5%

**랭킹** — 점수 = 레벨 × 100 + 전투력 (테스트 봇 제외). 영웅 이름은 대소문자 무시하고 중복 불가

**경매장** — 미장착 장비를 즉시 구매가로 등록 (최대 10개), 판매 시 수수료 5%. 등록 중인 장비는 장착·강화·판매 불가

**레이드** (`game/Raid.kt`) — 주 1회(월요일 0시 초기화). 본인 + 친구 4명, 10턴. 누적 데미지 3,000 / 9,000 / 20,000 에서 마룡이 진화하며 받는 피해 100% → 45%.
총 데미지로 F~S 등급 (S 30,000+), A 이상은 목걸이 보상

## 구조

```
src/main/kotlin/com/example/healthrpg
├── common/   헬스체크, 예외 처리
├── auth/     회원가입·로그인·토큰
├── hero/     영웅 생성·능력치·전투력
├── run/      러닝 기록 동기화와 보상
├── item/     장비·가방·대장간
├── guild/    모험자 길드 일일 토벌
├── world/    10개 지역 던전
├── friend/   친구
├── raid/     레이드
├── dev/      개발용 API
├── game/     게임 규칙·밸런스 숫자
src/main/resources/db/migration   Flyway SQL (스키마 변경은 V2__*.sql 추가로)
```
