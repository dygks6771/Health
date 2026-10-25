package com.example.health.sync

import com.example.health.game.GameCharacter
import com.example.health.game.SyncReward
import com.example.health.health.StepsReader
import org.json.JSONObject

data class SyncResult(
    val userId: Long,
    val syncedDays: Int,
    /** 서버가 저장한 같은 기간 합계 (앱에서 읽은 값과 같아야 정상) */
    val serverTotal: Long,
    val localTotal: Long,
    val reward: SyncReward,
    val character: GameCharacter,
)

/** Health Connect 에서 최근 걸음수를 읽어 서버로 보내고, 보상과 캐릭터 상태를 받아온다 */
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
        val response = api.syncSteps(userId, daily, source = SOURCE)

        val characterJson = response.getJSONObject("character")
        settings.cachedCharacterJson = characterJson.toString()
        settings.lastSyncAt = System.currentTimeMillis()

        val savedDays = response.getJSONArray("days")
        return SyncResult(
            userId = userId,
            syncedDays = daily.size,
            serverTotal = (0 until savedDays.length()).sumOf { savedDays.getJSONObject(it).getLong("steps") },
            localTotal = daily.sumOf { it.steps },
            reward = SyncReward.from(response.getJSONObject("reward")),
            character = GameCharacter.from(characterJson),
        )
    }

    /** 마지막으로 받아온 캐릭터 상태 (서버 연결 전에 먼저 보여주기용) */
    fun cachedCharacter(): GameCharacter? =
        settings.cachedCharacterJson?.let { runCatching { GameCharacter.from(JSONObject(it)) }.getOrNull() }

    companion object {
        private const val DEFAULT_NICKNAME = "용사"

        /** 하루 합계는 Health Connect 가 여러 출처를 중복 제거해 합친 값 */
        private const val SOURCE = "health_connect"
    }
}
