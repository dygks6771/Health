package com.example.healthrpg

import com.example.healthrpg.auth.AttemptLimiter
import com.example.healthrpg.hero.HeroRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired objectMapper: ObjectMapper,
    @Autowired private val heroRepository: HeroRepository,
) {
    private val api = ApiTestSupport(mockMvc, objectMapper)

    private fun login(loginId: String, password: String) = mockMvc.post("/api/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"loginId":"$loginId","password":"$password"}"""
    }

    @Test
    fun `five wrong passwords lock the login id for a while`() {
        val loginId = ApiTestSupport.nextId()
        api.signup(loginId, "password123")
        repeat(5) { login(loginId, "wrong-pass").andExpect { status { isUnauthorized() } } }
        // 맞는 비밀번호여도 잠시 차단
        login(loginId, "password123").andExpect {
            status { isTooManyRequests() }
            jsonPath("$.code") { value("TOO_MANY_REQUESTS") }
        }
    }

    @Test
    fun `success resets the fail count`() {
        val loginId = ApiTestSupport.nextId()
        api.signup(loginId, "password123")
        repeat(4) { login(loginId, "wrong-pass").andExpect { status { isUnauthorized() } } }
        login(loginId, "password123").andExpect { status { isOk() } }
        repeat(4) { login(loginId, "wrong-pass").andExpect { status { isUnauthorized() } } }
        login(loginId, "password123").andExpect { status { isOk() } }
    }

    @Test
    fun `account deletion needs the password and removes hero, bots and tokens`() {
        val loginId = ApiTestSupport.nextId()
        val token = api.signup(loginId, "password123")
        val hero = api.createHero(token)
        api.run(token, 3)
        val friends = api.json(api.post(token, "/api/dev/friend-bots").andExpect { status { isOk() } })["friends"]
        val botIds = (0 until friends.size()).map { friends[it]["hero"]["id"].asLong() }
        assertEquals(4, botIds.size)
        val heroId = hero["id"].asLong()
        assertNotNull(heroRepository.findById(heroId).orElse(null))

        api.post(token, "/api/me/account/delete", """{"password":"wrong-pass"}""").andExpect { status { isBadRequest() } }
        api.post(token, "/api/me/account/delete", """{"password":"password123"}""").andExpect { status { isNoContent() } }

        assertNull(heroRepository.findById(heroId).orElse(null))
        botIds.forEach { assertNull(heroRepository.findById(it).orElse(null)) }
        api.get(token, "/api/me/hero").andExpect { status { isUnauthorized() } }
        login(loginId, "password123").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `attempt limiter window expires`() {
        var now = Instant.parse("2026-11-01T00:00:00Z")
        val limiter = AttemptLimiter(2, Duration.ofMinutes(15)) { now }
        limiter.record("a")
        assertNull(limiter.blockedSeconds("a"))
        limiter.record("a")
        assertEquals(15 * 60L, limiter.blockedSeconds("a"))
        now = now.plus(Duration.ofMinutes(15))
        assertNull(limiter.blockedSeconds("a"))
    }

    @Test
    fun `web page can delete an account with id and password`() {
        val loginId = ApiTestSupport.nextId()
        api.signup(loginId, "password123")
        val del = { password: String ->
            mockMvc.post("/api/auth/delete-account") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"loginId":"$loginId","password":"$password"}"""
            }
        }
        del("wrong-pass").andExpect { status { isUnauthorized() } }
        del("password123").andExpect { status { isNoContent() } }
        login(loginId, "password123").andExpect { status { isUnauthorized() } }
        mockMvc.get("/delete-account.html").andExpect { status { isOk() } }
        mockMvc.get("/privacy.html").andExpect { status { isOk() } }
    }
}
