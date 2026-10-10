package com.example.health.sync

import com.example.health.health.StepsReader

data class SyncResult(
    val userId: Long,
    val syncedDays: Int,
    /** 동기화 후 서버 DB 에 저장된 같은 기간 합계 (앱에서 읽은 값과 같아야 정상) */
    val serverTotal: Long,
    val localTotal: Long,
)

/** Health Connect 에서 최근 걸음수를 읽어 서버로 보낸다 */
class StepsSyncer(
    private val reader: StepsReader,
    private val settings: SyncSettings,
) {
    suspend fun sync(days: Int = 7): SyncResult {
        val api = ServerApi(settings.serverUrl)

        // 서버 DB 를 초기화해도 deviceId 로 다시 등록되도록 매번 호출 (서버에서 중복 등록 안 됨)
        val userId = api.registerUser(settings.deviceId, DEFAULT_NICKNAME)
        settings.userId = userId

        val daily = reader.readDaily(days)
        api.syncSteps(userId, daily, source = SOURCE)

        return SyncResult(
            userId = userId,
            syncedDays = daily.size,
            serverTotal = api.totalSteps(userId, daily.first().date, daily.last().date),
            localTotal = daily.sumOf { it.steps },
        )
    }

    companion object {
        private const val DEFAULT_NICKNAME = "용사"

        /** 하루 합계는 Health Connect 가 여러 출처를 중복 제거해 합친 값 */
        private const val SOURCE = "health_connect"
    }
}
