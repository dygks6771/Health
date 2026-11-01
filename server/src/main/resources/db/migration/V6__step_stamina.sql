-- 행동력은 걸음수로 충전 (하루 상한), 경험치·골드는 러닝 km (상한 없음)
CREATE TABLE step_days (
    id               BIGSERIAL PRIMARY KEY,
    hero_id          BIGINT    NOT NULL REFERENCES heroes (id) ON DELETE CASCADE,
    step_date        DATE      NOT NULL,
    steps            BIGINT    NOT NULL,
    -- 이 날 걸음수로 이미 지급한 행동력. 다시 동기화하면 늘어난 만큼만 지급
    stamina_awarded  INT       NOT NULL,
    synced_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_step_days UNIQUE (hero_id, step_date)
);
