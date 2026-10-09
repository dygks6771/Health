package com.example.healthrpg.steps

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailyStepsRepository : JpaRepository<DailySteps, Long> {
    fun findByUserIdAndDate(userId: Long, date: LocalDate): DailySteps?

    fun findAllByUserIdAndDateBetweenOrderByDateAsc(userId: Long, from: LocalDate, to: LocalDate): List<DailySteps>
}
