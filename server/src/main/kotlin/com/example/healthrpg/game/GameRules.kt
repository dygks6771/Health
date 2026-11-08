package com.example.healthrpg.game

import kotlin.math.roundToInt

/** 러닝·걸음 보상, 레벨업, 전투력 규칙. 밸런스 숫자는 game 패키지에서만 바꾸면 됨 */
object GameRules {
    /** 러닝 1km 당 보상 (하루 상한 없음) */
    const val EXP_PER_KM = 50
    const val GOLD_PER_KM = 20

    /**
     * 부상 예방: 하루 이 거리까지는 보상 100%, 넘는 거리는 [OVER_DAILY_REWARD_RATE] 만큼만.
     * 상한이 아니라 무리해서 몰아 뛰기보다 꾸준히 뛰도록 유도하는 장치.
     */
    const val FULL_REWARD_KM_PER_DAY = 10
    const val OVER_DAILY_REWARD_RATE = 0.5

    /**
     * 오늘 이미 [alreadyToday] m 를 뛴 상태에서 [meters] m 를 더 뛰었을 때 보상 계산에 쓰는 거리.
     * 하루 [FULL_REWARD_KM_PER_DAY] km 를 넘는 부분은 [OVER_DAILY_REWARD_RATE] 배.
     */
    fun rewardMeters(meters: Int, alreadyToday: Long): Int {
        val fullLeft = (FULL_REWARD_KM_PER_DAY * 1000L - alreadyToday).coerceAtLeast(0)
        val full = minOf(meters.toLong(), fullLeft)
        val over = meters - full
        return (full + over * OVER_DAILY_REWARD_RATE).toInt()
    }

    /** 이보다 빠른 평균 속도의 기록은 러닝이 아닌 것으로 보고 보상하지 않음 (차량·자전거 이동 등) */
    const val MAX_SPEED_KMH = 25.0

    /** 걸음수 100보 = 행동력 1 */
    const val STEPS_PER_STAMINA = 100

    /** 하루에 걸음으로 얻을 수 있는 행동력 상한 (15,000보) */
    const val DAILY_STEP_STAMINA_CAP = 150

    /** 컨텐츠 입장에 쓰는 행동력 최대 보유량 */
    const val MAX_STAMINA = 300

    fun staminaForSteps(steps: Long): Int = (steps / STEPS_PER_STAMINA).coerceIn(0, DAILY_STEP_STAMINA_CAP.toLong()).toInt()

    /** 레벨업마다 자유롭게 찍는 포인트. 이와 별도로 직업 주 능력치가 +1 자동 상승 */
    const val STAT_POINTS_PER_LEVEL = 3
    const val BASE_STAT = 5

    fun rewardFor(meters: Int, perKm: Int): Int = (meters.toLong() * perKm / 1000).toInt()

    /** [level] 에서 다음 레벨로 가는 데 필요한 경험치: Lv1→2 는 100(2km), 이후 레벨마다 30(0.6km)씩 증가 */
    fun expToNextLevel(level: Int): Long = 100L + 30L * (level - 1)

    fun progress(totalExp: Long): LevelProgress {
        var level = 1
        var remaining = totalExp
        while (remaining >= expToNextLevel(level)) {
            remaining -= expToNextLevel(level)
            level++
        }
        return LevelProgress(level = level, expInLevel = remaining, expToNextLevel = expToNextLevel(level))
    }

    /** 전투력: 능력치 합 × 3 + 레벨 × 10 */
    fun power(stats: Map<Stat, Int>, level: Int): Int = stats.values.sum() * 3 + level * 10

    /** 장비 없이 레벨만큼 성장한 영웅의 전투력. 토벌 의뢰의 권장 전투력 기준 */
    fun recommendedPower(level: Int): Int = 70 + 22 * (level - 1).coerceAtLeast(0)
}

data class LevelProgress(val level: Int, val expInLevel: Long, val expToNextLevel: Long)

enum class Stat(val displayName: String) {
    ATTACK("공격력"),
    DEFENSE("방어력"),
    AGILITY("민첩성"),
    SPIRIT("정신력"),
}

/** 직업. 생성 시 주 능력치 +3, 보조 능력치 +2, 레벨업마다 주 능력치 +1 */
enum class Job(
    val displayName: String,
    val role: String,
    val primary: Stat,
    val secondary: Stat,
    val weaponName: String,
    val description: String,
) {
    KNIGHT("기사", "방어형", Stat.DEFENSE, Stat.ATTACK, "기사검", "단단한 방패로 동료를 지키는 수호자"),
    WARRIOR("전사", "공격형", Stat.ATTACK, Stat.DEFENSE, "대검", "거대한 검으로 적을 베어 넘기는 돌격대장"),
    ARCHER("궁수", "공격형", Stat.AGILITY, Stat.ATTACK, "장궁", "빠른 몸놀림과 치명타로 적을 꿰뚫는 사냥꾼"),
    MAGE("마법사", "마법형", Stat.SPIRIT, Stat.ATTACK, "지팡이", "원소의 힘으로 강력한 마법을 쏟아내는 현자"),
    PRIEST("성직자", "지원형", Stat.SPIRIT, Stat.DEFENSE, "성구", "신성한 축복으로 파티 전체를 강화하는 사제"),
}

