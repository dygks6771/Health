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

@SpringBootTest
@AutoConfigureMockMvc
class RpgApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @Test
    fun `running gives exp, gold, stamina and stat points once`() {
        val userId = registerUser("rpg-run")

        // 5km / 30분 → EXP 250 (Lv1→3), 골드 100, 행동력 50, 스탯 포인트 6
        syncRun(userId, "run-1", "2026-10-07T06:00:00Z", "2026-10-07T06:30:00Z", 5000).andExpect {
            jsonPath("$.reward.newRuns") { value(1) }
            jsonPath("$.reward.expGained") { value(250) }
            jsonPath("$.reward.goldGained") { value(100) }
            jsonPath("$.reward.staminaGained") { value(50) }
            jsonPath("$.reward.levelAfter") { value(3) }
            jsonPath("$.reward.statPointsGained") { value(6) }
            jsonPath("$.character.statPoints") { value(6) }
            jsonPath("$.character.stamina") { value(50) }
        }

        // 같은 기록을 다시 보내면 보상 없음
        syncRun(userId, "run-1", "2026-10-07T06:00:00Z", "2026-10-07T06:30:00Z", 5000).andExpect {
            jsonPath("$.reward.newRuns") { value(0) }
            jsonPath("$.reward.expGained") { value(0) }
        }
    }

    @Test
    fun `too fast runs are rejected and daily distance is capped`() {
        val userId = registerUser("rpg-cap")

        // 10km / 10분 = 60km/h → 보상 없음
        syncRun(userId, "fast", "2026-10-07T01:00:00Z", "2026-10-07T01:10:00Z", 10000).andExpect {
            jsonPath("$.reward.expGained") { value(0) }
            jsonPath("$.reward.rejected.length()") { value(1) }
        }

        // 25km / 3시간 → 하루 상한 20km 까지만 (EXP 1000)
        syncRun(userId, "long", "2026-10-07T02:00:00Z", "2026-10-07T05:00:00Z", 25000).andExpect {
            jsonPath("$.reward.countedKm") { value(20.0) }
            jsonPath("$.reward.expGained") { value(1000) }
        }
    }

    @Test
    fun `stat points are spent on stats`() {
        val userId = registerUser("rpg-stats")
        syncRun(userId, "r", "2026-10-07T06:00:00Z", "2026-10-07T06:30:00Z", 5000)

        allocate(userId, """{"attack":4,"agility":2}""").andExpect {
            status { isOk() }
            jsonPath("$.statPoints") { value(0) }
            jsonPath("$.stats[0].stat") { value("ATTACK") }
            jsonPath("$.stats[0].total") { value(9) }
            jsonPath("$.stats[2].total") { value(7) }
        }
        allocate(userId, """{"defense":1}""").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `hunt gives gold and checks requirements`() {
        val userId = registerUser("rpg-hunt")

        // 행동력 없음
        attempt(userId, "HUNT", 1).andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("행동력 10 필요") }
        }

        syncRun(userId, "r", "2026-10-07T06:00:00Z", "2026-10-07T06:30:00Z", 5000)
        attempt(userId, "HUNT", 1).andExpect {
            status { isOk() }
            jsonPath("$.reward.gold") { value(50) }
            jsonPath("$.character.gold") { value(150) }
            jsonPath("$.character.stamina") { value(40) }
        }

        // 토벌 2단계: 레벨 3 은 됐지만 공격력·민첩성 7 필요
        attempt(userId, "HUNT", 2).andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("공격력 7 필요, 민첩성 7 필요") }
        }
    }

    @Test
    fun `dungeon gives enhance stones used for weapon enhance`() {
        val userId = registerUser("rpg-dungeon")
        syncRun(userId, "r", "2026-10-07T06:00:00Z", "2026-10-07T06:30:00Z", 5000)

        attempt(userId, "DUNGEON", 1).andExpect {
            status { isOk() }
            jsonPath("$.reward.enhanceStones") { value(2) }
        }
        mockMvc.post("/api/users/$userId/weapon/enhance").andExpect {
            status { isOk() }
            jsonPath("$.enhance") { value(1) }
            jsonPath("$.character.enhanceStones") { value(1) }
            jsonPath("$.character.gold") { value(0) }
            jsonPath("$.character.weapon.attackBonus") { value(2) }
            jsonPath("$.character.stats[0].total") { value(7) }
        }
        // 골드 부족 (다음 강화 200)
        mockMvc.post("/api/users/$userId/weapon/enhance").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `raid gives an equipped accessory`() {
        val userId = registerUser("rpg-raid")
        // 15km → EXP 750 → Lv5
        syncRun(userId, "r", "2026-10-07T06:00:00Z", "2026-10-07T08:00:00Z", 15000)

        attempt(userId, "RAID", 1).andExpect {
            status { isOk() }
            jsonPath("$.reward.accessory.grade") { value("COMMON") }
            jsonPath("$.reward.accessory.bonus") { value(3) }
            jsonPath("$.reward.accessory.equipped") { value(true) }
            jsonPath("$.character.accessories.length()") { value(1) }
        }
    }

    @Test
    fun `content list shows 20 difficulties with reasons`() {
        val userId = registerUser("rpg-list")
        mockMvc.get("/api/users/$userId/content").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(3) }
            jsonPath("$[0].type") { value("HUNT") }
            jsonPath("$[0].difficulties.length()") { value(20) }
            jsonPath("$[0].difficulties[0].available") { value(false) }
            jsonPath("$[2].difficulties[19].minLevel") { value(43) }
            jsonPath("$[2].difficulties[19].minGrade") { value("LEGENDARY") }
        }
    }

    private fun registerUser(deviceId: String): Long {
        val body = mockMvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"deviceId":"$deviceId","nickname":"tester"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return objectMapper.readTree(body).get("id").asLong()
    }

    private fun syncRun(userId: Long, id: String, start: String, end: String, meters: Int): ResultActionsDsl =
        mockMvc.post("/api/users/$userId/runs") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"runs":[{"externalId":"$id","startTime":"$start","endTime":"$end","distanceMeters":$meters}]}"""
        }.andExpect { status { isOk() } }

    private fun allocate(userId: Long, json: String): ResultActionsDsl =
        mockMvc.post("/api/users/$userId/stats") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }

    private fun attempt(userId: Long, type: String, difficulty: Int): ResultActionsDsl =
        mockMvc.post("/api/users/$userId/content/$type/$difficulty")
}
