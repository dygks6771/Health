-- 계정·영웅·장비·모험자 길드·던전·친구·레이드 구조로 전면 개편.
-- 개발 중 테스트 데이터만 있으므로 기존 게임 테이블은 삭제하고 새로 만든다.
DROP TABLE IF EXISTS content_clears, accessories, run_sessions, daily_steps, users CASCADE;

CREATE TABLE accounts (
    id             BIGSERIAL    PRIMARY KEY,
    login_id       VARCHAR(20)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 로그인 토큰. 원문은 앱에만 있고 서버는 SHA-256 해시만 저장
CREATE TABLE auth_tokens (
    id          BIGSERIAL   PRIMARY KEY,
    account_id  BIGINT      NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 계정당 영웅 1명
CREATE TABLE heroes (
    id              BIGSERIAL   PRIMARY KEY,
    account_id      BIGINT      NOT NULL UNIQUE REFERENCES accounts (id) ON DELETE CASCADE,
    name            VARCHAR(12) NOT NULL UNIQUE,
    job             VARCHAR(20) NOT NULL,
    appearance      INT         NOT NULL,
    level           INT         NOT NULL,
    exp             BIGINT      NOT NULL,
    gold            BIGINT      NOT NULL,
    stamina         INT         NOT NULL,
    stat_points     INT         NOT NULL,
    attack          INT         NOT NULL,
    defense         INT         NOT NULL,
    agility         INT         NOT NULL,
    spirit          INT         NOT NULL,
    enhance_stones  INT         NOT NULL,
    is_bot          BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE run_sessions (
    id               BIGSERIAL    PRIMARY KEY,
    hero_id          BIGINT       NOT NULL REFERENCES heroes (id) ON DELETE CASCADE,
    external_id      VARCHAR(200) NOT NULL,
    run_date         DATE         NOT NULL,
    start_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time         TIMESTAMP WITH TIME ZONE NOT NULL,
    distance_meters  INT          NOT NULL,
    counted_meters   INT          NOT NULL,
    exp_awarded      INT          NOT NULL,
    gold_awarded     INT          NOT NULL,
    stamina_awarded  INT          NOT NULL,
    rejected_reason  VARCHAR(100),
    synced_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_run_sessions_hero_external UNIQUE (hero_id, external_id)
);
CREATE INDEX idx_run_sessions_hero_date ON run_sessions (hero_id, run_date);

CREATE TABLE items (
    id           BIGSERIAL   PRIMARY KEY,
    hero_id      BIGINT      NOT NULL REFERENCES heroes (id) ON DELETE CASCADE,
    slot         VARCHAR(20) NOT NULL,
    name         VARCHAR(50) NOT NULL,
    grade        INT         NOT NULL,
    tier         INT         NOT NULL,
    main_value   INT         NOT NULL,
    sub_stat     VARCHAR(20),
    sub_value    INT         NOT NULL,
    enhance      INT         NOT NULL,
    equipped     BOOLEAN     NOT NULL,
    obtained_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_items_hero ON items (hero_id);

-- 모험자 길드 일일 토벌 의뢰 (하루 5개, 그날 처음 볼 때 생성)
CREATE TABLE daily_hunts (
    id              BIGSERIAL   PRIMARY KEY,
    hero_id         BIGINT      NOT NULL REFERENCES heroes (id) ON DELETE CASCADE,
    board_date      DATE        NOT NULL,
    slot            INT         NOT NULL,
    difficulty      VARCHAR(10) NOT NULL,
    monster         VARCHAR(50) NOT NULL,
    region          INT         NOT NULL,
    target_level    INT         NOT NULL,
    required_power  INT         NOT NULL,
    reward_gold     INT         NOT NULL,
    cleared         BOOLEAN     NOT NULL,
    CONSTRAINT uk_daily_hunts UNIQUE (hero_id, board_date, slot)
);

CREATE TABLE dungeon_clears (
    id          BIGSERIAL PRIMARY KEY,
    hero_id     BIGINT    NOT NULL REFERENCES heroes (id) ON DELETE CASCADE,
    region      INT       NOT NULL,
    cleared_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_dungeon_clears_hero ON dungeon_clears (hero_id);

CREATE TABLE friendships (
    id            BIGSERIAL   PRIMARY KEY,
    requester_id  BIGINT      NOT NULL REFERENCES heroes (id) ON DELETE CASCADE,
    addressee_id  BIGINT      NOT NULL REFERENCES heroes (id) ON DELETE CASCADE,
    status        VARCHAR(10) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_friendships UNIQUE (requester_id, addressee_id)
);

CREATE TABLE raid_records (
    id            BIGSERIAL   PRIMARY KEY,
    hero_id       BIGINT      NOT NULL REFERENCES heroes (id) ON DELETE CASCADE,
    total_damage  BIGINT      NOT NULL,
    final_stage   INT         NOT NULL,
    grade         VARCHAR(2)  NOT NULL,
    party         VARCHAR(200) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_raid_records_hero ON raid_records (hero_id);
