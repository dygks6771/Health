package com.example.healthrpg.steps

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class StepEntry(
    val date: LocalDate,

    @field:PositiveOrZero
    val steps: Long,

    /** 데이터 출처 앱 패키지명 (예: com.sec.android.app.shealth) */
    @field:Size(max = 100)
    val source: String? = null,
)

/** 앱에서 여러 날짜를 한 번에 동기화 */
data class SyncStepsRequest(
    @field:NotEmpty
    @field:Size(max = 31)
    @field:Valid
    val entries: List<StepEntry>,
)

data class DailyStepsResponse(
    val date: LocalDate,
    val steps: Long,
    val source: String?,
    val syncedAt: Instant,
) {
    companion object {
        fun from(e: DailySteps) = DailyStepsResponse(e.date, e.steps, e.source, e.syncedAt)
    }
}

data class StepsSummaryResponse(
    val userId: Long,
    val from: LocalDate,
    val to: LocalDate,
    val totalSteps: Long,
    val days: List<DailyStepsResponse>,
)
