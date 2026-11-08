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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
class GameApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired objectMapper: ObjectMapper,
) {
    private val api = ApiTestSupport(mockMvc, objectMapper)

    @Test
    fun `signup, login, create hero`() {
        val loginId = ApiTestSupport.nextId()
        val token = api.signup(loginId)

        // 같은 아이디 / 틀린 비밀번호 / 토큰 없음
        mockMvc.post("/api/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"loginId":"$loginId","password":"password123"}"""
        }.andExpect { status { isConflict() } }
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"loginId":"$loginId","password":"wrong-password"}"""
        }.andExpect { status { isUnauthorized() } }
        mockMvc.get("/api/me/hero").andExpect { status { isUnauthorized() } }

        // 영웅이 없으면 404 HERO_NOT_FOUND → 앱이 생성 화면으로
        api.get(token, "/api/me/hero").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("HERO_NOT_FOUND") }
        }

        // 전사: 공격력 5 + 직업 3 + 견습 무기 6 = 14
        val hero = api.createHero(token, name = "아서왕", job = "WARRIOR")
        assertEquals("전사", hero["jobName"].asString())
        assertEquals(14, hero["stats"][0]["total"].asInt())
        assertEquals("견습생의 대검", hero["equipment"][0]["name"].asString())

        api.post(token, "/api/me/hero", """{"name":"아서왕","job":"MAGE","appearance":1}""")
            .andExpect { status { isConflict() } }

        // 다시 로그인해도 같은 영웅
        val login = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"loginId":"$loginId","password":"password123"}"""
        }.andExpect { status { isOk() } }
        val token2 = api.json(login)["token"].asString()
        api.get(token2, "/api/me/hero").andExpect { jsonPath("$.name") { value("아서왕") } }

        // 로그아웃한 토큰은 사용 불가
        api.post(token2, "/api/auth/logout").andExpect { status { isNoContent() } }
        api.get(token2, "/api/me/hero").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `running levels up hero and grows job stat`() {
        val token = api.signup()
        api.createHero(token, job = "ARCHER")

        // 5km → EXP 250 → Lv3: 스탯 포인트 6, 궁수 주 능력치(민첩성) +2
        val result = api.run(token, 5)
        assertEquals(3, result["reward"]["levelAfter"].asInt())
        assertEquals(6, result["hero"]["statPoints"].asInt())
        assertEquals(5 + 3 + 2, result["hero"]["stats"][2]["base"].asInt())
        assertEquals(0, result["hero"]["stamina"].asInt()) // 러닝은 행동력을 주지 않음 (걸음수로 충전)

        api.post(token, "/api/me/hero/stats", """{"attack":6}""").andExpect {
            status { isOk() }
            jsonPath("$.statPoints") { value(0) }
        }
    }

    @Test
    fun `guild posts 5 daily hunts and each can be cleared once`() {
        val token = api.signup()
        api.createHero(token)
        api.run(token, 5)
        api.steps(token, 5_000)

        val board = api.json(api.get(token, "/api/me/guild/hunts"))
        val hunts = board["hunts"]
        assertEquals(5, hunts.size())
        val difficulties = (0 until 5).map { hunts[it]["difficulty"].asString() }
        assertEquals(mapOf("EASY" to 2, "NORMAL" to 2, "HARD" to 1), difficulties.groupingBy { it }.eachCount())

        // 다시 봐도 같은 의뢰
        val again = api.json(api.get(token, "/api/me/guild/hunts"))
        assertEquals(hunts[0]["monster"].asString(), again["hunts"][0]["monster"].asString())

        val easy = (0 until 5).first { hunts[it]["difficulty"].asString() == "EASY" }
        val slot = hunts[easy]["slot"].asInt()
        api.post(token, "/api/me/guild/hunts/$slot").andExpect {
            status { isOk() }
            jsonPath("$.victory") { value(true) }
            jsonPath("$.winChance") { value(100) }
            jsonPath("$.hero.stamina") { value(40) }
        }
        api.post(token, "/api/me/guild/hunts/$slot").andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("오늘 이미 완료한 의뢰") }
        }
    }

    @Test
    fun `dungeons have level and stat requirements and give rewards`() {
        val token = api.signup()
        api.createHero(token)
        api.run(token, 5)
        api.steps(token, 5_000)

        val world = api.json(api.get(token, "/api/me/world"))
        assertEquals(10, world["regions"].size())
        assertEquals("초보자 마을", world["regions"][0]["name"].asString())
        assertTrue(world["regions"][0]["dungeon"]["available"].asBoolean())

        // 능력치가 최소치의 1.5배 이상이면 승률 100%. 보상은 상자 등급에 따라 기본(강화석 2) 이상
        val cleared = api.json(api.post(token, "/api/me/world/1/dungeon").andExpect { status { isOk() } })
        assertEquals(true, cleared["victory"].asBoolean())
        assertEquals(100, cleared["winChance"].asInt())
        assertTrue(cleared["rewardStones"].asInt() >= 2)
        assertEquals(cleared["rewardStones"].asInt(), cleared["hero"]["enhanceStones"].asInt())
        assertEquals(30, cleared["hero"]["stamina"].asInt())
        api.post(token, "/api/me/world/2/dungeon").andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("레벨 6 필요, 능력치 합계 44 필요") }
        }
        // 원정 지도: 지역 3 은 누적 15km 필요 (지금 5km)
        assertEquals(5.0, world["totalKm"].asDouble())
        assertEquals(15, world["regions"][2]["dungeon"]["requiredKm"].asInt())
        assertEquals("누적 러닝 15km 필요 (현재 5.0km)", world["regions"][2]["dungeon"]["reasons"][0].asString())
    }

    @Test
    fun `smith enhances and inventory sells items`() {
        val token = api.signup()
        val hero = api.createHero(token)
        val weaponId = hero["equipment"][0]["id"].asLong()
        api.run(token, 5)
        api.steps(token, 5_000)
        api.post(token, "/api/me/world/1/dungeon")

        // +0 → +1 은 성공률 100%: 강화석 1, 골드 30
        val stones = api.json(api.get(token, "/api/me/hero"))["enhanceStones"].asInt()
        api.post(token, "/api/me/smith/$weaponId/enhance").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.item.enhance") { value(1) }
            jsonPath("$.hero.enhanceStones") { value(stones - 1) }
        }

        api.post(token, "/api/me/items/$weaponId/sell").andExpect { status { isBadRequest() } }
        api.post(token, "/api/me/items/$weaponId/unequip").andExpect { status { isOk() } }
        val before = api.json(api.get(token, "/api/me/hero"))["gold"].asLong()
        val sold = api.json(api.post(token, "/api/me/items/$weaponId/sell"))
        assertEquals(before + 30, sold["hero"]["gold"].asLong())
    }

    @Test
    fun `friends by hero name`() {
        val a = api.signup()
        api.createHero(a, name = "란슬롯")
        val b = api.signup()
        api.createHero(b, name = "가웨인")

        api.post(a, "/api/me/friends", """{"name":"가웨인"}""").andExpect {
            status { isOk() }
            jsonPath("$.outgoing[0].hero.name") { value("가웨인") }
        }
        val incoming = api.json(api.get(b, "/api/me/friends"))["incoming"][0]
        api.post(b, "/api/me/friends/${incoming["friendshipId"].asLong()}/accept").andExpect {
            jsonPath("$.friends[0].hero.name") { value("란슬롯") }
        }
        api.post(a, "/api/me/friends", """{"name":"가웨인"}""").andExpect { status { isConflict() } }
        api.post(a, "/api/me/friends", """{"name":"없는영웅"}""").andExpect { status { isNotFound() } }
    }

    @Test
    fun `raid needs 4 friends and grades the damage`() {
        val token = api.signup()
        api.createHero(token)
        api.run(token, 10)
        api.steps(token, 20_000)

        api.post(token, "/api/me/raid", """{"friendHeroIds":[1,2,3,4]}""").andExpect { status { isBadRequest() } }

        val friends = api.json(api.post(token, "/api/dev/friend-bots"))["friends"]
        assertEquals(4, friends.size())
        val ids = (0 until 4).map { friends[it]["hero"]["id"].asLong() }

        val result = api.json(
            api.post(token, "/api/me/raid", """{"friendHeroIds":$ids}""").andExpect { status { isOk() } },
        )
        assertEquals(10, result["turns"].size())
        assertEquals(5, result["members"].size())
        assertTrue(result["totalDamage"].asLong() > 0)
        assertTrue(result["grade"].asString() in listOf("S", "A", "B", "C", "D", "E", "F"))
        assertEquals(150 - 30, result["hero"]["stamina"].asInt())
        // 러닝 배율: 이번 주 0km → ×0.5, 테스트 봇은 6km → ×1.1
        assertEquals(0.5, result["members"][0]["runBonus"].asDouble())
        assertEquals(1.1, result["members"][1]["runBonus"].asDouble(), 1e-9)

        api.get(token, "/api/me/raid").andExpect {
            jsonPath("$.friends.length()") { value(4) }
            jsonPath("$.bestGrade") { value(result["grade"].asString()) }
        }
    }
}
