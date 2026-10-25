-- RPG 시스템: 골드, 날짜별 지급 경험치(중복 지급 방지), 퀘스트 완료 기록
ALTER TABLE users ADD COLUMN gold BIGINT NOT NULL DEFAULT 0;

-- 그날 걸음수로 이미 지급한 경험치. 같은 날을 다시 동기화하면 늘어난 만큼만 추가 지급
ALTER TABLE daily_steps ADD COLUMN exp_awarded INT NOT NULL DEFAULT 0;

CREATE TABLE quest_completions (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    quest_code    VARCHAR(50)  NOT NULL,
    -- 일일 퀘스트는 그날, 주간 퀘스트는 그 주 월요일
    period_start  DATE         NOT NULL,
    completed_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_quest_completion UNIQUE (user_id, quest_code, period_start)
);
