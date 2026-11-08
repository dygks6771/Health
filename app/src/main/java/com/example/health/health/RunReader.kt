package com.example.health.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

/** Health Connect 러닝 기록 1건 */
data class RunRecord(
    /** Health Connect 기록 ID. 서버에서 같은 러닝을 두 번 보상하지 않는 데 사용 */
    val id: String,
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Int,
    /** 기록한 앱 (삼성헬스면 com.sec.android.app.shealth) */
    val source: String,
)

/**
 * 삼성헬스 등에서 Health Connect 로 들어온 러닝(운동 세션) 기록과 걸음수를 읽는다.
 * 러닝 km → 경험치·골드, 걸음수 → 행동력.
 * 거리는 운동 세션 시간 동안 같은 앱이 기록한 거리(DistanceRecord)의 합계.
 */
class RunReader(private val context: Context) {

    /** 러닝(경험치) + 걸음수(행동력) 읽기 */
    val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
    )

    /** 처음 권한을 요청할 때 한 번에 묻는 권한들 (백그라운드 읽기는 지원 기기에서만 표시됨) */
    val requestPermissions: Set<String> get() = permissions + BACKGROUND_PERMISSION

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    suspend fun hasAllPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(permissions)

    /** 최근 [days]일 러닝 기록 (오래된 것부터) */
    suspend fun readRuns(days: Int = 7, zone: ZoneId = ZoneId.systemDefault()): List<RunRecord> {
        val start = LocalDate.now(zone).minusDays(days - 1L).atStartOfDay(zone).toInstant()
        val sessions = mutableListOf<ExerciseSessionRecord>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(start),
                    pageToken = pageToken,
                ),
            )
            sessions += response.records
            pageToken = response.pageToken
        } while (pageToken != null)

        return sessions
            .filter { it.exerciseType in RUNNING_TYPES }
            .map { session ->
                val distance = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime),
                        dataOriginFilter = setOf(session.metadata.dataOrigin),
                    ),
                )[DistanceRecord.DISTANCE_TOTAL]
                RunRecord(
                    id = session.metadata.id,
                    startTime = session.startTime,
                    endTime = session.endTime,
                    distanceMeters = distance?.inMeters?.toInt() ?: 0,
                    source = session.metadata.dataOrigin.packageName,
                )
            }
            .sortedBy { it.startTime }
    }

    /** 최근 [days]일 날짜별 걸음수 (Health Connect 가 여러 기기 중복을 제거한 합계) */
    suspend fun readDailySteps(days: Int = 7, zone: ZoneId = ZoneId.systemDefault()): List<Pair<LocalDate, Long>> {
        val today = LocalDate.now(zone)
        val start = today.minusDays(days - 1L)
        val response = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                timeRangeSlicer = Period.ofDays(1),
            ),
        )
        val byDate = response.associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
        return (0 until days).map { start.plusDays(it.toLong()) }.map { it to (byDate[it] ?: 0L) }
    }

    companion object {
        /**
         * 앱이 꺼져 있을 때(자동 동기화) Health Connect 를 읽기 위한 권한.
         * 지원하지 않는 기기/Health Connect 버전에서는 권한 화면에 나오지 않고 무시된다.
         */
        const val BACKGROUND_PERMISSION = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

        private val RUNNING_TYPES = setOf(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
        )
    }
}
