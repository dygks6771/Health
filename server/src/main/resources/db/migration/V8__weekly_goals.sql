-- 주간 원정 목표: 주가 시작될 때(첫 동기화·조회 시) 지난 4주 기록으로 목표를 정해 저장 (주 중간에 바뀌지 않도록)
CREATE TABLE weekly_goals (
    id           BIGSERIAL PRIMARY KEY,
    hero_id      BIGINT  NOT NULL REFERENCES heroes (id) ON DELETE CASCADE,
    week_start   DATE    NOT NULL,
    goal_meters  INTEGER NOT NULL,
    claimed      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_weekly_goals UNIQUE (hero_id, week_start)
);
