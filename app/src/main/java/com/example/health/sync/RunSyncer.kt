package com.example.health.sync

import com.example.health.game.HeroInfo
import com.example.health.game.RunReward
import com.example.health.health.RunReader
import com.example.health.net.ApiClient
import com.example.health.net.Session

data class RunSyncResult(
    /** Health Connect 에서 읽은 러닝 수 */
    val readRuns: Int,
    val reward: RunReward,
    val hero: HeroInfo,
)

/** Health Connect 러닝 기록을 서버로 보내고 보상과 영웅 상태를 받아온다 (로그인 필요) */
class RunSyncer(
    private val reader: RunReader,
    private val session: Session,
    private val api: ApiClient = ApiClient(session),
) {
    suspend fun sync(days: Int = 7): RunSyncResult {
        val runs = reader.readRuns(days)
        val response = api.syncRuns(runs)
        val heroJson = response.getJSONObject("hero")
        session.cachedHeroJson = heroJson.toString()
        session.lastSyncAt = System.currentTimeMillis()
        return RunSyncResult(
            readRuns = runs.size,
            reward = RunReward.from(response.getJSONObject("reward")),
            hero = HeroInfo.from(heroJson),
        )
    }
}
