package com.example.health.health

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
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

    /** 개발용 테스트 러닝 기록 권한 */
    val writePermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
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

    /** 개발용: 방금 [count] 걸음을 걸은 것으로 기록 */
    suspend fun insertTestSteps(count: Long = 3_000) {
        val end = Instant.now()
        val start = end.minusSeconds(60)
        val rules = ZoneId.systemDefault().rules
        client.insertRecords(
            listOf<Record>(
                StepsRecord(
                    startTime = start,
                    startZoneOffset = rules.getOffset(start),
                    endTime = end,
                    endZoneOffset = rules.getOffset(end),
                    count = count,
                    metadata = Metadata.manualEntry(),
                ),
            ),
        )
    }

    /**
     * 개발용: [km] km 러닝(20분)을 이 앱 이름으로 Health Connect 에 기록.
     * 여러 번 눌러도 기록 시간이 겹치지 않도록 직전 테스트 기록 바로 앞 시간대로 거슬러 올라가며 기록한다.
     * (겹치면 거리가 합산돼 "20분에 9km" 같은 속도 초과로 보상이 빠짐)
     */
    // Android Studio 검사기(lint)가 공개 생성자를 같은 인자 목록의 라이브러리 내부 생성자로 잘못 인식해
    // "can only be called from within the same library" 를 띄움. 실제 호출은 공개 생성자라 경고만 끈다.
    @SuppressLint("RestrictedApi")
    suspend fun insertTestRun(km: Double = 3.0) {
        val prefs = context.getSharedPreferences("test_runs", Context.MODE_PRIVATE)
        val now = Instant.now()
        val latestEnd = Instant.ofEpochMilli(prefs.getLong(KEY_LATEST_END, 0L))
        val earliestStart = Instant.ofEpochMilli(prefs.getLong(KEY_EARLIEST_START, 0L))
        val end: Instant
        if (!now.minusSeconds(20 * 60).isBefore(latestEnd)) {
            // 마지막 테스트 기록과 겹치지 않음 → 방금 끝난 러닝으로 기록
            end = now
            prefs.edit { putLong(KEY_LATEST_END, now.toEpochMilli()) }
        } else {
            // 연속으로 눌렀음 → 가장 이른 테스트 기록 1분 전에 끝난 러닝으로 기록
            end = earliestStart.minusSeconds(60)
        }
        val start = end.minusSeconds(20 * 60)
        prefs.edit { putLong(KEY_EARLIEST_START, start.toEpochMilli()) }
        val rules = ZoneId.systemDefault().rules
        client.insertRecords(
            // 타입을 명시하지 않으면 내부 전용 공통 타입(IntervalRecord)으로 추론돼 컴파일 오류
            listOf<Record>(
                ExerciseSessionRecord(
                    startTime = start,
                    startZoneOffset = rules.getOffset(start),
                    endTime = end,
                    endZoneOffset = rules.getOffset(end),
                    metadata = Metadata.manualEntry(),
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                    title = "테스트 러닝",
                    // 공개 생성자에만 있는 인자 (내부 생성자는 exerciseRouteResult)
                    exerciseRoute = null,
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
        private const val KEY_LATEST_END = "latest_end"
        private const val KEY_EARLIEST_START = "earliest_start"

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
