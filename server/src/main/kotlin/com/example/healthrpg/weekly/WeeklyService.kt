package com.example.healthrpg.weekly

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.game.ChestResponse
import com.example.healthrpg.game.ExpeditionRules
import com.example.healthrpg.game.Region
import com.example.healthrpg.game.WeeklyRules
import com.example.healthrpg.hero.Hero
import com.example.healthrpg.hero.HeroResponse
import com.example.healthrpg.hero.HeroService
import com.example.healthrpg.item.ItemFactory
import com.example.healthrpg.item.ItemResponse
import com.example.healthrpg.run.RunSessionRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Entity
@Table(name = "weekly_goals")
class WeeklyGoal(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_id", nullable = false)
    val hero: Hero,

    @Column(name = "week_start", nullable = false)
    val weekStart: LocalDate,

    @Column(name = "goal_meters", nullable = false)
    val goalMeters: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(nullable = false)
    var claimed: Boolean = false

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
}

interface WeeklyGoalRepository : JpaRepository<WeeklyGoal, Long> {
    fun findByHeroIdAndWeekStart(heroId: Long, weekStart: LocalDate): WeeklyGoal?

    /** 최근 주부터 (연속 달성 계산용) */
    fun findByHeroIdAndWeekStartLessThanOrderByWeekStartDesc(heroId: Long, weekStart: LocalDate): List<WeeklyGoal>
}

data class DayKm(val date: LocalDate, val km: Double)

data class RunRecords(
    /** 누적 러닝 거리 (보상에 반영된 거리) */
    val totalKm: Double,
    val longestRunKm: Double,
    /** 1km 이상 러닝 중 가장 빠른 평균 페이스 (초/km), 기록 없으면 null */
    val bestPaceSecPerKm: Int?,
)

data class ExpeditionResponse(
    val totalKm: Double,
    /** 누적 거리로 열린 지역 수 (1~10) */
    val unlockedTier: Int,
    /** 다음에 열릴 지역. 모두 열렸으면 null */
    val nextRegionName: String?,
    val nextRegionKm: Int?,
    /** 이전 지역 기준 → 다음 지역까지 진행률 0~1 */
    val progressToNext: Double,
)

data class WeeklyResponse(
    val weekStart: LocalDate,
    val goalKm: Double,
    val progressKm: Double,
    val achieved: Boolean,
    val claimed: Boolean,
    /** 지난주까지 연속 달성한 주 수 */
    val streakBefore: Int,
    /** 이번 주 달성 시 연속 주 수 (보상 기준) */
    val streakIfAchieved: Int,
    val daysLeft: Int,
    val runDays: Int,
    /** 월~일 하루별 거리 */
    val days: List<DayKm>,
    val rewardChest: ChestResponse,
    val rewardGold: Long,
    val rewardStones: Int,
    val records: RunRecords,
    val expedition: ExpeditionResponse,
)

data class WeeklyClaimResponse(
    val chest: ChestResponse,
    val gold: Long,
    val stones: Int,
    val streak: Int,
    val item: ItemResponse?,
    val hero: HeroResponse,
)

