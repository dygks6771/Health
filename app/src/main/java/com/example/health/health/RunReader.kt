package com.example.health.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import java.time.Instant
import java.time.LocalDate
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
 * 삼성헬스 등에서 Health Connect 로 들어온 러닝(운동 세션) 기록을 읽는다.
 * 거리는 운동 세션 시간 동안 같은 앱이 기록한 거리(DistanceRecord)의 합계.
 */
class RunReader(private val context: Context) {

    val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    /** 처음 권한을 요청할 때 한 번에 묻는 권한들 (백그라운드 읽기는 지원 기기에서만 표시됨) */
    val requestPermissions: Set<String> get() = permissions + StepsReader.BACKGROUND_PERMISSION

    /** 개발용 테스트 러닝 기록 권한 */
    val writePermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
    )

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    suspend fun hasAllPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(permissions)

    suspend fun hasWritePermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(writePermissions)

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

    /** 개발용: 방금 끝난 [km] km 러닝(20분)을 이 앱 이름으로 Health Connect 에 기록 */
    suspend fun insertTestRun(km: Double = 3.0) {
        val end = Instant.now()
        val start = end.minusSeconds(20 * 60)
        val rules = ZoneId.systemDefault().rules
        client.insertRecords(
            listOf(
                ExerciseSessionRecord(
                    startTime = start,
                    startZoneOffset = rules.getOffset(start),
                    endTime = end,
                    endZoneOffset = rules.getOffset(end),
                    metadata = Metadata.manualEntry(),
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                    title = "테스트 러닝",
                ),
                DistanceRecord(
                    startTime = start,
                    startZoneOffset = rules.getOffset(start),
                    endTime = end,
                    endZoneOffset = rules.getOffset(end),
                    distance = Length.meters(km * 1000),
                    metadata = Metadata.manualEntry(),
                ),
            ),
        )
    }

    companion object {
        private val RUNNING_TYPES = setOf(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
        )
    }
}
