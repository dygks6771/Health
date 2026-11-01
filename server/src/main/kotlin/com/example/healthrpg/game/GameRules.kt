package com.example.healthrpg.game

/** 러닝 → 보상, 레벨업 규칙. 밸런스 숫자는 여기서만 바꾸면 됨 */
object GameRules {
    /** 러닝 1km 당 보상 */
    const val EXP_PER_KM = 50
    const val GOLD_PER_KM = 20
    const val STAMINA_PER_KM = 10

    /** 하루에 보상받을 수 있는 최대 러닝 거리 (조작·과도한 보상 방지) */
    const val DAILY_KM_CAP = 20

    /** 이보다 빠른 평균 속도의 기록은 러닝이 아닌 것으로 보고 보상하지 않음 (차량 이동 등) */
    const val MAX_SPEED_KMH = 25.0

    /** 컨텐츠 입장에 쓰는 행동력 최대치 */
    const val MAX_STAMINA = 300

    const val STAT_POINTS_PER_LEVEL = 3
    const val BASE_STAT = 5

    fun rewardFor(meters: Int, perKm: Int): Int = (meters.toLong() * perKm / 1000).toInt()

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

enum class Stat(val displayName: String) {
    ATTACK("공격력"),
    DEFENSE("방어력"),
    AGILITY("민첩성"),
    SPIRIT("정신력"),
}

/** 장비 등급. 무기는 강화 수치로, 장신구는 획득한 레이드 난이도로 정해짐 */
enum class Grade(val displayName: String) {
    COMMON("일반"),
    UNCOMMON("고급"),
    RARE("희귀"),
    EPIC("영웅"),
    LEGENDARY("전설"),
    ;

    companion object {
        /** 난이도 1~4 일반, 5~8 고급, 9~12 희귀, 13~16 영웅, 17~20 전설 */
        fun forDifficulty(difficulty: Int): Grade = entries[((difficulty - 1) / 4).coerceIn(0, entries.size - 1)]
    }
}

object WeaponRules {
    const val MAX_ENHANCE = 20

    /** +0~3 일반, +4~7 고급, +8~11 희귀, +12~15 영웅, +16 이상 전설 */
    fun grade(enhance: Int): Grade = Grade.entries[(enhance / 4).coerceIn(0, Grade.entries.size - 1)]

    fun attackBonus(enhance: Int): Int = enhance * 2

    /** 현재 +[enhance] 에서 한 단계 올리는 비용 (최대 강화면 null) */
    fun nextCost(enhance: Int): EnhanceCost? =
        if (enhance >= MAX_ENHANCE) null else EnhanceCost(stones = enhance + 1, gold = 100L * (enhance + 1))
}

data class EnhanceCost(val stones: Int, val gold: Long)

/** 레이드 보상 장신구. 종류마다 올려주는 스탯이 다르고, 종류별로 하나씩 장착 */
enum class AccessoryType(val displayName: String, val stat: Stat) {
    RING("반지", Stat.ATTACK),
    BRACELET("팔찌", Stat.DEFENSE),
    EARRING("귀걸이", Stat.AGILITY),
    NECKLACE("목걸이", Stat.SPIRIT),
    ;

    companion object {
        /** 일반 3, 고급 6, 희귀 9, 영웅 12, 전설 15 */
        fun bonusFor(grade: Grade): Int = 3 * (grade.ordinal + 1)
    }
}
