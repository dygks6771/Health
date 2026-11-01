package com.example.healthrpg

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
class MarketApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired objectMapper: ObjectMapper,
) {
    private val api = ApiTestSupport(mockMvc, objectMapper)

    @Test
    fun `shop draws weapons and armor for gold`() {
        val token = api.signup()
        api.createHero(token)
        api.run(token, 10) // 골드 200

        val shop = api.json(api.get(token, "/api/me/shop"))
        assertEquals(150, shop["offers"][0]["price"].asLong())
        assertEquals(5, shop["chances"].size())

        val weapon = api.json(api.post(token, "/api/me/shop/WEAPON/draw").andExpect { status { isOk() } })
        assertEquals("WEAPON", weapon["item"]["slot"].asString())
        assertEquals(50, weapon["hero"]["gold"].asLong())

        api.post(token, "/api/me/shop/ARMOR/draw").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `hero names are unique ignoring case`() {
        val a = api.signup()
        api.createHero(a, name = "Arthur")
        mockMvc.get("/api/game/names/available?name=arthur").andExpect {
            status { isOk() }
            jsonPath("$.available") { value(false) }
        }
        mockMvc.get("/api/game/names/available?name=Merlin").andExpect { jsonPath("$.available") { value(true) } }
        api.post(api.signup(), "/api/me/hero", """{"name":"ARTHUR","job":"MAGE","appearance":0}""")
            .andExpect { status { isConflict() } }
    }

    @Test
    fun `ranking orders every hero by level x 100 + power and skips bots`() {
        val strong = api.signup()
        api.createHero(strong, name = "랭커강자")
        api.run(strong, 20)
        val weak = api.signup()
        api.createHero(weak, name = "랭커약자")
        api.post(weak, "/api/dev/friend-bots")

        val ranking = api.json(api.get(weak, "/api/ranking"))
        val names = (0 until ranking["entries"].size()).map { ranking["entries"][it]["name"].asString() }
        assertTrue(names.indexOf("랭커강자") < names.indexOf("랭커약자"))
        assertTrue(names.none { it.startsWith("용병") })
        val me = ranking["me"]
        assertEquals("랭커약자", me["name"].asString())
        assertEquals(me["level"].asLong() * 100 + me["power"].asLong(), me["score"].asLong())
    }

    @Test
    fun `raid is limited to once a week`() {
        val token = api.signup()
        api.createHero(token)
        api.run(token, 20)
        api.steps(token, 15_000)
        val friends = api.json(api.post(token, "/api/dev/friend-bots"))["friends"]
        val ids = (0 until 4).map { friends[it]["hero"]["id"].asLong() }

        api.post(token, "/api/me/raid", """{"friendHeroIds":$ids}""").andExpect { status { isOk() } }
        api.get(token, "/api/me/raid").andExpect { jsonPath("$.remainingThisWeek") { value(0) } }
        api.post(token, "/api/me/raid", """{"friendHeroIds":$ids}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("레이드는 주 1회입니다. 월요일 0시에 초기화됩니다") }
        }

        // 개발용 초기화 후 다시 가능
        api.post(token, "/api/dev/raid-reset").andExpect { status { isOk() } }
        api.post(token, "/api/me/raid", """{"friendHeroIds":$ids}""").andExpect { status { isOk() } }
    }

    @Test
    fun `auction moves item and gold with a 5 percent fee`() {
        val seller = api.signup()
        val sellerHero = api.createHero(seller)
        val weaponId = sellerHero["equipment"][0]["id"].asLong()
        val buyer = api.signup()
        api.createHero(buyer)
        api.run(buyer, 10) // 골드 200

        // 장착 중이면 등록 불가 → 해제 후 등록
        api.post(seller, "/api/me/auction", """{"itemId":$weaponId,"price":100}""").andExpect { status { isBadRequest() } }
        api.post(seller, "/api/me/items/$weaponId/unequip")
        val listed = api.json(api.post(seller, "/api/me/auction", """{"itemId":$weaponId,"price":100}""").andExpect { status { isOk() } })
        val listingId = listed["listings"][0]["id"].asLong()

        // 등록 중에는 장착·판매 불가, 자기 물건 구매 불가
        api.post(seller, "/api/me/items/$weaponId/equip").andExpect { status { isBadRequest() } }
        api.post(seller, "/api/me/auction/$listingId/buy").andExpect { status { isBadRequest() } }

        val board = api.json(api.get(buyer, "/api/me/auction?slot=WEAPON"))
        assertTrue((0 until board["listings"].size()).any { board["listings"][it]["id"].asLong() == listingId })

        val bought = api.json(api.post(buyer, "/api/me/auction/$listingId/buy").andExpect { status { isOk() } })
        assertEquals(100, bought["hero"]["gold"].asLong())
        assertEquals(weaponId, bought["item"]["id"].asLong())

        assertEquals(95, api.json(api.get(seller, "/api/me/hero"))["gold"].asLong())
        val buyerItems = api.json(api.get(buyer, "/api/me/items"))["items"]
        assertTrue((0 until buyerItems.size()).any { buyerItems[it]["id"].asLong() == weaponId })
        api.post(buyer, "/api/me/auction/$listingId/buy").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `auction listing can be cancelled`() {
        val seller = api.signup()
        val weaponId = api.createHero(seller)["equipment"][0]["id"].asLong()
        api.post(seller, "/api/me/items/$weaponId/unequip")
        val listingId = api.json(api.post(seller, "/api/me/auction", """{"itemId":$weaponId,"price":500}"""))["listings"][0]["id"].asLong()

        api.post(seller, "/api/me/auction/$listingId/cancel").andExpect {
            status { isOk() }
            jsonPath("$.listings[0].status") { value("CANCELLED") }
        }
        api.post(seller, "/api/me/items/$weaponId/equip").andExpect { status { isOk() } }
    }
}
