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

        val history = api.json(api.get(token, "/api/me/runs"))
        assertEquals(2, history["runs"].size())
        assertEquals(0, history["runs"][0]["expAwarded"].asInt())
        assertEquals(true, history["runs"][0]["rejectedReason"].asString().startsWith("속도 초과"))
    }
}
