-- 사용자 (= 플레이어). 레벨/경험치는 RPG 시스템용으로 미리 잡아둔 컬럼
CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    device_id   VARCHAR(100) NOT NULL UNIQUE,
    nickname    VARCHAR(30)  NOT NULL,
    level       INT          NOT NULL DEFAULT 1,
    exp         BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 하루 단위 걸음수. 앱이 여러 번 동기화해도 (user_id, step_date) 당 한 행만 유지(upsert)
CREATE TABLE daily_steps (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    step_date   DATE         NOT NULL,
    steps       BIGINT       NOT NULL CHECK (steps >= 0),
    source      VARCHAR(100),
    synced_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_daily_steps_user_date UNIQUE (user_id, step_date)
);
