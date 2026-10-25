package com.example.healthrpg.game

/** 걸음수 → 경험치, 레벨업 규칙. 숫자는 여기서만 바꾸면 됨 */
object GameRules {
    /** 이 걸음수마다 경험치 1 */
    const val STEPS_PER_EXP = 100

    /** 하루에 걸음수로 얻을 수 있는 최대 경험치 (30,000보). 퀘스트 보상은 별도 */
    const val DAILY_EXP_CAP = 300

    fun expForSteps(steps: Long): Int = (steps / STEPS_PER_EXP).coerceIn(0, DAILY_EXP_CAP.toLong()).toInt()

    /** [level] 에서 다음 레벨로 가는 데 필요한 경험치: Lv1→2 는 100, 이후 레벨마다 50씩 증가 */
    fun expToNextLevel(level: Int): Long = 100L + 50L * (level - 1)

    fun progress(totalExp: Long): LevelProgress {
        var level = 1
        var remaining = totalExp
        while (remaining >= expToNextLevel(level)) {
            remaining -= expToNextLevel(level)
            level++
        }
        return LevelProgress(level = level, expInLevel = remaining, expToNextLevel = expToNextLevel(level))
    }
}

data class LevelProgress(val level: Int, val expInLevel: Long, val expToNextLevel: Long)
