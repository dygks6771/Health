package com.example.healthrpg

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.ZoneId

@SpringBootTest
@AutoConfigureMockMvc
class GameApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    // 2026-10-07 은 수요일 (주간 퀘스트 기간: 10-05 ~ 10-11)
    private val wed = "2026-10-07"

    @Test
    fun `steps give exp and quest rewards once`() {
        val userId = registerUser("game-once")

        // 10,000보 = 걸음 경험치 100 + 일일 퀘스트 2개(20+30) = 150, 골드 50+100
        sync(userId, """[{"date":"$wed","steps":10000}]""").andExpect {
            jsonPath("$.reward.expGained") { value(150) }
            jsonPath("$.reward.goldGained") { value(150) }
            jsonPath("$.reward.levelBefore") { value(1) }
            jsonPath("$.reward.levelAfter") { value(2) }
            jsonPath("$.reward.leveledUp") { value(true) }
            jsonPath("$.reward.completedQuests.length()") { value(2) }
            jsonPath("$.character.totalExp") { value(150) }
            jsonPath("$.character.expInLevel") { value(50) }
            jsonPath("$.character.expToNextLevel") { value(150) }
        }

        // 같은 날 같은 걸음수로 다시 동기화 → 추가 보상 없음
        sync(userId, """[{"date":"$wed","steps":10000}]""").andExpect {
            jsonPath("$.reward.expGained") { value(0) }
            jsonPath("$.reward.goldGained") { value(0) }
            jsonPath("$.reward.completedQuests.length()") { value(0) }
        }

        // 걸음수가 늘면 늘어난 만큼만 (12,000 - 10,000 = 2,000보 = 20)
        sync(userId, """[{"date":"$wed","steps":12000}]""").andExpect {
            jsonPath("$.reward.expGained") { value(20) }
            jsonPath("$.character.totalExp") { value(170) }
        }
    }

    @Test
    fun `daily step exp is capped and weekly quest completes`() {
        val userId = registerUser("game-cap")

        // 걸음 경험치는 300 상한 + 일일 20+30 + 주간 100 = 450
        sync(userId, """[{"date":"$wed","steps":50000}]""").andExpect {
            jsonPath("$.reward.expGained") { value(450) }
            jsonPath("$.reward.goldGained") { value(450) }
            jsonPath("$.reward.completedQuests.length()") { value(3) }
        }
    }

    @Test
    fun `character shows today's quest progress`() {
        val userId = registerUser("game-character")
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

        sync(userId, """[{"date":"$today","steps":6000}]""")

        mockMvc.get("/api/users/$userId/character").andExpect {
            status { isOk() }
            jsonPath("$.todaySteps") { value(6000) }
            jsonPath("$.gold") { value(50) }
            jsonPath("$.quests[0].code") { value("DAILY_5K") }
            jsonPath("$.quests[0].completed") { value(true) }
            jsonPath("$.quests[1].code") { value("DAILY_10K") }
            jsonPath("$.quests[1].completed") { value(false) }
            jsonPath("$.quests[1].progress") { value(6000) }
        }
    }

    private fun registerUser(deviceId: String): Long {
        val body = mockMvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"deviceId":"$deviceId","nickname":"tester"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return objectMapper.readTree(body).get("id").asLong()
    }

    private fun sync(userId: Long, entriesJson: String): ResultActionsDsl =
        mockMvc.post("/api/users/$userId/steps") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"entries":$entriesJson}"""
        }.andExpect { status { isOk() } }
}
