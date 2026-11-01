package com.example.healthrpg.run

import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.game.CharacterService
import com.example.healthrpg.game.GameRules
import com.example.healthrpg.user.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

/** Health Connect 러닝 기록 → 경험치, 골드, 행동력 */
@Service
class RunService(
    private val userService: UserService,
    private val characterService: CharacterService,
    private val runSessionRepository: RunSessionRepository,
) {
    @Transactional
    fun sync(userId: Long, request: SyncRunsRequest): SyncRunsResponse {
        // 같은 사용자의 동기화가 동시에 와도 보상이 중복되지 않도록 잠금
        val user = userService.getForUpdate(userId)
        val levelBefore = user.level
        var newRuns = 0
        var countedMeters = 0L
        var exp = 0L
        var gold = 0L
        var stamina = 0
        val rejected = mutableListOf<String>()

        for (run in request.runs.sortedBy { it.startTime }.distinctBy { it.externalId }) {
            if (!run.endTime.isAfter(run.startTime)) throw BadRequestException("러닝 종료 시간이 시작 시간보다 빨라요")
            if (runSessionRepository.existsByUserIdAndExternalId(userId, run.externalId)) continue

            val runDate = run.startTime.atZone(characterService.zoneId).toLocalDate()
            val hours = Duration.between(run.startTime, run.endTime).toMillis() / 3_600_000.0
            val speedKmh = run.distanceMeters / 1000.0 / hours

            var reason: String? = null
            var counted = 0
            if (speedKmh > GameRules.MAX_SPEED_KMH) {
                reason = "속도 초과 (%.1f km/h)".format(speedKmh)
            } else {
                val usedToday = runSessionRepository.sumCountedMeters(userId, runDate, runDate)
                val left = (GameRules.DAILY_KM_CAP * 1000L - usedToday).coerceAtLeast(0)
                counted = minOf(run.distanceMeters.toLong(), left).toInt()
                if (counted < run.distanceMeters) reason = "하루 상한 ${GameRules.DAILY_KM_CAP}km 초과"
            }

            val runExp = GameRules.rewardFor(counted, GameRules.EXP_PER_KM)
            val runGold = GameRules.rewardFor(counted, GameRules.GOLD_PER_KM)
            val runStamina = minOf(
                GameRules.rewardFor(counted, GameRules.STAMINA_PER_KM),
                GameRules.MAX_STAMINA - user.stamina,
            ).coerceAtLeast(0)
            user.stamina += runStamina
            user.gold += runGold

            runSessionRepository.save(
                RunSession(
                    user = user,
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

        val (_, levelAfter) = characterService.addExp(user, exp)
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
            character = characterService.character(userId),
        )
    }
}