/** 주간 원정 목표(개인 맞춤 거리) · 연속 달성 · 개인 기록 · 원정 지도 진행 */
@Service
class WeeklyService(
    private val heroService: HeroService,
    private val weeklyGoalRepository: WeeklyGoalRepository,
    private val runSessionRepository: RunSessionRepository,
    private val itemFactory: ItemFactory,
) {
    // 다른 서비스에서 프록시를 통해 부르는 메서드에는 기본값 인자를 쓰지 않는다 (Kotlin 기본값 처리 함수는 프록시를 우회함)
    fun weekStart(): LocalDate = heroService.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** 이번 주 목표가 없으면 지난 4주 기록으로 정해 저장 */
    @Transactional
    fun ensureGoal(hero: Hero): WeeklyGoal {
        val start = weekStart()
        weeklyGoalRepository.findByHeroIdAndWeekStart(hero.id!!, start)?.let { return it }
        val past = (1..WeeklyRules.LOOKBACK_WEEKS).map { weeksAgo ->
            val from = start.minusWeeks(weeksAgo.toLong())
            weekMeters(hero.id!!, from) / 1000.0
        }
        val goalKm = WeeklyRules.goalKm(past)
        return weeklyGoalRepository.save(WeeklyGoal(hero, start, (goalKm * 1000).toInt()))
    }

    @Transactional
    fun weekly(accountId: Long): WeeklyResponse {
        // 동기화와 동시에 불려도 이번 주 목표가 한 번만 만들어지도록 영웅 행을 잠근다
        val hero = heroService.requireForUpdate(accountId)
        val goal = ensureGoal(hero)
        val heroId = hero.id!!
        val start = goal.weekStart
        val today = heroService.today()
        val progress = weekMeters(heroId, start)
        val achieved = progress >= goal.goalMeters
        val streakBefore = streakBefore(heroId, start)
        val streakIfAchieved = streakBefore + 1
        val byDay = runSessionRepository.dailyCountedMeters(heroId, start, start.plusDays(6))
            .associate { (it[0] as LocalDate) to (it[1] as Number).toLong() }

        return WeeklyResponse(
            weekStart = start,
            goalKm = goal.goalMeters / 1000.0,
            progressKm = progress / 1000.0,
            achieved = achieved,
            claimed = goal.claimed,
            streakBefore = streakBefore,
            streakIfAchieved = streakIfAchieved,
            daysLeft = Duration.between(today.atStartOfDay(), start.plusDays(7).atStartOfDay()).toDays().toInt(),
            runDays = byDay.count { it.value > 0 },
            days = (0..6L).map { start.plusDays(it) }.map { DayKm(it, (byDay[it] ?: 0L) / 1000.0) },
            rewardChest = ChestResponse.of(WeeklyRules.chest(streakIfAchieved)),
            rewardGold = WeeklyRules.gold(hero.level, streakIfAchieved),
            rewardStones = WeeklyRules.stones(streakIfAchieved),
            records = records(heroId),
            expedition = expedition(heroId),
        )
    }

    @Transactional
    fun claim(accountId: Long): WeeklyClaimResponse {
        val hero = heroService.requireForUpdate(accountId)
        val goal = ensureGoal(hero)
        if (goal.claimed) throw BadRequestException("이번 주 원정 보상은 이미 받았습니다. 다음 주 월요일에 새 목표가 열려요")
        val progress = weekMeters(hero.id!!, goal.weekStart)
        if (progress < goal.goalMeters) {
            throw BadRequestException("목표까지 %.1fkm 남았어요".format((goal.goalMeters - progress) / 1000.0))
        }
        val streak = streakBefore(hero.id!!, goal.weekStart) + 1
        val chest = WeeklyRules.chest(streak)
        val gold = WeeklyRules.gold(hero.level, streak)
        val stones = WeeklyRules.stones(streak)
        hero.gold += gold
        hero.enhanceStones += stones
        // 황금 상자(3주 연속~)는 내 레벨 지역 장비
        val item = if (chest.itemChance >= 100) itemFactory.dungeonDrop(hero, Region.forLevel(hero.level)) else null
        goal.claimed = true
        return WeeklyClaimResponse(ChestResponse.of(chest), gold, stones, streak, item?.let(ItemResponse::from), heroService.response(hero))
    }

    /** 누적 거리로 원정 지도에서 열린 지역 */
    @Transactional(readOnly = true)
    fun expedition(heroId: Long): ExpeditionResponse {
        val totalKm = runSessionRepository.sumAllCountedMeters(heroId) / 1000.0
        val unlocked = Region.entries.count { totalKm >= it.requiredTotalKm }.coerceAtLeast(1)
        val next = Region.byTier(unlocked + 1)
        val prevKm = ExpeditionRules.requiredKm(unlocked)
        return ExpeditionResponse(
            totalKm = totalKm,
            unlockedTier = unlocked,
            nextRegionName = next?.displayName,
            nextRegionKm = next?.requiredTotalKm,
            progressToNext = next?.let { ((totalKm - prevKm) / (it.requiredTotalKm - prevKm)).coerceIn(0.0, 1.0) } ?: 1.0,
        )
    }

    /** 이번 주(월~일) 보상에 반영된 러닝 거리 (m) — 레이드 러닝 보너스에서 사용 */
    fun thisWeekMeters(heroId: Long): Long = weekMeters(heroId, weekStart())

    fun weekMeters(heroId: Long, weekStart: LocalDate): Long =
        runSessionRepository.sumCountedMeters(heroId, weekStart, weekStart.plusDays(6))

    /** [weekStart] 직전 주부터 거슬러 올라가며 목표를 달성한 연속 주 수 (목표가 없던 주에서 끊김) */
    private fun streakBefore(heroId: Long, weekStart: LocalDate): Int {
        var expected = weekStart.minusWeeks(1)
        var streak = 0
        for (goal in weeklyGoalRepository.findByHeroIdAndWeekStartLessThanOrderByWeekStartDesc(heroId, weekStart)) {
            if (goal.weekStart != expected) break
            if (weekMeters(heroId, goal.weekStart) < goal.goalMeters) break
            streak++
            expected = expected.minusWeeks(1)
        }
        return streak
    }

    private fun records(heroId: Long): RunRecords {
        val runs = runSessionRepository.findByHeroIdAndCountedMetersGreaterThanEqual(heroId, 1000)
        val bestPace = runs.minOfOrNull { Duration.between(it.startTime, it.endTime).seconds * 1000.0 / it.countedMeters }
        return RunRecords(
            totalKm = runSessionRepository.sumAllCountedMeters(heroId) / 1000.0,
            longestRunKm = (runs.maxOfOrNull { it.countedMeters } ?: 0) / 1000.0,
            bestPaceSecPerKm = bestPace?.toInt(),
        )
    }
}

@RestController
class WeeklyController(private val weeklyService: WeeklyService) {
    @GetMapping("/api/me/weekly")
    fun weekly(@LoginAccount accountId: Long): WeeklyResponse = weeklyService.weekly(accountId)

    @PostMapping("/api/me/weekly/claim")
    fun claim(@LoginAccount accountId: Long): WeeklyClaimResponse = weeklyService.claim(accountId)
}
