package com.example.healthrpg.run

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.game.GameRules
import com.example.healthrpg.hero.HeroResponse
import com.example.healthrpg.hero.HeroService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

data class RunEntry(
    /** Health Connect 기록 ID. 같은 러닝을 두 번 보상하지 않기 위해 사용 */
    @field:NotBlank
    @field:Size(max = 200)
    val externalId: String,
    val startTime: Instant,
    val endTime: Instant,
    @field:PositiveOrZero
    val distanceMeters: Int,
)

data class SyncRunsRequest(
    @field:Size(max = 100)
    val runs: List<@Valid RunEntry>,
)

data class RunRewardResponse(
    /** 이번에 새로 반영된 러닝 수 (이미 받은 기록은 제외) */
    val newRuns: Int,
    val countedKm: Double,
    val expGained: Long,
    val goldGained: Long,
    val staminaGained: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    val statPointsGained: Int,
    /** 보상하지 않은 기록과 이유 */
    val rejected: List<String>,
) {
    val leveledUp: Boolean get() = levelAfter > levelBefore
}

data class SyncRunsResponse(val reward: RunRewardResponse, val hero: HeroResponse)

data class RunHistoryEntry(
    val runDate: LocalDate,
    val startTime: Instant,
    val endTime: Instant,
    val distanceKm: Double,
    /** 보상에 반영된 거리 */
    val countedKm: Double,
    val expAwarded: Int,
    val goldAwarded: Int,
    val staminaAwarded: Int,
    val rejectedReason: String?,
)

data class RunHistoryResponse(val runs: List<RunHistoryEntry>)

/** Health Connect 러닝 기록 → 경험치, 골드, 행동력 */
@Service
class RunService(
    private val heroService: HeroService,
    private val runSessionRepository: RunSessionRepository,
) {
    @Transactional
    fun sync(accountId: Long, request: SyncRunsRequest): SyncRunsResponse {
        val hero = heroService.requireForUpdate(accountId)
        val heroId = hero.id!!
        val levelBefore = hero.level
        var newRuns = 0
        var countedMeters = 0L
        var exp = 0L
        var gold = 0L
        var stamina = 0
        val rejected = mutableListOf<String>()

        for (run in request.runs.sortedBy { it.startTime }.distinctBy { it.externalId }) {
            if (!run.endTime.isAfter(run.startTime)) throw BadRequestException("러닝 종료 시간이 시작 시간보다 빨라요")
            if (runSessionRepository.existsByHeroIdAndExternalId(heroId, run.externalId)) continue

            val runDate = run.startTime.atZone(heroService.zoneId).toLocalDate()
            val hours = Duration.between(run.startTime, run.endTime).toMillis() / 3_600_000.0
            val speedKmh = run.distanceMeters / 1000.0 / hours

            var reason: String? = null
            var counted = 0
            if (speedKmh > GameRules.MAX_SPEED_KMH) {
                reason = "속도 초과 (%.1f km/h)".format(speedKmh)
            } else {
                val usedToday = runSessionRepository.sumCountedMeters(heroId, runDate, runDate)
                val left = (GameRules.DAILY_KM_CAP * 1000L - usedToday).coerceAtLeast(0)
                counted = minOf(run.distanceMeters.toLong(), left).toInt()
                if (counted < run.distanceMeters) reason = "하루 상한 ${GameRules.DAILY_KM_CAP}km 초과"
            }

            val runExp = GameRules.rewardFor(counted, GameRules.EXP_PER_KM)
            val runGold = GameRules.rewardFor(counted, GameRules.GOLD_PER_KM)
            val runStamina = minOf(
                GameRules.rewardFor(counted, GameRules.STAMINA_PER_KM),
                GameRules.MAX_STAMINA - hero.stamina,
            ).coerceAtLeast(0)
            hero.stamina += runStamina
            hero.gold += runGold

            runSessionRepository.save(
                RunSession(
                    hero = hero,
                    externalId = run.externalId,
                    runDate = runDate,
                    startTime = run.startTime,
                    endTime = run.endTime,
                    distanceMeters = run.distanceMeters,
                    countedMeters = counted,
                    expAwarded = runExp,
                    goldAwarded = runGold,
                    staminaAwarded = runStamina,
                    rejectedReason = reason,
                ),
            )

            newRuns++
            countedMeters += counted
            exp += runExp
            gold += runGold
            stamina += runStamina
            if (reason != null) rejected += "$runDate %.2fkm: $reason".format(run.distanceMeters / 1000.0)
        }

        val (_, levelAfter) = heroService.addExp(hero, exp)
        return SyncRunsResponse(
            reward = RunRewardResponse(
                newRuns = newRuns,
                countedKm = countedMeters / 1000.0,
                expGained = exp,
                goldGained = gold,
                staminaGained = stamina,
                levelBefore = levelBefore,
                levelAfter = levelAfter,
                statPointsGained = GameRules.STAT_POINTS_PER_LEVEL * (levelAfter - levelBefore),
                rejected = rejected,
            ),
            hero = heroService.response(hero),
        )
    }
}

@RestController
class RunController(
    private val runService: RunService,
    private val heroService: HeroService,
    private val runSessionRepository: RunSessionRepository,
) {
    /** 최근 러닝 기록 20건과 각 기록의 보상 (보상이 빠졌다면 이유 포함) */
    @GetMapping("/api/me/runs")
    @Transactional(readOnly = true)
    fun history(@LoginAccount accountId: Long): RunHistoryResponse {
        val hero = heroService.require(accountId)
        return RunHistoryResponse(
            runSessionRepository.findTop20ByHeroIdOrderByStartTimeDesc(hero.id!!).map {
                RunHistoryEntry(
                    runDate = it.runDate,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    distanceKm = it.distanceMeters / 1000.0,
                    countedKm = it.countedMeters / 1000.0,
                    expAwarded = it.expAwarded,
                    goldAwarded = it.goldAwarded,
                    staminaAwarded = it.staminaAwarded,
                    rejectedReason = it.rejectedReason,
                )
            },
        )
    }

    /** 러닝 기록 동기화 → 보상 + 영웅 상태 */
    @PostMapping("/api/me/runs")
    fun sync(@LoginAccount accountId: Long, @Valid @RequestBody request: SyncRunsRequest): SyncRunsResponse =
        runService.sync(accountId, request)
}
