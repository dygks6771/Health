package com.example.healthrpg

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @Test
    fun `health check reports database up`() {
        mockMvc.get("/api/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
            jsonPath("$.database") { value("UP") }
        }
    }

    @Test
    fun `register user is idempotent by device id`() {
        val first = registerUser("device-idem")
        val second = registerUser("device-idem")
        assert(first == second)
    }

    @Test
    fun `sync steps upserts per day and summary sums them`() {
        val userId = registerUser("device-steps")

        syncSteps(userId, """[{"date":"2026-10-01","steps":3000},{"date":"2026-10-02","steps":5000}]""")
        // 같은 날 다시 동기화하면 덮어씀
        syncSteps(userId, """[{"date":"2026-10-02","steps":8000,"source":"com.sec.android.app.shealth"}]""")

        mockMvc.get("/api/users/$userId/steps?from=2026-10-01&to=2026-10-07").andExpect {
            status { isOk() }
            jsonPath("$.totalSteps") { value(11000) }
            jsonPath("$.days.length()") { value(2) }
            jsonPath("$.days[1].steps") { value(8000) }
            jsonPath("$.days[1].source") { value("com.sec.android.app.shealth") }
        }
    }

    @Test
    fun `negative steps are rejected`() {
        val userId = registerUser("device-negative")
        mockMvc.post("/api/users/$userId/steps") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"entries":[{"date":"2026-10-01","steps":-1}]}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `unknown user returns 404`() {
        mockMvc.get("/api/users/999999").andExpect { status { isNotFound() } }
    }

    private fun registerUser(deviceId: String): Long {
        val body = mockMvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"deviceId":"$deviceId","nickname":"tester"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return objectMapper.readTree(body).get("id").asLong()
    }

    private fun syncSteps(userId: Long, entriesJson: String) {
        mockMvc.post("/api/users/$userId/steps") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"entries":$entriesJson}"""
        }.andExpect { status { isOk() } }
    }
}
