package com.example.healthrpg.steps

import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.user.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class StepsService(
    private val userService: UserService,
    private val dailyStepsRepository: DailyStepsRepository,
) {
    /**
     * 날짜별 걸음수 upsert.
     * Health Connect 집계값이 그날의 최신 누적값이므로 같은 날짜는 덮어쓴다.
     */
    @Transactional
    fun sync(userId: Long, request: SyncStepsRequest): List<DailySteps> {
        val user = userService.get(userId)
        val now = Instant.now()
        return request.entries
            .associateBy { it.date } // 같은 날짜가 중복으로 오면 마지막 값 사용
            .values
            .map { entry ->
                val existing = dailyStepsRepository.findByUserIdAndDate(userId, entry.date)
                if (existing != null) {
                    existing.steps = entry.steps
                    existing.source = entry.source
                    existing.syncedAt = now
                    existing
                } else {
                    dailyStepsRepository.save(
                        DailySteps(user = user, date = entry.date, steps = entry.steps, source = entry.source)
                            .apply { syncedAt = now },
                    )
                }
            }
            .sortedBy { it.date }
    }

    fun summary(userId: Long, from: LocalDate, to: LocalDate): StepsSummaryResponse {
        if (from.isAfter(to)) throw BadRequestException("from must be before or equal to to")
        if (ChronoUnit.DAYS.between(from, to) > 366) throw BadRequestException("range must be within 1 year")
        userService.get(userId)

        val days = dailyStepsRepository.findAllByUserIdAndDateBetweenOrderByDateAsc(userId, from, to)
            .map(DailyStepsResponse::from)
        return StepsSummaryResponse(userId, from, to, days.sumOf { it.steps }, days)
    }
}
