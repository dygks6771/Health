package com.example.healthrpg

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
class RunRewardTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired objectMapper: ObjectMapper,
) {
    private val api = ApiTestSupport(mockMvc, objectMapper)

    private fun syncRun(token: String, id: String, start: String, end: String, meters: Int) =
        api.json(
            mockMvc.post("/api/me/runs") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = """{"runs":[{"externalId":"$id","startTime":"$start","endTime":"$end","distanceMeters":$meters}]}"""
            }.andExpect { status { isOk() } },
        )

    /**
     * 테스트 러닝 버튼을 20분 안에 여러 번 누르면 기록 구간이 겹쳐 거리가 합산되고(9km/20분),
     * 속도 초과로 보상이 빠진다. 이때 오늘 거리(todayKm)는 늘지만 보상 거리(todayRewardKm)는 늘지 않아야 함.
     */
    @Test
    fun `rejected runs show in todayKm but not in todayRewardKm`() {
        val token = api.signup()
        api.createHero(token)
        val day = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"))
        val start = day.atTime(9, 0).atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant()
        val end = start.plusSeconds(20 * 60)

        val ok = syncRun(token, "t1", start.toString(), end.toString(), 3000)
        assertEquals(150, ok["reward"]["expGained"].asInt())

        val tooFast = syncRun(token, "t2", start.plusSeconds(60).toString(), end.plusSeconds(60).toString(), 9000)
        assertEquals(0, tooFast["reward"]["expGained"].asInt())
        assertEquals(1, tooFast["reward"]["rejected"].size())
        assertEquals(12.0, tooFast["hero"]["todayKm"].asDouble())
        assertEquals(3.0, tooFast["hero"]["todayRewardKm"].asDouble())

        // 하루 상한 없음: 30km 도 전부 경험치 (정상 속도)
        val long = syncRun(token, "t3", start.plusSeconds(3600).toString(), start.plusSeconds(3600 + 3 * 3600).toString(), 30_000)
        assertEquals(1500, long["reward"]["expGained"].asInt())
        assertEquals(0, long["hero"]["stamina"].asInt())

        val history = api.json(api.get(token, "/api/me/runs"))
        assertEquals(3, history["runs"].size())
        assertEquals(0, history["runs"][1]["expAwarded"].asInt())
        assertEquals(true, history["runs"][1]["rejectedReason"].asString().startsWith("속도 초과"))
    }

    @Test
    fun `steps charge stamina with a daily cap and only the increase counts`() {
        val token = api.signup()
        api.createHero(token)

        assertEquals(50, api.steps(token, 5_000)["reward"]["staminaGained"].asInt())
        assertEquals(30, api.steps(token, 8_000)["reward"]["staminaGained"].asInt())
        // 다시 같은 값 → 0, 하루 상한 150 (15,000보)
        assertEquals(0, api.steps(token, 8_000)["reward"]["staminaGained"].asInt())
        val capped = api.steps(token, 40_000)
        assertEquals(70, capped["reward"]["staminaGained"].asInt())
        assertEquals(150, capped["hero"]["stamina"].asInt())
        assertEquals(40_000, capped["hero"]["todaySteps"].asLong())
        assertEquals(150, capped["hero"]["todayStepStamina"].asInt())

        // 미래 날짜는 거부
        api.post(token, "/api/me/steps", """{"days":[{"date":"2999-01-01","steps":100}]}""").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `daily reward chest once a day`() {
        val token = api.signup()
        api.createHero(token)
        api.get(token, "/api/me/daily").andExpect { jsonPath("$.claimedToday") { value(false) } }
        val claim = api.json(api.post(token, "/api/me/daily/claim").andExpect { status { isOk() } })
        assertTrue(claim["gold"].asLong() >= 100)
        assertTrue(claim["chest"]["name"].asString().endsWith("상자"))
        api.post(token, "/api/me/daily/claim").andExpect { status { isBadRequest() } }
        api.get(token, "/api/me/daily").andExpect { jsonPath("$.claimedToday") { value(true) } }
    }

    @Test
    fun `bulk sell removes only unequipped items up to the grade`() {
        val token = api.signup()
        val weaponId = api.createHero(token)["equipment"][0]["id"].asLong()
        api.run(token, 10)
        repeat(2) { api.post(token, "/api/me/shop/WEAPON/draw") }
        api.post(token, "/api/me/items/$weaponId/unequip")

        val before = api.json(api.get(token, "/api/me/items"))["items"].size()
        val result = api.json(api.post(token, "/api/me/items/sell-bulk?maxGrade=LEGENDARY").andExpect { status { isOk() } })
        val left = result["inventory"]["items"]
        // 장착 중인 장비만 남음 (여기서는 모두 미장착이라 전부 판매)
        assertEquals(before - result["soldCount"].asInt(), left.size())
        assertTrue((0 until left.size()).all { left[it]["equipped"].asBoolean() })
    }
}
