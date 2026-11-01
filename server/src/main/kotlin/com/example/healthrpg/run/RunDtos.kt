package com.example.healthrpg.run

import com.example.healthrpg.game.CharacterResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.Instant

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
    @field:Valid
    val runs: List<RunEntry>,
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

data class SyncRunsResponse(
    val reward: RunRewardResponse,
    val character: CharacterResponse,
)
