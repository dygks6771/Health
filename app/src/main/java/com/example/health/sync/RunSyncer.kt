package com.example.health.sync

import com.example.health.game.GameCharacter
import com.example.health.game.RunReward
import com.example.health.health.RunReader
import org.json.JSONObject

data class RunSyncResult(
    val userId: Long,
    /** Health Connect 에서 읽은 러닝 수 */
    val readRuns: Int,
    val reward: RunReward,
    val character: GameCharacter,
)

/** Health Connect 러닝 기록을 서버로 보내고, 보상과 캐릭터 상태를 받아온다 */
class RunSyncer(
    private val reader: RunReader,
    private val settings: SyncSettings,
) {
    suspend fun sync(days: Int = 7): RunSyncResult {
        val api = ServerApi(settings.serverUrl)

        // 서버 DB 를 초기화해도 deviceId 로 다시 등록되도록 매번 호출 (서버에서 중복 등록 안 됨)
        val userId = api.registerUser(settings.deviceId, DEFAULT_NICKNAME)
        settings.userId = userId

        val runs = reader.readRuns(days)
        val response = api.syncRuns(userId, runs)
        val characterJson = response.getJSONObject("character")
        save(characterJson)
        settings.lastSyncAt = System.currentTimeMillis()

        return RunSyncResult(
            userId = userId,
            readRuns = runs.size,
            reward = RunReward.from(response.getJSONObject("reward")),
            character = GameCharacter.from(characterJson),
        )
    }

    /** 다른 화면(스탯·강화·컨텐츠)에서 받은 최신 캐릭터 상태도 저장 */
    fun save(characterJson: JSONObject) {
        settings.cachedCharacterJson = characterJson.toString()
    }

    /** 마지막으로 받아온 캐릭터 상태 (서버 연결 전에 먼저 보여주기용) */
    fun cachedCharacter(): GameCharacter? =
        settings.cachedCharacterJson?.let { runCatching { GameCharacter.from(JSONObject(it)) }.getOrNull() }

    companion object {
        private const val DEFAULT_NICKNAME = "용사"
    }
}
