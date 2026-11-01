package com.example.healthrpg.game

/**
 * 토벌 / 던전 / 레이드. 각각 난이도 1~20 단계.
 * 난이도가 오를수록 최소 레벨, 최소 장비(무기) 등급, 최소 능력치가 올라감.
 */
enum class ContentType(
    val displayName: String,
    /** 1단계 최소 레벨. 단계마다 +2 */
    private val baseLevel: Int,
    /** 입장 조건으로 보는 능력치 */
    val requiredStats: List<Stat>,
    /** 단계마다 필요한 능력치 증가량 (1단계는 기본값 5) */
    private val statPerDifficulty: Int,
    /** 입장 시 소모하는 행동력 (러닝으로 충전) */
    val staminaCost: Int,
) {
    /** 토벌: 골드 */
    HUNT("토벌", baseLevel = 1, listOf(Stat.ATTACK, Stat.AGILITY), statPerDifficulty = 2, staminaCost = 10),

    /** 던전: 무기 강화석 */
    DUNGEON("던전", baseLevel = 3, listOf(Stat.ATTACK, Stat.DEFENSE), statPerDifficulty = 2, staminaCost = 20),

    /** 레이드: 장신구 */
    RAID("레이드", baseLevel = 5, Stat.entries.toList(), statPerDifficulty = 1, staminaCost = 30),
    ;

    fun requirement(difficulty: Int): ContentRequirement {
        require(difficulty in DIFFICULTIES) { "difficulty must be 1..20" }
        val statValue = GameRules.BASE_STAT + statPerDifficulty * (difficulty - 1)
        return ContentRequirement(
            minLevel = baseLevel + 2 * (difficulty - 1),
            minGrade = Grade.forDifficulty(difficulty),
            minStats = requiredStats.associateWith { statValue },
        )
    }

    fun rewardDescription(difficulty: Int): String = when (this) {
        HUNT -> "골드 ${huntGold(difficulty)}"
        DUNGEON -> "무기 강화석 ${dungeonStones(difficulty)}개"
        RAID -> "${Grade.forDifficulty(difficulty).displayName} 장신구 1개"
    }

    companion object {
        val DIFFICULTIES = 1..20

        fun huntGold(difficulty: Int): Long = 50L * difficulty

        fun dungeonStones(difficulty: Int): Int = 2 * difficulty
    }
}

data class ContentRequirement(
    val minLevel: Int,
    val minGrade: Grade,
    val minStats: Map<Stat, Int>,
)
