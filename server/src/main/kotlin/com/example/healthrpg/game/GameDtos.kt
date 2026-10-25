package com.example.healthrpg.game

data class QuestStatusResponse(
    val code: String,
    val title: String,
    val period: QuestPeriod,
    val progress: Long,
    val target: Long,
    val completed: Boolean,
    val rewardExp: Long,
    val rewardGold: Long,
)

/** 캐릭터 화면에 필요한 정보 한 번에 */
data class CharacterResponse(
    val id: Long,
    val nickname: String,
    val level: Int,
    val totalExp: Long,
    val expInLevel: Long,
    val expToNextLevel: Long,
    val gold: Long,
    val todaySteps: Long,
    val quests: List<QuestStatusResponse>,
)

/** 이번 동기화로 얻은 보상 */
data class SyncRewardResponse(
    val expGained: Long,
    val goldGained: Long,
    val levelBefore: Int,
    val levelAfter: Int,
    val completedQuests: List<String>,
) {
    val leveledUp: Boolean get() = levelAfter > levelBefore
}