/** 영웅 외형 (앱에서 초상화로 표시) */
object Appearances {
    val names = listOf("견습 기사", "여검사", "떠돌이 용병", "엘프 레인저", "은빛 마녀", "성녀")
}

/** 장비 등급 */
enum class Grade(val displayName: String, val multiplier: Double) {
    COMMON("일반", 1.0),
    UNCOMMON("고급", 1.25),
    RARE("희귀", 1.5),
    EPIC("영웅", 1.8),
    LEGENDARY("전설", 2.2),
}

/** 장비 부위. 부위마다 주 능력치가 정해짐 */
enum class ItemSlot(val displayName: String, val stat: Stat) {
    WEAPON("무기", Stat.ATTACK),
    HELMET("투구", Stat.DEFENSE),
    ARMOR("갑옷", Stat.DEFENSE),
    BOOTS("장화", Stat.AGILITY),
    ACCESSORY("목걸이", Stat.SPIRIT),
}

/** 장비 수치와 대장간 강화 규칙 */
object ItemRules {
    const val MAX_ENHANCE = 25

    /** +0→+1 부터 +24→+25 까지 성공 확률(%). 높을수록 실패 확률 증가 */
    private val SUCCESS_RATES = listOf(
        100, 100, 100, 95, 90, // +0 ~ +4
        85, 80, 75, 70, 65, // +5 ~ +9
        60, 55, 50, 45, 40, // +10 ~ +14
        35, 30, 25, 20, 16, // +15 ~ +19
        12, 9, 7, 5, 3, // +20 ~ +24
    )

    /** 강화 실패 시 떨어지는 단계: +0~4 는 유지, +5 이상은 1단계만 하락 */
    fun failDrop(enhance: Int): Int = if (enhance >= 5) 1 else 0

    /** 티어(던전 1~10) × 등급으로 주 능력치 결정: 티어1 일반 6 ~ 티어10 전설 73 */
    fun mainValue(tier: Int, grade: Grade): Int = ((3 + 3 * tier) * grade.multiplier).roundToInt()

    /** 희귀 이상은 다른 능력치 하나가 주 능력치의 1/3 만큼 추가 */
    fun subValue(mainValue: Int, grade: Grade): Int = if (grade >= Grade.RARE) mainValue / 3 else 0

    /** 강화 1단계당 +10% */
    fun enhanced(value: Int, enhance: Int): Int = value + value * enhance / 10

    fun successRate(enhance: Int): Int? = SUCCESS_RATES.getOrNull(enhance)

    fun enhanceCost(tier: Int, enhance: Int): EnhanceCost? =
        if (enhance >= MAX_ENHANCE) null
        else EnhanceCost(stones = 1 + enhance + (tier - 1) / 3, gold = 30L * tier * (enhance + 1))

    fun sellPrice(tier: Int, grade: Grade, enhance: Int): Long = 20L * tier * (grade.ordinal + 1) + 10L * tier * enhance
}

data class EnhanceCost(val stones: Int, val gold: Long)

/** 상점 장비 뽑기 */
object ShopRules {
    /** 1회 가격: 내 레벨 지역 티어 × 150 골드 (초보자 마을 150 ~ 마왕성 1,500) */
    fun price(tier: Int): Long = 150L * tier

    /** 등급 확률 (천분율): 일반 60% · 고급 28% · 희귀 9% · 영웅 2.5% · 전설 0.5% */
    val gradeWeights = listOf(600, 280, 90, 25, 5)
}

/** 상점 뽑기 종류 */
enum class ShopCategory(val displayName: String, val slots: List<ItemSlot>, val prefix: String) {
    WEAPON("무기", listOf(ItemSlot.WEAPON), "왕국 공방제"),
    ARMOR("방어구", listOf(ItemSlot.HELMET, ItemSlot.ARMOR, ItemSlot.BOOTS), "왕국 공방제"),
}

object RankingRules {
    /**
     * 랭킹 점수 = 레벨 × 100 + 전투력.
     * 전투력이 레벨보다 훨씬 큰 수라 그대로 더하면 레벨이 거의 반영되지 않아 레벨에 가중치를 둠.
     */
    const val LEVEL_WEIGHT = 100

    fun score(level: Int, power: Int): Long = level.toLong() * LEVEL_WEIGHT + power
}

object AuctionRules {
    /** 판매 대금에서 떼는 수수료 % */
    const val FEE_PERCENT = 5
    const val MIN_PRICE = 1L
    const val MAX_PRICE = 10_000_000L

