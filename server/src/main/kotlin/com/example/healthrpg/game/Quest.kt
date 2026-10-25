package com.example.healthrpg.game

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class QuestPeriod { DAILY, WEEKLY }

/** 걸음수 퀘스트. 목표를 달성하면 동기화할 때 자동으로 보상 지급 (기간마다 1번) */
enum class Quest(
    val title: String,
    val period: QuestPeriod,
    val targetSteps: Long,
    val rewardExp: Long,
    val rewardGold: Long,
) {
    DAILY_5K("오늘 5,000보 걷기", QuestPeriod.DAILY, 5_000, rewardExp = 20, rewardGold = 50),
    DAILY_10K("오늘 10,000보 걷기", QuestPeriod.DAILY, 10_000, rewardExp = 30, rewardGold = 100),
    WEEKLY_50K("이번 주 50,000보 걷기", QuestPeriod.WEEKLY, 50_000, rewardExp = 100, rewardGold = 300),
    ;

    /** 일일 퀘스트는 그날, 주간 퀘스트는 그 주 월요일 */
    fun periodStart(date: LocalDate): LocalDate = when (period) {
        QuestPeriod.DAILY -> date
        QuestPeriod.WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    fun periodEnd(date: LocalDate): LocalDate = when (period) {
        QuestPeriod.DAILY -> date
        QuestPeriod.WEEKLY -> periodStart(date).plusDays(6)
    }
}
