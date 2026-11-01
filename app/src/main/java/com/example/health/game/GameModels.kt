package com.example.health.game

import org.json.JSONArray
import org.json.JSONObject

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

data class StatInfo(
    /** ATTACK / DEFENSE / AGILITY / SPIRIT (서버 API 에 보내는 값) */
    val code: String,
    val name: String,
    val base: Int,
    val bonus: Int,
    val total: Int,
) {
    companion object {
        fun from(json: JSONObject) = StatInfo(
            code = json.getString("stat"),
            name = json.getString("name"),
            base = json.getInt("base"),
            bonus = json.getInt("bonus"),
            total = json.getInt("total"),
        )
    }
}

data class WeaponInfo(
    val enhance: Int,
    val gradeName: String,
    val attackBonus: Int,
    val maxEnhance: Int,
    /** 다음 강화 비용. 최대 강화면 null */
    val nextCostStones: Int?,
    val nextCostGold: Long?,
) {
    companion object {
        fun from(json: JSONObject) = WeaponInfo(
            enhance = json.getInt("enhance"),
            gradeName = json.getString("gradeName"),
            attackBonus = json.getInt("attackBonus"),
            maxEnhance = json.getInt("maxEnhance"),
            nextCostStones = if (json.isNull("nextCostStones")) null else json.getInt("nextCostStones"),
            nextCostGold = if (json.isNull("nextCostGold")) null else json.getLong("nextCostGold"),
        )
    }
}

data class AccessoryInfo(
    val id: Long,
    val typeName: String,
    val gradeName: String,
    val statName: String,
    val bonus: Int,
    val equipped: Boolean,
) {
    companion object {
        fun from(json: JSONObject) = AccessoryInfo(
            id = json.getLong("id"),
            typeName = json.getString("typeName"),
            gradeName = json.getString("gradeName"),
            statName = json.getString("statName"),
            bonus = json.getInt("bonus"),
            equipped = json.getBoolean("equipped"),
        )
    }
}

/** 서버 GET /api/users/{id}/character 응답 */
data class GameCharacter(
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
    val stats: List<StatInfo>,
    val weapon: WeaponInfo,
    val enhanceStones: Int,
    val accessories: List<AccessoryInfo>,
    val todayKm: Double,
    val weekKm: Double,
) {
    val levelProgress: Float
        get() = if (expToNextLevel <= 0) 0f else (expInLevel.toFloat() / expToNextLevel).coerceIn(0f, 1f)

    val staminaProgress: Float
        get() = if (maxStamina <= 0) 0f else (stamina.toFloat() / maxStamina).coerceIn(0f, 1f)

    companion object {
        fun from(json: JSONObject) = GameCharacter(
            id = json.getLong("id"),
            nickname = json.getString("nickname"),
            level = json.getInt("level"),
            totalExp = json.getLong("totalExp"),
            expInLevel = json.getLong("expInLevel"),
            expToNextLevel = json.getLong("expToNextLevel"),
            gold = json.getLong("gold"),
            stamina = json.getInt("stamina"),
            maxStamina = json.getInt("maxStamina"),
            statPoints = json.getInt("statPoints"),
            stats = json.getJSONArray("stats").mapObjects(StatInfo::from),
            weapon = WeaponInfo.from(json.getJSONObject("weapon")),
            enhanceStones = json.getInt("enhanceStones"),
            accessories = json.getJSONArray("accessories").mapObjects(AccessoryInfo::from),
            todayKm = json.getDouble("todayKm"),
            weekKm = json.getDouble("weekKm"),
        )
    }
}

/** 러닝 동기화 1번으로 얻은 보상 */
data class RunReward(
    val newRuns: Int,
    val countedKm: Double,
    val expGained: Long,
    val goldGained: Long,
    val staminaGained: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    val statPointsGained: Int,
    val rejected: List<String>,
) {
    val leveledUp: Boolean get() = levelAfter > levelBefore
    val isEmpty: Boolean get() = newRuns == 0

    companion object {
        fun from(json: JSONObject) = RunReward(
            newRuns = json.getInt("newRuns"),
            countedKm = json.getDouble("countedKm"),
            expGained = json.getLong("expGained"),
            goldGained = json.getLong("goldGained"),
            staminaGained = json.getInt("staminaGained"),
            levelBefore = json.getInt("levelBefore"),
            levelAfter = json.getInt("levelAfter"),
            statPointsGained = json.getInt("statPointsGained"),
            rejected = json.getJSONArray("rejected").strings(),
        )
    }
}

data class DifficultyInfo(
    val difficulty: Int,
    val minLevel: Int,
    val minGradeName: String,
    /** 능력치 이름 → 최소값 */
    val minStats: List<Pair<String, Int>>,
    val reward: String,
    val available: Boolean,
    val reasons: List<String>,
) {
    companion object {
        fun from(json: JSONObject) = DifficultyInfo(
            difficulty = json.getInt("difficulty"),
            minLevel = json.getInt("minLevel"),
            minGradeName = json.getString("minGradeName"),
            minStats = json.getJSONArray("minStats").mapObjects { it.getString("name") to it.getInt("value") },
            reward = json.getString("reward"),
            available = json.getBoolean("available"),
            reasons = json.getJSONArray("reasons").strings(),
        )
    }
}

/** 토벌 / 던전 / 레이드 */
data class ContentInfo(
    /** HUNT / DUNGEON / RAID (서버 API 에 보내는 값) */
    val type: String,
    val name: String,
    val staminaCost: Int,
    val difficulties: List<DifficultyInfo>,
) {
    companion object {
        fun listFrom(json: JSONArray): List<ContentInfo> = json.mapObjects {
            ContentInfo(
                type = it.getString("type"),
                name = it.getString("name"),
                staminaCost = it.getInt("staminaCost"),
                difficulties = it.getJSONArray("difficulties").mapObjects(DifficultyInfo::from),
            )
        }
    }
}

/** 컨텐츠 클리어 결과 */
data class ContentResult(
    val content: String,
    val gold: Long,
    val enhanceStones: Int,
    /** 레이드 장신구 (예: "희귀 반지 (공격력 +9)") */
    val accessory: String?,
    val character: GameCharacter,
) {
    val rewardText: String
        get() = listOfNotNull(
            "골드 +$gold".takeIf { gold > 0 },
            "강화석 +$enhanceStones".takeIf { enhanceStones > 0 },
            accessory,
        ).joinToString(" · ")

    companion object {
        fun from(json: JSONObject): ContentResult {
            val reward = json.getJSONObject("reward")
            val accessory = if (reward.isNull("accessory")) null else AccessoryInfo.from(reward.getJSONObject("accessory"))
            return ContentResult(
                content = json.getString("content"),
                gold = reward.getLong("gold"),
                enhanceStones = reward.getInt("enhanceStones"),
                accessory = accessory?.let { "${it.gradeName} ${it.typeName} (${it.statName} +${it.bonus})" },
                character = GameCharacter.from(json.getJSONObject("character")),
            )
        }
    }
}

/** 레벨별 칭호와 캐릭터 모습 (그림 리소스가 생기기 전까지 이모지로 표시) */
object CharacterLook {
    fun title(level: Int): String = when {
        level >= 40 -> "전설의 러너"
        level >= 20 -> "베테랑 러너"
        level >= 10 -> "숙련 모험가"
        else -> "초보 모험가"
    }

    fun avatar(level: Int): String = when {
        level >= 40 -> "🧙"
        level >= 20 -> "🦸"
        level >= 10 -> "🧝"
        else -> "🐣"
    }
}
