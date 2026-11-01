package com.example.health.sync

import com.example.health.health.DailySteps
import com.example.health.health.RunRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** 서버가 거절한 요청. message 는 서버가 보낸 이유 (예: "레벨 5 필요") */
class ServerException(val code: Int, message: String) : IOException(message)

/** Health RPG 서버 API 호출 (server/ 의 REST API 와 1:1 대응) */
class ServerApi(baseUrl: String) {

    private val baseUrl = baseUrl.trim().trimEnd('/')

    /** GET /api/health → 서버·DB 상태 */
    suspend fun health(): JSONObject = request("GET", "/api/health")

    /** POST /api/users → 같은 deviceId 면 기존 사용자를 돌려주므로 매번 호출해도 됨 */
    suspend fun registerUser(deviceId: String, nickname: String): Long =
        request(
            "POST",
            "/api/users",
            JSONObject().put("deviceId", deviceId).put("nickname", nickname),
        ).getLong("id")

    /** POST /api/users/{id}/steps → 날짜별 걸음수 기록 (개발자 탭 확인용, 게임 보상 없음) */
    suspend fun syncSteps(userId: Long, days: List<DailySteps>, source: String?): JSONArray {
        val entries = JSONArray()
        days.forEach { day ->
            entries.put(
                JSONObject()
                    .put("date", day.date.toString())
                    .put("steps", day.steps)
                    .put("source", source ?: JSONObject.NULL),
            )
        }
        return JSONArray(requestText("POST", "/api/users/$userId/steps", JSONObject().put("entries", entries)))
    }

    /** POST /api/users/{id}/runs → 러닝 기록 동기화. 응답: reward(보상), character */
    suspend fun syncRuns(userId: Long, runs: List<RunRecord>): JSONObject {
        val list = JSONArray()
        runs.forEach { run ->
            list.put(
                JSONObject()
                    .put("externalId", run.id)
                    .put("startTime", run.startTime.toString())
                    .put("endTime", run.endTime.toString())
                    .put("distanceMeters", run.distanceMeters),
            )
        }
        return request("POST", "/api/users/$userId/runs", JSONObject().put("runs", list))
    }

    /** GET /api/users/{id}/character → 레벨, 스탯, 장비 등 */
    suspend fun character(userId: Long): JSONObject = request("GET", "/api/users/$userId/character")

    /** POST /api/users/{id}/stats → 스탯 포인트 [amount] 만큼 [stat](ATTACK 등)에 찍기. 응답: character */
    suspend fun allocateStat(userId: Long, stat: String, amount: Int = 1): JSONObject =
        request("POST", "/api/users/$userId/stats", JSONObject().put(stat.lowercase(), amount))

    /** POST /api/users/{id}/weapon/enhance → 응답: enhance, character */
    suspend fun enhanceWeapon(userId: Long): JSONObject = request("POST", "/api/users/$userId/weapon/enhance")

    /** GET /api/users/{id}/content → 토벌/던전/레이드 단계별 조건 */
    suspend fun content(userId: Long): JSONArray = JSONArray(requestText("GET", "/api/users/$userId/content"))

    /** POST /api/users/{id}/content/{type}/{difficulty} → 응답: content, reward, character */
    suspend fun attemptContent(userId: Long, type: String, difficulty: Int): JSONObject =
        request("POST", "/api/users/$userId/content/$type/$difficulty")

    /** GET /api/users/{id}/steps → 서버에 저장된 기간 합계 */
    suspend fun totalSteps(userId: Long, from: LocalDate, to: LocalDate): Long =
        request("GET", "/api/users/$userId/steps?from=$from&to=$to").getLong("totalSteps")

    private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject {
        val text = requestText(method, path, body)
        return if (text.trimStart().startsWith("{")) JSONObject(text) else JSONObject()
    }

    private suspend fun requestText(method: String, path: String, body: JSONObject? = null): String =
        withContext(Dispatchers.IO) {
            val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = 5_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/json")
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val message = runCatching { JSONObject(text).getString("message") }.getOrDefault(text)
                    throw ServerException(code, message.ifBlank { "HTTP $code" })
                }
                text
            } finally {
                conn.disconnect()
            }
        }
}
