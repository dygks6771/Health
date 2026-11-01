package com.example.healthrpg.step

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.game.GameRules
import com.example.healthrpg.hero.Hero
import com.example.healthrpg.hero.HeroResponse
import com.example.healthrpg.hero.HeroService
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate

/** 하루 걸음수와 그날 지급한 행동력 */
@Entity
@Table(name = "step_days")
class StepDay(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_id", nullable = false)
    val hero: Hero,

    @Column(name = "step_date", nullable = false)
    val stepDate: LocalDate,

    @Column(nullable = false)
    var steps: Long,

    @Column(name = "stamina_awarded", nullable = false)
    var staminaAwarded: Int = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "synced_at", nullable = false)
    var syncedAt: Instant = Instant.now()
}

interface StepDayRepository : JpaRepository<StepDay, Long> {
    fun findByHeroIdAndStepDate(heroId: Long, stepDate: LocalDate): StepDay?
}

data class StepEntry(
    val date: LocalDate,
    @field:PositiveOrZero @field:Max(300_000) val steps: Long,
)

data class SyncStepsRequest(@field:Size(max = 14) val days: List<@Valid StepEntry>)

data class StepRewardResponse(
    val staminaGained: Int,
    /** 최대 보유량에 막혀 받지 못한 행동력 */
    val staminaOverflow: Int,
    val todaySteps: Long,
)

data class SyncStepsResponse(val reward: StepRewardResponse, val hero: HeroResponse)

/**
 * 걸음수 → 행동력. 100보 = 1, 하루 최대 150 (15,000보), 보유 최대 300.
 * 같은 날을 여러 번 동기화해도 늘어난 걸음만큼만 지급.
 */
@Service
class StepService(
    private val heroService: HeroService,
    private val stepDayRepository: StepDayRepository,
) {
    @Transactional
    fun sync(accountId: Long, request: SyncStepsRequest): SyncStepsResponse {
        val hero = heroService.requireForUpdate(accountId)
        val today = heroService.today()
        var gained = 0
        var overflow = 0

        for (entry in request.days.sortedBy { it.date }.associateBy { it.date }.values) {
            if (entry.date.isAfter(today) || entry.date.isBefore(today.minusDays(13))) {
                throw BadRequestException("최근 2주 걸음수만 동기화할 수 있습니다")
            }
            val day = stepDayRepository.findByHeroIdAndStepDate(hero.id!!, entry.date)
                ?: stepDayRepository.save(StepDay(hero, entry.date, 0))
            // 걸음수는 하루 누적값이라 줄어드는 일은 없지만, 기기 문제로 적게 오면 큰 값을 유지
            day.steps = maxOf(day.steps, entry.steps)
            day.syncedAt = Instant.now()

            val earned = GameRules.staminaForSteps(day.steps)
            val delta = earned - day.staminaAwarded
            if (delta > 0) {
                val room = (GameRules.MAX_STAMINA - hero.stamina).coerceAtLeast(0)
                val added = minOf(delta, room)
                hero.stamina += added
                gained += added
                overflow += delta - added
                day.staminaAwarded = earned
            }
        }

        val todaySteps = stepDayRepository.findByHeroIdAndStepDate(hero.id!!, today)?.steps ?: 0
        return SyncStepsResponse(StepRewardResponse(gained, overflow, todaySteps), heroService.response(hero))
    }
}

@RestController
class StepController(private val stepService: StepService) {
    /** 최근 날짜별 걸음수 → 행동력 */
    @PostMapping("/api/me/steps")
    fun sync(@LoginAccount accountId: Long, @Valid @RequestBody request: SyncStepsRequest): SyncStepsResponse =
        stepService.sync(accountId, request)
}
