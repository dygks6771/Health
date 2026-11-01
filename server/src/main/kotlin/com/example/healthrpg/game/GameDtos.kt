package com.example.healthrpg.game

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.PositiveOrZero

data class StatResponse(
    val stat: Stat,
    val name: String,
    /** 찍은 값 (기본 5 포함) */
    val base: Int,
    /** 무기·장신구 보너스 */
    val bonus: Int,
    val total: Int,
)

data class WeaponResponse(
    val enhance: Int,
    val grade: Grade,
    val gradeName: String,
    val attackBonus: Int,
    val maxEnhance: Int,
    /** 다음 강화 비용. 최대 강화면 null */
    val nextCostStones: Int?,
    val nextCostGold: Long?,
)

data class AccessoryResponse(
    val id: Long,
    val type: AccessoryType,
    val typeName: String,
    val grade: Grade,
    val gradeName: String,
    val stat: Stat,
    val statName: String,
    val bonus: Int,
    val equipped: Boolean,
) {
    companion object {
        fun from(a: Accessory): AccessoryResponse {
            val grade = Grade.entries[a.grade]
            return AccessoryResponse(
                id = a.id!!,
                type = a.type,
                typeName = a.type.displayName,
                grade = grade,
                gradeName = grade.displayName,
                stat = a.type.stat,
                statName = a.type.stat.displayName,
                bonus = a.statBonus,
                equipped = a.equipped,
            )
        }
    }
}

/** 캐릭터 화면에 필요한 정보 한 번에 */
data class CharacterResponse(
    val id: Long,
    val nickname: String,
    val level: Int,
    val totalExp: Long,
    val expInLevel: Long,
    val expToNextLevel: Long,
    val gold: Long,
    val stamina: Int,
    val maxStamina: Int,
    val statPoints: Int,
    val stats: List<StatResponse>,
    val weapon: WeaponResponse,
    val enhanceStones: Int,
    val accessories: List<AccessoryResponse>,
    val todayKm: Double,
    val weekKm: Double,
)

data class AllocateStatsRequest(
    @field:PositiveOrZero @field:Max(1000) val attack: Int = 0,
    @field:PositiveOrZero @field:Max(1000) val defense: Int = 0,
    @field:PositiveOrZero @field:Max(1000) val agility: Int = 0,
    @field:PositiveOrZero @field:Max(1000) val spirit: Int = 0,
)

data class StatValue(val stat: Stat, val name: String, val value: Int)

data class DifficultyResponse(
    val difficulty: Int,
    val minLevel: Int,
    val minGrade: Grade,
    val minGradeName: String,
    val minStats: List<StatValue>,
    val reward: String,
    /** 지금 바로 도전 가능한지 */
    val available: Boolean,
    /** 도전할 수 없는 이유 (레벨 부족 등) */
    val reasons: List<String>,
)

data class ContentResponse(
    val type: ContentType,
    val name: String,
    val staminaCost: Int,
    val difficulties: List<DifficultyResponse>,
)

data class ContentRewardResponse(
    val gold: Long,
    val enhanceStones: Int,
    val accessory: AccessoryResponse?,
)

data class ContentResultResponse(
    /** 예: "던전 3단계" */
    val content: String,
    val reward: ContentRewardResponse,
    val character: CharacterResponse,
)

data class EnhanceResponse(
    val enhance: Int,
    val character: CharacterResponse,
)
