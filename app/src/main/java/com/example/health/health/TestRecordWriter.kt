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
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import java.time.Instant
import java.time.ZoneId

/**
 * 개발·시연용: 삼성헬스 없이 Health Connect 에 러닝·걸음 기록을 직접 넣는다.
 * debug 빌드의 설정 화면에서만 쓰며, 쓰기 권한도 debug 매니페스트에만 있다 (출시 빌드에는 없음).
 */
class TestRecordWriter(private val context: Context) {

    val writePermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
    )

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun hasWritePermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(writePermissions)

    /** 방금 [count] 걸음을 걸은 것으로 기록 */
    suspend fun insertSteps(count: Long = 3_000) {
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
     * [km] km 러닝(20분)을 기록. 여러 번 눌러도 기록 시간이 겹치지 않도록
     * 직전 테스트 기록 바로 앞 시간대로 거슬러 올라가며 기록한다 (겹치면 속도 초과로 보상 제외).
     */
    // Android Studio 검사기가 공개 생성자를 라이브러리 내부 생성자로 잘못 인식하는 경고만 끈다
    @SuppressLint("RestrictedApi")
    suspend fun insertRun(km: Double = 3.0) {
        val prefs = context.getSharedPreferences("test_runs", Context.MODE_PRIVATE)
        val now = Instant.now()
        val latestEnd = Instant.ofEpochMilli(prefs.getLong(KEY_LATEST_END, 0L))
        val earliestStart = Instant.ofEpochMilli(prefs.getLong(KEY_EARLIEST_START, 0L))
        val end: Instant
        if (!now.minusSeconds(20 * 60).isBefore(latestEnd)) {
            end = now
            prefs.edit { putLong(KEY_LATEST_END, now.toEpochMilli()) }
        } else {
            end = earliestStart.minusSeconds(60)
        }
        val start = end.minusSeconds(20 * 60)
        prefs.edit { putLong(KEY_EARLIEST_START, start.toEpochMilli()) }
        val rules = ZoneId.systemDefault().rules
        client.insertRecords(
            listOf<Record>(
                ExerciseSessionRecord(
                    startTime = start,
                    startZoneOffset = rules.getOffset(start),
                    endTime = end,
                    endZoneOffset = rules.getOffset(end),
                    metadata = Metadata.manualEntry(),
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                    title = "테스트 러닝",
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

    private companion object {
        const val KEY_LATEST_END = "latest_end"
        const val KEY_EARLIEST_START = "earliest_start"
    }
}
