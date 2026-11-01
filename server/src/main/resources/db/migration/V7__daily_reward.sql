-- 하루 한 번 출석 보상 상자
CREATE TABLE daily_rewards (
    id           BIGSERIAL PRIMARY KEY,
    hero_id      BIGINT    NOT NULL REFERENCES heroes (id) ON DELETE CASCADE,
    reward_date  DATE      NOT NULL,
    chest        VARCHAR(10) NOT NULL,
    claimed_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_daily_rewards UNIQUE (hero_id, reward_date)
);
