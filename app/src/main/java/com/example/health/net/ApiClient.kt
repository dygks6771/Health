package com.example.health.net

import com.example.health.health.RunRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.net.UnknownServiceException
import javax.net.ssl.SSLException

/** 서버가 거절한 요청. message 는 서버가 보낸 이유 (예: "레벨 6 필요") */
class ApiException(val status: Int, val code: String?, message: String) : IOException(message) {
    val unauthorized: Boolean get() = status == 401
    val heroNotFound: Boolean get() = code == "HERO_NOT_FOUND"
}

/**
 * 서버에 닿지 못함 (꺼짐, 주소 오류, 네트워크 끊김). message 는 사용자에게 보여줄 원인과 해결 방법.
 * [ApiException] 과 달리 잠시 후 다시 시도하면 성공할 수 있다.
 */
class ServerUnreachableException(message: String, cause: Throwable) : IOException(message, cause)

/** 네트워크 오류를 사용자가 이해할 수 있는 원인으로 바꾼다 */
internal fun describeNetworkError(e: IOException, serverUrl: String): String = when (e) {
    is MalformedURLException -> "서버 주소 형식이 올바르지 않습니다 ($serverUrl). 예: http://192.168.0.10:8080"
    is UnknownHostException -> "서버 주소를 찾을 수 없습니다 ($serverUrl). 주소를 확인하거나 인터넷 연결을 확인해 주세요"
    is SocketTimeoutException -> "서버 응답이 없습니다. PC 서버가 켜져 있는지, 같은 와이파이(또는 Tailscale)에 연결돼 있는지 확인해 주세요"
    is ConnectException, is NoRouteToHostException ->
        if ("127.0.0.1" in serverUrl || "localhost" in serverUrl) {
            "서버에 연결할 수 없습니다. USB 연결 시 adb reverse 를 다시 실행하거나, 설정에서 PC 주소로 바꿔 주세요"
        } else {
            "서버에 연결할 수 없습니다. PC 서버가 켜져 있는지, PC 방화벽에서 8080 포트를 허용했는지 확인해 주세요"
        }
    is UnknownServiceException -> "이 빌드는 http:// 주소에 접속할 수 없습니다. https:// 주소를 사용해 주세요"
    is SSLException -> "보안 연결(https)에 실패했습니다. 주소가 맞는지 확인해 주세요"
    else -> if (e.message?.startsWith("gateway ") == true) {
        "원격 주소에는 연결됐지만 PC 서버가 꺼져 있습니다. PC 에서 서버를 켜 주세요"
    } else "서버에 연결하지 못했습니다 (${e.javaClass.simpleName})"
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

    // ── 주간 원정 ──
    suspend fun weekly(): JSONObject = obj("GET", "/api/me/weekly")

    suspend fun claimWeekly(): JSONObject = obj("POST", "/api/me/weekly/claim")

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
            val serverUrl = Session.normalizeServerUrl(session.serverUrl)
            try {
                request(serverUrl + path, method, body)
            } catch (e: ApiException) {
                throw e
            } catch (e: IOException) {
                throw ServerUnreachableException(describeNetworkError(e, serverUrl), e)
            }
        }

    private fun request(url: String, method: String, body: JSONObject?): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            // 와이파이·원격(Tailscale 등) 접속은 USB 보다 느릴 수 있어 여유를 둔다
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            // ngrok 무료 주소로 접속할 때 브라우저 안내 페이지 대신 API 응답을 받기 위한 헤더 (다른 서버는 무시)
            conn.setRequestProperty("ngrok-skip-browser-warning", "true")
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
                if (error == null && status in GATEWAY_ERRORS) {
                    // 원격 터널(ngrok·Cloudflare 등)은 연결됐지만 그 뒤의 PC 서버가 꺼져 있음 → 나중에 다시 시도
                    throw IOException("gateway $status")
                }
                throw ApiException(
                    status = status,
                    code = error?.optString("code")?.ifBlank { null },
                    message = error?.optString("message")?.ifBlank { null } ?: "서버 오류 (HTTP $status)",
                )
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        /** 프록시·터널이 "뒤쪽 서버에 닿지 못함"을 알릴 때 쓰는 상태 코드 (530: Cloudflare) */
        val GATEWAY_ERRORS = setOf(502, 503, 504, 530)
    }
}
