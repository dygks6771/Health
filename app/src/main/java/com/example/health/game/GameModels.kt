package com.example.health.game

import org.json.JSONObject

/** 서버 GET /api/users/{id}/character 응답 */
data class GameCharacter(
    val id: Long,
    val nickname: String,
    val level: Int,
    val totalExp: Long,
    val expInLevel: Long,
    val expToNextLevel: Long,
    val gold: Long,
    val todaySteps: Long,
    val quests: List<QuestStatus>,
) {
    val levelProgress: Float
        get() = if (expToNextLevel <= 0) 0f else (expInLevel.toFloat() / expToNextLevel).coerceIn(0f, 1f)

    companion object {
        fun from(json: JSONObject): GameCharacter {
            val quests = json.getJSONArray("quests")
            return GameCharacter(
                id = json.getLong("id"),
                nickname = json.getString("nickname"),
                level = json.getInt("level"),
                totalExp = json.getLong("totalExp"),
                expInLevel = json.getLong("expInLevel"),
                expToNextLevel = json.getLong("expToNextLevel"),
                gold = json.getLong("gold"),
                todaySteps = json.getLong("todaySteps"),
                quests = (0 until quests.length()).map { QuestStatus.from(quests.getJSONObject(it)) },
            )
        }
    }
}

data class QuestStatus(
    val code: String,
    val title: String,
    val weekly: Boolean,
    val progress: Long,
    val target: Long,
    val completed: Boolean,
    val rewardExp: Long,
    val rewardGold: Long,
) {
    val fraction: Float
        get() = if (target <= 0) 0f else (progress.toFloat() / target).coerceIn(0f, 1f)

    companion object {
        fun from(json: JSONObject) = QuestStatus(
            code = json.getString("code"),
            title = json.getString("title"),
            weekly = json.getString("period") == "WEEKLY",
            progress = json.getLong("progress"),
            target = json.getLong("target"),
            completed = json.getBoolean("completed"),
            rewardExp = json.getLong("rewardExp"),
            rewardGold = json.getLong("rewardGold"),
        )
    }
}

/** 동기화 1번으로 얻은 보상 */
data class SyncReward(
    val expGained: Long,
    val goldGained: Long,
    val levelBefore: Int,
    val levelAfter: Int,
    val completedQuests: List<String>,
) {
    val leveledUp: Boolean get() = levelAfter > levelBefore
    val isEmpty: Boolean get() = expGained == 0L && goldGained == 0L && completedQuests.isEmpty()

    companion object {
        fun from(json: JSONObject): SyncReward {
            val quests = json.getJSONArray("completedQuests")
            return SyncReward(
                expGained = json.getLong("expGained"),
                goldGained = json.getLong("goldGained"),
                levelBefore = json.getInt("levelBefore"),
                levelAfter = json.getInt("levelAfter"),
                completedQuests = (0 until quests.length()).map { quests.getString(it) },
            )
        }
    }
}

/** 레벨별 칭호와 캐릭터 모습 (그림 리소스가 생기기 전까지 이모지로 표시) */
object CharacterLook {
    fun title(level: Int): String = when {
        level >= 20 -> "전설의 워커"
        level >= 10 -> "베테랑 워커"
        level >= 5 -> "숙련 모험가"
        else -> "초보 모험가"
    }

    fun avatar(level: Int): String = when {
        level >= 20 -> "🧙"
        level >= 10 -> "🦸"
        level >= 5 -> "🧝"
        else -> "🐣"
    }
}
