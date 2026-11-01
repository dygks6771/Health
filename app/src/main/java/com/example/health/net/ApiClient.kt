package com.example.health.net

import com.example.health.health.RunRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 서버가 거절한 요청. message 는 서버가 보낸 이유 (예: "레벨 6 필요") */
class ApiException(val status: Int, val code: String?, message: String) : IOException(message) {
    val unauthorized: Boolean get() = status == 401
    val heroNotFound: Boolean get() = code == "HERO_NOT_FOUND"
}

/** Health RPG 서버 API (server/ 의 REST API 와 1:1 대응). 로그인 토큰은 [Session] 에서 읽음 */
class ApiClient(private val session: Session) {

    // ── 인증 ──
    suspend fun signup(loginId: String, password: String): JSONObject =
        obj("POST", "/api/auth/signup", JSONObject().put("loginId", loginId).put("password", password))

    suspend fun login(loginId: String, password: String): JSONObject =
        obj("POST", "/api/auth/login", JSONObject().put("loginId", loginId).put("password", password))

    suspend fun logout() {
        text("POST", "/api/auth/logout")
    }

    suspend fun health(): JSONObject = obj("GET", "/api/health")

    // ── 영웅 ──
    suspend fun meta(): JSONObject = obj("GET", "/api/game/meta")

    suspend fun hero(): JSONObject = obj("GET", "/api/me/hero")

    /** 영웅 이름 중복 확인 → {available, reason} */
    suspend fun nameAvailable(name: String): JSONObject =
        obj("GET", "/api/game/names/available?name=" + java.net.URLEncoder.encode(name.trim(), "UTF-8"))

    suspend fun createHero(name: String, job: String, appearance: Int): JSONObject =
        obj("POST", "/api/me/hero", JSONObject().put("name", name).put("job", job).put("appearance", appearance))

    /** [stat] = ATTACK / DEFENSE / AGILITY / SPIRIT */
    suspend fun allocateStat(stat: String, amount: Int = 1): JSONObject =
        obj("POST", "/api/me/hero/stats", JSONObject().put(stat.lowercase(), amount))

    suspend fun syncRuns(runs: List<RunRecord>): JSONObject {
        val list = JSONArray()
        runs.forEach {
            list.put(
                JSONObject()
                    .put("externalId", it.id)
                    .put("startTime", it.startTime.toString())
                    .put("endTime", it.endTime.toString())
                    .put("distanceMeters", it.distanceMeters),
            )
        }
        return obj("POST", "/api/me/runs", JSONObject().put("runs", list))
    }

    /** 날짜별 걸음수 → 행동력 */
    suspend fun syncSteps(days: List<Pair<java.time.LocalDate, Long>>): JSONObject {
        val list = JSONArray()
        days.forEach { (date, steps) -> list.put(JSONObject().put("date", date.toString()).put("steps", steps)) }
        return obj("POST", "/api/me/steps", JSONObject().put("days", list))
    }

    /** 일괄 판매: [maxGrade] 이하 (COMMON / UNCOMMON / RARE …) */
    suspend fun sellBulk(maxGrade: String): JSONObject = obj("POST", "/api/me/items/sell-bulk?maxGrade=$maxGrade")

    /** 최근 러닝 기록 20건과 보상 (빠진 이유 포함) */
    suspend fun runHistory(): JSONObject = obj("GET", "/api/me/runs")

    // ── 가방·대장간 ──
    suspend fun items(): JSONObject = obj("GET", "/api/me/items")
    suspend fun equip(itemId: Long): JSONObject = obj("POST", "/api/me/items/$itemId/equip")
    suspend fun unequip(itemId: Long): JSONObject = obj("POST", "/api/me/items/$itemId/unequip")
    suspend fun sell(itemId: Long): JSONObject = obj("POST", "/api/me/items/$itemId/sell")
    suspend fun enhance(itemId: Long): JSONObject = obj("POST", "/api/me/smith/$itemId/enhance")

    // ── 출석 보상 ──
    suspend fun dailyStatus(): JSONObject = obj("GET", "/api/me/daily")
    suspend fun claimDaily(): JSONObject = obj("POST", "/api/me/daily/claim")

    // ── 상점 ──
    suspend fun shop(): JSONObject = obj("GET", "/api/me/shop")

    /** [category] = WEAPON / ARMOR */
    suspend fun draw(category: String): JSONObject = obj("POST", "/api/me/shop/$category/draw")

    // ── 랭킹 ──
    suspend fun ranking(): JSONObject = obj("GET", "/api/ranking?limit=100")

    // ── 경매장 ──
    suspend fun auction(slot: String?): JSONObject = obj("GET", "/api/me/auction" + (slot?.let { "?slot=$it" } ?: ""))
    suspend fun auctionMine(): JSONObject = obj("GET", "/api/me/auction/mine")

    suspend fun listItem(itemId: Long, price: Long): JSONObject =
        obj("POST", "/api/me/auction", JSONObject().put("itemId", itemId).put("price", price))

    suspend fun buyListing(listingId: Long): JSONObject = obj("POST", "/api/me/auction/$listingId/buy")
    suspend fun cancelListing(listingId: Long): JSONObject = obj("POST", "/api/me/auction/$listingId/cancel")

    // ── 모험자 길드 ──
    suspend fun guild(): JSONObject = obj("GET", "/api/me/guild/hunts")
    suspend fun hunt(slot: Int): JSONObject = obj("POST", "/api/me/guild/hunts/$slot")

    // ── 세계·던전 ──
    suspend fun world(): JSONObject = obj("GET", "/api/me/world")
    suspend fun dungeon(tier: Int): JSONObject = obj("POST", "/api/me/world/$tier/dungeon")

    // ── 친구 ──
    suspend fun friends(): JSONObject = obj("GET", "/api/me/friends")
    suspend fun addFriend(name: String): JSONObject = obj("POST", "/api/me/friends", JSONObject().put("name", name))
    suspend fun acceptFriend(friendshipId: Long): JSONObject = obj("POST", "/api/me/friends/$friendshipId/accept")
    suspend fun removeFriend(friendshipId: Long): JSONObject = obj("DELETE", "/api/me/friends/$friendshipId")

    /** 개발용: 친구 봇 4명 (서버 game.dev-tools.enabled=true 일 때만) */
    suspend fun devFriendBots(): JSONObject = obj("POST", "/api/dev/friend-bots")

    /** 개발용: 레이드 주 1회 제한 초기화 */
    suspend fun devRaidReset(): JSONObject = obj("POST", "/api/dev/raid-reset")

    // ── 레이드 ──
    suspend fun raid(): JSONObject = obj("GET", "/api/me/raid")

    suspend fun attackRaid(friendHeroIds: List<Long>): JSONObject =
        obj("POST", "/api/me/raid", JSONObject().put("friendHeroIds", JSONArray(friendHeroIds)))

    private suspend fun obj(method: String, path: String, body: JSONObject? = null): JSONObject {
        val text = text(method, path, body)
        return if (text.trimStart().startsWith("{")) JSONObject(text) else JSONObject()
    }

    private suspend fun text(method: String, path: String, body: JSONObject? = null): String =
        withContext(Dispatchers.IO) {
            val url = session.serverUrl.trim().trimEnd('/') + path
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = 5_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/json")
                session.token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }

                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    val error = runCatching { JSONObject(text) }.getOrNull()
                    throw ApiException(
                        status = status,
                        code = error?.optString("code")?.ifBlank { null },
                        message = error?.optString("message")?.ifBlank { null } ?: "서버 오류 (HTTP $status)",
                    )
                }
                text
            } finally {
                conn.disconnect()
            }
        }
}
