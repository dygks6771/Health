package com.example.healthrpg.game

import kotlin.math.roundToInt

/** 러닝 보상, 레벨업, 전투력 규칙. 밸런스 숫자는 game 패키지에서만 바꾸면 됨 */
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

    /** 레벨업마다 자유롭게 찍는 포인트. 이와 별도로 직업 주 능력치가 +1 자동 상승 */
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
    ARMOR("갑옷", Stat.DEFENSE),
    BOOTS("장화", Stat.AGILITY),
    ACCESSORY("목걸이", Stat.SPIRIT),
}

/** 장비 수치와 대장간 강화 규칙 */
object ItemRules {
    const val MAX_ENHANCE = 15

    /** +0→+1 부터 +14→+15 까지 성공 확률(%). 실패해도 단계는 유지되고 재료만 소모 */
    private val SUCCESS_RATES = listOf(100, 100, 100, 90, 85, 80, 70, 60, 50, 40, 35, 30, 25, 20, 15)

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
    ARMOR("방어구", listOf(ItemSlot.ARMOR, ItemSlot.BOOTS), "왕국 공방제"),
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
