package com.example.healthrpg.hero

import com.example.healthrpg.game.Job
import com.example.healthrpg.game.Stat
import com.example.healthrpg.item.ItemResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero

data class CreateHeroRequest(
    @field:NotBlank val name: String,
    val job: Job,
    @field:PositiveOrZero val appearance: Int,
)

data class AllocateStatsRequest(
    @field:PositiveOrZero @field:Max(1000) val attack: Int = 0,
    @field:PositiveOrZero @field:Max(1000) val defense: Int = 0,
    @field:PositiveOrZero @field:Max(1000) val agility: Int = 0,
    @field:PositiveOrZero @field:Max(1000) val spirit: Int = 0,
)

data class StatResponse(
    val stat: Stat,
    val name: String,
    /** 찍은 값 (기본·직업 포함) */
    val base: Int,
    /** 장비 보너스 */
    val bonus: Int,
    val total: Int,
)

/** 로비에 필요한 영웅 정보 한 번에 */
data class HeroResponse(
    val id: Long,
    val name: String,
    val job: Job,
    val jobName: String,
    val jobRole: String,
    val appearance: Int,
    val level: Int,
    val totalExp: Long,
    val expInLevel: Long,
    val expToNextLevel: Long,
    val gold: Long,
    val stamina: Int,
    val maxStamina: Int,
    val statPoints: Int,
    val enhanceStones: Int,
    val power: Int,
    val stats: List<StatResponse>,
    val equipment: List<ItemResponse>,
    /** 오늘 기록된 러닝 거리 (보상 제외된 기록 포함) */
    val todayKm: Double,
    /** 오늘 실제로 보상받은 거리 (속도 초과·하루 상한 제외) */
    val todayRewardKm: Double,
    val weekKm: Double,
    /** 오늘 걸음수와 그 걸음으로 받은 행동력 (하루 상한 [dailyStepStaminaCap]) */
    val todaySteps: Long,
    val todayStepStamina: Int,
    val dailyStepStaminaCap: Int,
)

/** 다른 영웅(친구) 요약 */
data class HeroSummary(
    val id: Long,
    val name: String,
    val job: Job,
    val jobName: String,
    val appearance: Int,
    val level: Int,
    val power: Int,
)

data class JobInfo(
    val job: Job,
    val name: String,
    val role: String,
    val primary: Stat,
    val primaryName: String,
    val secondary: Stat,
    val secondaryName: String,
    val description: String,
)

data class AppearanceInfo(val index: Int, val name: String)

data class GameMetaResponse(val jobs: List<JobInfo>, val appearances: List<AppearanceInfo>)

data class NameCheckResponse(val name: String, val available: Boolean, val reason: String?)
