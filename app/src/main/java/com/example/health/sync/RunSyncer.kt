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
    /** 걸음수로 얻은 행동력 / 최대치에 막혀 못 받은 행동력 */
    val staminaGained: Int,
    val staminaOverflow: Int,
    val hero: HeroInfo,
)

/** 러닝 기록(경험치)과 걸음수(행동력)를 서버로 보내고 보상과 영웅 상태를 받아온다 (로그인 필요) */
class RunSyncer(
    private val reader: RunReader,
    private val session: Session,
    private val api: ApiClient = ApiClient(session),
) {
    suspend fun sync(days: Int = 7): RunSyncResult {
        val runs = reader.readRuns(days)
        val runResponse = api.syncRuns(runs)
        val stepResponse = api.syncSteps(reader.readDailySteps(days))
        val stepReward = stepResponse.getJSONObject("reward")
        val heroJson = stepResponse.getJSONObject("hero")
        session.cachedHeroJson = heroJson.toString()
        session.lastSyncAt = System.currentTimeMillis()
        return RunSyncResult(
            readRuns = runs.size,
            reward = RunReward.from(runResponse.getJSONObject("reward")),
            staminaGained = stepReward.getInt("staminaGained"),
            staminaOverflow = stepReward.getInt("staminaOverflow"),
            hero = HeroInfo.from(heroJson),
        )
    }
}
