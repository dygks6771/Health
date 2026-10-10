package com.example.health.sync

import com.example.health.health.DailySteps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

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

    /** POST /api/users/{id}/steps → 날짜별 걸음수 upsert */
    suspend fun syncSteps(userId: Long, days: List<DailySteps>, source: String?) {
        val entries = JSONArray()
        days.forEach { day ->
            entries.put(
                JSONObject()
                    .put("date", day.date.toString())
                    .put("steps", day.steps)
                    .put("source", source ?: JSONObject.NULL),
            )
        }
        request("POST", "/api/users/$userId/steps", JSONObject().put("entries", entries))
    }

    /** GET /api/users/{id}/steps → 서버에 저장된 기간 합계 */
    suspend fun totalSteps(userId: Long, from: LocalDate, to: LocalDate): Long =
        request("GET", "/api/users/$userId/steps?from=$from&to=$to").getLong("totalSteps")

    private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject =
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
                    throw ServerException(code, "HTTP $code: $message")
                }
                // 응답이 배열(걸음수 동기화 결과)인 경우는 내용을 쓰지 않으므로 빈 객체로 처리
                if (text.trimStart().startsWith("{")) JSONObject(text) else JSONObject()
            } finally {
                conn.disconnect()
            }
        }
}
