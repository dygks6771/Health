package com.example.healthrpg.game

import com.example.healthrpg.steps.DailySteps
import com.example.healthrpg.steps.DailyStepsRepository
import com.example.healthrpg.user.User
import com.example.healthrpg.user.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class GameService(
    private val userService: UserService,
    private val dailyStepsRepository: DailyStepsRepository,
    private val questCompletionRepository: QuestCompletionRepository,
    @Value("\${game.zone:Asia/Seoul}") zone: String,
) {
    private val zoneId: ZoneId = ZoneId.of(zone)

    fun today(): LocalDate = LocalDate.now(zoneId)

    /**
     * 동기화된 날짜들에 대해 경험치·퀘스트 보상 지급.
     * 날짜별로 이미 지급한 경험치를 기록해 두므로 같은 날을 여러 번 동기화해도 중복 지급되지 않는다.
     * 호출하는 쪽에서 사용자 행을 잠근 트랜잭션 안에서 불러야 함.
     */
    @Transactional
    fun applySync(user: User, days: List<DailySteps>): SyncRewardResponse {
        val userId = user.id!!
        val levelBefore = user.level
        var expGained = 0L
        var goldGained = 0L
        val completed = mutableListOf<String>()

        for (day in days) {
            val earned = GameRules.expForSteps(day.steps)
            if (earned > day.expAwarded) {
                expGained += earned - day.expAwarded
                day.expAwarded = earned
            }
        }

        for (quest in Quest.entries) {
            // 같은 주에 속한 날짜가 여러 개면 주간 퀘스트는 한 번만 확인
            val periods = days.map { quest.periodStart(it.date) }.distinct()
            for (periodStart in periods) {
                if (questCompletionRepository.existsByUserIdAndQuestAndPeriodStart(userId, quest, periodStart)) continue
                if (stepsInPeriod(userId, quest, periodStart) < quest.targetSteps) continue

                questCompletionRepository.save(QuestCompletion(user, quest, periodStart))
                expGained += quest.rewardExp
                goldGained += quest.rewardGold
                completed += quest.title
            }
        }

        user.exp += expGained
        user.gold += goldGained
        user.level = GameRules.progress(user.exp).level

        return SyncRewardResponse(expGained, goldGained, levelBefore, user.level, completed)
    }

    fun character(userId: Long): CharacterResponse {
        val user = userService.get(userId)
        val today = today()
        val progress = GameRules.progress(user.exp)
        val weekStart = Quest.WEEKLY_50K.periodStart(today)
        val done = questCompletionRepository.findAllByUserIdAndPeriodStartBetween(userId, weekStart, today)
            .map { it.quest to it.periodStart }
            .toSet()

        val quests = Quest.entries.map { quest ->
            val periodStart = quest.periodStart(today)
            QuestStatusResponse(
                code = quest.name,
                title = quest.title,
                period = quest.period,
                progress = stepsInPeriod(userId, quest, periodStart),
                target = quest.targetSteps,
                completed = (quest to periodStart) in done,
                rewardExp = quest.rewardExp,
                rewardGold = quest.rewardGold,
            )
        }

        return CharacterResponse(
            id = userId,
            nickname = user.nickname,
            level = progress.level,
            totalExp = user.exp,
            expInLevel = progress.expInLevel,
            expToNextLevel = progress.expToNextLevel,
            gold = user.gold,
            todaySteps = dailyStepsRepository.findByUserIdAndDate(userId, today)?.steps ?: 0,
            quests = quests,
        )
    }

    private fun stepsInPeriod(userId: Long, quest: Quest, periodStart: LocalDate): Long =
        dailyStepsRepository
            .findAllByUserIdAndDateBetweenOrderByDateAsc(userId, periodStart, quest.periodEnd(periodStart))
            .sumOf { it.steps }
}
