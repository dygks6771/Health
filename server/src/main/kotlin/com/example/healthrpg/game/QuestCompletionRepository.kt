package com.example.healthrpg.game

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface QuestCompletionRepository : JpaRepository<QuestCompletion, Long> {
    fun existsByUserIdAndQuestAndPeriodStart(userId: Long, quest: Quest, periodStart: LocalDate): Boolean

    fun findAllByUserIdAndPeriodStartBetween(userId: Long, from: LocalDate, to: LocalDate): List<QuestCompletion>
}