    /** 한 사람이 동시에 올릴 수 있는 최대 개수 */
    const val MAX_LISTINGS = 10

    fun sellerReceives(price: Long): Long = price - price * FEE_PERCENT / 100
}

/** 토벌·던전 승률. 결과는 서버에서 확률로 정해지고 질 수도 있다 */
object BattleRules {
    /** 권장 전투력의 이 비율 미만이면 도전 불가 (너무 약함) */
    const val HUNT_MIN_RATIO = 0.5

    /**
     * 토벌 승률 (내 전투력 / 권장 전투력):
     * 0.5배 10% → 1배 70% → 1.5배 이상 100%
     */
    fun huntWinChance(power: Int, required: Int): Double {
        val r = power.toDouble() / required.coerceAtLeast(1)
        return when {
            r >= 1.5 -> 1.0
            r >= 1.0 -> 0.7 + 0.6 * (r - 1.0)
            else -> (0.1 + 1.2 * (r - 0.5)).coerceAtLeast(0.0)
        }
    }

    /**
     * 던전 승률 (내 능력치 합 / 최소 능력치 합). 최소치를 넘어야 입장 가능:
     * 1배 60% → 1.5배 이상 100%
     */
    fun dungeonWinChance(totalStats: Int, minTotalStats: Int): Double {
        val r = totalStats.toDouble() / minTotalStats.coerceAtLeast(1)
        return if (r >= 1.5) 1.0 else (0.6 + 0.8 * (r - 1.0)).coerceIn(0.0, 1.0)
    }

    fun percent(chance: Double): Int = (chance * 100).toInt()
}

/** 승리 보상 상자. 등급에 따라 보상 배율과 추가 보상이 달라짐 */
enum class ChestTier(val displayName: String, val multiplier: Double, val weight: Int, val bonusStones: Int, val itemChance: Int) {
    WOOD("나무 상자", 1.0, 70, 0, 50),
    SILVER("은 상자", 1.5, 25, 1, 75),
    GOLD("황금 상자", 2.5, 5, 3, 100),
    ;

    companion object {
        fun roll(random: kotlin.random.Random): ChestTier {
            var roll = random.nextInt(entries.sumOf { it.weight })
            for (tier in entries) {
                if (roll < tier.weight) return tier
                roll -= tier.weight
            }
            return WOOD
        }
    }
}

/**
 * 주간 원정 목표: 지난 4주 동안 뛴 주의 평균보다 조금(10%) 높게, 사람마다 다른 목표.
 * 달성하면 상자, 연속 달성 주가 길수록 보상이 커진다.
 */
object WeeklyRules {
    const val MIN_GOAL_KM = 5.0
    const val MAX_GOAL_KM = 50.0
    const val GROWTH = 1.1
    const val LOOKBACK_WEEKS = 4

    /** 연속 달성 보너스는 이 주수까지 커짐 */
    const val MAX_STREAK_BONUS = 5

    /** [pastWeeksKm]: 지난 주들의 러닝 거리. 뛴 주(0 초과)의 평균 × 1.1 을 0.5km 단위로 반올림 */
    fun goalKm(pastWeeksKm: List<Double>): Double {
        val active = pastWeeksKm.filter { it > 0.0 }
        if (active.isEmpty()) return MIN_GOAL_KM
        val raw = active.average() * GROWTH
        return (Math.round(raw * 2) / 2.0).coerceIn(MIN_GOAL_KM, MAX_GOAL_KM)
    }

    /** 이번 주를 달성하면 몇 주 연속이 되는지 기준으로 상자 결정: 3주 연속부터 황금 상자 */
    fun chest(streak: Int): ChestTier = if (streak >= 3) ChestTier.GOLD else ChestTier.SILVER

    fun gold(level: Int, streak: Int): Long =
        ((150L + 20L * level) * (1 + 0.1 * streak.coerceAtMost(MAX_STREAK_BONUS))).toLong()

    fun stones(streak: Int): Int = 3 + streak.coerceAtMost(MAX_STREAK_BONUS)
}

/**
 * 원정 지도: 누적 러닝 거리로 다음 지역이 열린다 (레벨·능력치 조건과 별개).
 * 지역 1 은 처음부터, 지역 10 은 누적 225km.
 */
object ExpeditionRules {
    private val REQUIRED_KM = listOf(0, 5, 15, 30, 50, 75, 105, 140, 180, 225)

    fun requiredKm(tier: Int): Int = REQUIRED_KM[(tier - 1).coerceIn(0, REQUIRED_KM.size - 1)]
}

/** 하루 한 번 받는 출석 보상 */
object DailyRewardRules {
    fun gold(tier: Int, chest: ChestTier): Long = (100L * tier * chest.multiplier).toLong()

    fun stones(chest: ChestTier): Int = (2 * chest.multiplier).toInt()
}
