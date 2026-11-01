package com.example.healthrpg.game

/**
 * 레이드 보스. 파티(본인 + 친구 4명)가 10턴 동안 공격하고,
 * 누적 데미지가 기준을 넘을 때마다 보스가 진화(최대 4단계)하며 받는 데미지가 줄어든다.
 */
object RaidBoss {
    const val NAME = "고대 마룡 바르카스"
    const val PARTY_SIZE = 5
    const val TURNS = 10
    const val STAMINA_COST = 30

    /** 주(월요일 0시 ~ 일요일)당 도전 가능 횟수 */
    const val WEEKLY_LIMIT = 1

    val stages = listOf(
        RaidStage(1, "새끼 마룡", threshold = 0, damageRate = 1.0),
        RaidStage(2, "성체 마룡", threshold = 3_000, damageRate = 0.8),
        RaidStage(3, "고대 마룡", threshold = 9_000, damageRate = 0.6),
        RaidStage(4, "멸망의 마룡 바르카스", threshold = 20_000, damageRate = 0.45),
    )

    fun stageFor(totalDamage: Long): RaidStage = stages.last { totalDamage >= it.threshold }

    /** 최소 데미지 높은 순 */
    val grades = listOf(
        RaidGrade("S", 30_000, gold = 3_000, stones = 30, itemGrade = Grade.EPIC),
        RaidGrade("A", 20_000, gold = 2_000, stones = 20, itemGrade = Grade.RARE),
        RaidGrade("B", 12_000, gold = 1_200, stones = 12, itemGrade = null),
        RaidGrade("C", 7_000, gold = 800, stones = 8, itemGrade = null),
        RaidGrade("D", 3_500, gold = 500, stones = 5, itemGrade = null),
        RaidGrade("E", 1_500, gold = 300, stones = 3, itemGrade = null),
        RaidGrade("F", 0, gold = 100, stones = 1, itemGrade = null),
    )

    fun gradeFor(totalDamage: Long): RaidGrade = grades.first { totalDamage >= it.minDamage }

    /**
     * 영웅 1명의 기본 타격 데미지: 공격력×2 + 직업 주 능력치 + 레벨×3.
     * 민첩성이 높을수록 치명타(1.5배) 확률 증가 (최대 50%).
     */
    fun baseHit(stats: Map<Stat, Int>, job: Job, level: Int): Int =
        (stats[Stat.ATTACK] ?: 0) * 2 + (stats[job.primary] ?: 0) + level * 3

    fun critChance(stats: Map<Stat, Int>): Double {
        val agility = (stats[Stat.AGILITY] ?: 0).toDouble()
        return (agility / (agility + 150)).coerceAtMost(0.5)
    }

    /** 파티 전체 정신력 1000 당 데미지 +100% (성직자·마법사의 축복) */
    fun spiritBuff(partySpirit: Int): Double = 1.0 + partySpirit / 1000.0
}

data class RaidStage(val stage: Int, val name: String, val threshold: Long, val damageRate: Double)

data class RaidGrade(val grade: String, val minDamage: Long, val gold: Long, val stones: Int, val itemGrade: Grade?)

/** 모험자 길드 토벌 의뢰 난이도 */
enum class HuntDifficulty(val displayName: String, val goldMultiplier: Double) {
    EASY("쉬움", 1.0),
    NORMAL("적정", 1.0),
    HARD("도전", 1.5),
}

object GuildRules {
    const val HUNT_STAMINA_COST = 10

    /** 하루 5개: 쉬움 2 (레벨-2, -3), 적정 2 (레벨), 도전 1 (레벨+1) */
    fun targetLevels(level: Int): List<Pair<HuntDifficulty, Int>> = listOf(
        HuntDifficulty.EASY to (level - 2).coerceAtLeast(1),
        HuntDifficulty.EASY to (level - 3).coerceAtLeast(1),
        HuntDifficulty.NORMAL to level,
        HuntDifficulty.NORMAL to level,
        HuntDifficulty.HARD to level + 1,
    )

    fun rewardGold(targetLevel: Int, difficulty: HuntDifficulty): Int =
        ((30 + 10 * targetLevel) * difficulty.goldMultiplier).toInt()
}
