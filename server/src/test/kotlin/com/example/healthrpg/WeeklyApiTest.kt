package com.example.healthrpg

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
class WeeklyApiTest(
    @Autowired mockMvc: MockMvc,
    @Autowired objectMapper: ObjectMapper,
) {
    private val api = ApiTestSupport(mockMvc, objectMapper)
    private val today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString()

    @Test
    fun `weekly goal starts at 5km and pays a chest once when reached`() {
        val token = api.signup()
        api.createHero(token)

        val before = api.json(api.get(token, "/api/me/weekly").andExpect { status { isOk() } })
        assertEquals(5.0, before["goalKm"].asDouble())
        assertEquals(0.0, before["progressKm"].asDouble())
        assertFalse(before["achieved"].asBoolean())
        assertEquals(7, before["days"].size())
        assertEquals(1, before["expedition"]["unlockedTier"].asInt())
        api.post(token, "/api/me/weekly/claim").andExpect { status { isBadRequest() } }

        api.run(token, 5, day = today)
        val after = api.json(api.get(token, "/api/me/weekly"))
        assertEquals(5.0, after["progressKm"].asDouble())
        assertTrue(after["achieved"].asBoolean())
        assertEquals(1, after["runDays"].asInt())
        assertEquals(5.0, after["records"]["totalKm"].asDouble())
        assertEquals(5.0, after["records"]["longestRunKm"].asDouble())
        assertEquals(360, after["records"]["bestPaceSecPerKm"].asInt()) // 테스트 러닝은 10km/h = 6분/km
        // 누적 5km → 지역 2 해금, 다음은 15km
        assertEquals(2, after["expedition"]["unlockedTier"].asInt())
        assertEquals(15, after["expedition"]["nextRegionKm"].asInt())

        val claim = api.json(api.post(token, "/api/me/weekly/claim").andExpect { status { isOk() } })
        assertEquals("SILVER", claim["chest"]["tier"].asString())
        assertEquals(1, claim["streak"].asInt())
        assertTrue(claim["gold"].asLong() > 0)
        api.post(token, "/api/me/weekly/claim").andExpect { status { isBadRequest() } }
        assertTrue(api.json(api.get(token, "/api/me/weekly"))["claimed"].asBoolean())
    }
}
