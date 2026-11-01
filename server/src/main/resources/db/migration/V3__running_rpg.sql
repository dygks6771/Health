-- 걸음수 → 러닝 거리 기반으로 전환, 스탯·장비·컨텐츠(토벌/던전/레이드) 추가

-- 걸음수 퀘스트는 토벌/던전/레이드로 대체
DROP TABLE quest_completions;

ALTER TABLE users ADD COLUMN stamina INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN stat_points INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN attack INT NOT NULL DEFAULT 5;
ALTER TABLE users ADD COLUMN defense INT NOT NULL DEFAULT 5;
ALTER TABLE users ADD COLUMN agility INT NOT NULL DEFAULT 5;
ALTER TABLE users ADD COLUMN spirit INT NOT NULL DEFAULT 5;
ALTER TABLE users ADD COLUMN weapon_enhance INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN enhance_stones INT NOT NULL DEFAULT 0;

-- 기존 사용자도 지금까지 오른 레벨만큼 스탯 포인트를 받음 (레벨당 3)
UPDATE users SET stat_points = 3 * (level - 1);

-- 러닝 기록. Health Connect 기록 ID(external_id) 로 같은 러닝을 두 번 보상하지 않음
CREATE TABLE run_sessions (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    external_id      VARCHAR(200) NOT NULL,
    run_date         DATE         NOT NULL,
    start_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time         TIMESTAMP WITH TIME ZONE NOT NULL,
    distance_meters  INT          NOT NULL,
    -- 하루 상한·속도 검사 후 실제 보상에 반영된 거리
    counted_meters   INT          NOT NULL,
    exp_awarded      INT          NOT NULL,
    gold_awarded     INT          NOT NULL,
    stamina_awarded  INT          NOT NULL,
    rejected_reason  VARCHAR(100),
    synced_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_run_sessions_user_external UNIQUE (user_id, external_id)
);
CREATE INDEX idx_run_sessions_user_date ON run_sessions (user_id, run_date);

-- 레이드 보상 장신구
CREATE TABLE accessories (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         VARCHAR(20) NOT NULL,
    grade        INT         NOT NULL,
    stat_bonus   INT         NOT NULL,
    equipped     BOOLEAN     NOT NULL,
    obtained_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_accessories_user ON accessories (user_id);

-- 컨텐츠 클리어 기록
CREATE TABLE content_clears (
    id            BIGSERIAL   PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content_type  VARCHAR(20) NOT NULL,
    difficulty    INT         NOT NULL,
    cleared_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_content_clears_user ON content_clears (user_id);
