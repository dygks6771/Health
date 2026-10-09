package com.example.health.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

/**
 * 삼성헬스 걸음수 읽기.
 *
 * 삼성헬스는 걸음수를 Health Connect 로 동기화하므로, 앱은 Health Connect 에서 읽는다.
 * (폰에서 삼성헬스 → 설정 → Health Connect 연동이 켜져 있어야 함)
 *
 * 걸음수는 반드시 aggregate API 로 읽는다. 원시 StepsRecord 를 더하면
 * 폰/워치 등 여러 출처의 데이터가 중복 합산되지만, aggregate 는 Health Connect 가 중복을 제거해준다.
 */
class StepsReader(private val context: Context) {

    val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    suspend fun hasAllPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(permissions)

    /** 오늘 걸음수 (전체 출처 합산 / 삼성헬스 출처만) */
    suspend fun readToday(zone: ZoneId = ZoneId.systemDefault()): TodaySteps {
        val today = LocalDate.now(zone)
        val range = TimeRangeFilter.between(today.atStartOfDay(), today.plusDays(1).atStartOfDay())

        val all = client.aggregate(
            AggregateRequest(metrics = setOf(StepsRecord.COUNT_TOTAL), timeRangeFilter = range),
        )
        val samsungOnly = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = range,
                dataOriginFilter = setOf(DataOrigin(SAMSUNG_HEALTH_PACKAGE)),
            ),
        )
        return TodaySteps(
            date = today,
            total = all[StepsRecord.COUNT_TOTAL] ?: 0L,
            samsungHealth = samsungOnly[StepsRecord.COUNT_TOTAL] ?: 0L,
            sources = all.dataOrigins.map { it.packageName }.sorted(),
        )
    }

    /** 최근 [days]일 날짜별 걸음수 (오늘 포함, 오래된 날짜 → 최신 순). 서버 동기화에 그대로 사용 */
    suspend fun readDaily(days: Int = 7, zone: ZoneId = ZoneId.systemDefault()): List<DailySteps> {
        require(days > 0)
        val today = LocalDate.now(zone)
        val start = today.minusDays(days - 1L)
        val response = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                timeRangeSlicer = Period.ofDays(1),
            ),
        )
        // 데이터가 없는 날은 결과에서 빠지므로 0 으로 채운다
        val byDate = response.associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
        return (0 until days).map { offset ->
            val date = start.plusDays(offset.toLong())
            DailySteps(date, byDate[date] ?: 0L)
        }
    }

    companion object {
        const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"
        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    }
}

data class TodaySteps(
    val date: LocalDate,
    val total: Long,
    val samsungHealth: Long,
    val sources: List<String>,
)

data class DailySteps(val date: LocalDate, val steps: Long)
