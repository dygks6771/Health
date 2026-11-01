package com.example.healthrpg

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.atomic.AtomicInteger

/** 테스트용: 회원가입 → 영웅 생성 → 러닝으로 행동력·레벨 확보 */
class ApiTestSupport(private val mockMvc: MockMvc, private val objectMapper: ObjectMapper) {

    fun signup(loginId: String = nextId(), password: String = "password123"): String {
        val body = mockMvc.post("/api/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"loginId":"$loginId","password":"$password"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return objectMapper.readTree(body).get("token").asString()
    }

    fun createHero(token: String, name: String = "용사" + counter.incrementAndGet(), job: String = "WARRIOR"): JsonNode =
        json(
            mockMvc.post("/api/me/hero") {
                auth(token)
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"$name","job":"$job","appearance":0}"""
            }.andExpect { status { isCreated() } },
        )

    /** 하루 상한 안에서 [km] 만큼 뛴 기록 (속도 10km/h) */
    fun run(token: String, km: Int, id: String = "run-" + counter.incrementAndGet(), day: String = "2026-10-07"): JsonNode =
        json(
            mockMvc.post("/api/me/runs") {
                auth(token)
                contentType = MediaType.APPLICATION_JSON
                val minutes = km * 6
                content = """{"runs":[{"externalId":"$id","startTime":"${day}T00:00:00Z",""" +
                    """"endTime":"${day}T%02d:%02d:00Z","distanceMeters":${km * 1000}}]}""".format(minutes / 60, minutes % 60)
            }.andExpect { status { isOk() } },
        )

    /** 오늘(게임 시간대) 걸음수 → 행동력 (100보 = 1, 하루 최대 150) */
    fun steps(token: String, steps: Long, date: String = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString()): JsonNode =
        json(post(token, "/api/me/steps", """{"days":[{"date":"$date","steps":$steps}]}""").andExpect { status { isOk() } })

    fun get(token: String, path: String): ResultActionsDsl = mockMvc.get(path) { auth(token) }

    fun post(token: String, path: String, body: String? = null): ResultActionsDsl = mockMvc.post(path) {
        auth(token)
        if (body != null) {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }
    }

    fun json(result: ResultActionsDsl): JsonNode = objectMapper.readTree(result.andReturn().response.contentAsString)

    private fun org.springframework.test.web.servlet.MockHttpServletRequestDsl.auth(token: String) {
        header("Authorization", "Bearer $token")
    }

    companion object {
        private val counter = AtomicInteger()

        fun nextId() = "tester" + counter.incrementAndGet()
    }
}
