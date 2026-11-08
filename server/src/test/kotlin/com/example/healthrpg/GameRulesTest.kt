package com.example.healthrpg

import com.example.healthrpg.game.ChestTier
import com.example.healthrpg.game.ExpeditionRules
import com.example.healthrpg.game.GameRules
import com.example.healthrpg.game.Grade
import com.example.healthrpg.game.GuildRules
import com.example.healthrpg.game.HuntDifficulty
import com.example.healthrpg.game.ItemRules
import com.example.healthrpg.game.LevelProgress
import com.example.healthrpg.game.RaidBoss
import com.example.healthrpg.game.Region
import com.example.healthrpg.game.WeeklyRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameRulesTest {

    @Test
    fun `running rewards and levels`() {
        assertEquals(250, GameRules.rewardFor(5_000, GameRules.EXP_PER_KM))
        assertEquals(LevelProgress(level = 2, expInLevel = 0, expToNextLevel = 130), GameRules.progress(100))
        assertEquals(LevelProgress(level = 4, expInLevel = 70, expToNextLevel = 190), GameRules.progress(460))
    }

    @Test
    fun `regions get harder from beginner village to demon castle`() {
        assertEquals(10, Region.entries.size)
        assertEquals("초보자 마을", Region.entries.first().displayName)
        assertEquals(1, Region.BEGINNER_VILLAGE.dungeonMinLevel)
        assertEquals(46, Region.DEMON_CASTLE.dungeonMinLevel)
        Region.entries.zipWithNext().forEach { (a, b) ->
            assertTrue(b.dungeonMinLevel > a.dungeonMinLevel && b.dungeonMinTotalStats > a.dungeonMinTotalStats)
        }
        Region.entries.forEach { assertEquals(100, it.gradeWeights.sum(), it.name) }
        assertEquals(Region.WHISPERING_FOREST, Region.forLevel(7))
        assertEquals(Region.DEMON_CASTLE, Region.forLevel(99))
    }

    @Test
    fun `item values scale with tier and grade`() {
        assertEquals(6, ItemRules.mainValue(1, Grade.COMMON))
        assertEquals(73, ItemRules.mainValue(10, Grade.LEGENDARY))
        assertEquals(0, ItemRules.subValue(6, Grade.UNCOMMON))
        assertEquals(10, ItemRules.subValue(30, Grade.RARE))
        assertEquals(15, ItemRules.enhanced(10, 5))
        assertEquals(100, ItemRules.successRate(0))
        assertEquals(3, ItemRules.successRate(24))
        assertNull(ItemRules.successRate(25))
        assertNull(ItemRules.enhanceCost(1, 25))
        // 실패 시 +5 이상은 1단계만 하락
        assertEquals(listOf(0, 0, 1, 1, 1, 1), listOf(0, 4, 5, 10, 15, 24).map(ItemRules::failDrop))
        // 성공률은 단계가 오를수록 줄어듦
        assertTrue((0 until 24).all { ItemRules.successRate(it)!! >= ItemRules.successRate(it + 1)!! })
    }

    @Test
    fun `guild board has 2 easy, 2 normal, 1 hard`() {
        val targets = GuildRules.targetLevels(10)
        assertEquals(2, targets.count { it.first == HuntDifficulty.EASY && it.second < 10 })
        assertEquals(2, targets.count { it.first == HuntDifficulty.NORMAL && it.second == 10 })
        assertEquals(1, targets.count { it.first == HuntDifficulty.HARD && it.second > 10 })
        assertEquals(listOf(1, 1, 1, 1, 2), GuildRules.targetLevels(1).map { it.second })
    }

    @Test
    fun `win chances grow with power and can lose`() {
        assertEquals(0.7, com.example.healthrpg.game.BattleRules.huntWinChance(100, 100), 1e-9)
        assertEquals(1.0, com.example.healthrpg.game.BattleRules.huntWinChance(150, 100), 1e-9)
        assertEquals(0.1, com.example.healthrpg.game.BattleRules.huntWinChance(50, 100), 1e-9)
        assertEquals(0.6, com.example.healthrpg.game.BattleRules.dungeonWinChance(20, 20), 1e-9)
        assertEquals(1.0, com.example.healthrpg.game.BattleRules.dungeonWinChance(30, 20), 1e-9)
    }

    @Test
    fun `raid boss evolves in 4 stages and grades F to S`() {
        assertEquals(1, RaidBoss.stageFor(0).stage)
        assertEquals(2, RaidBoss.stageFor(3_000).stage)
        assertEquals(4, RaidBoss.stageFor(1_000_000).stage)
        assertEquals("F", RaidBoss.gradeFor(0).grade)
        assertEquals("C", RaidBoss.gradeFor(7_000).grade)
        assertEquals("S", RaidBoss.gradeFor(30_000).grade)
    }

    @Test
    fun `weekly goal follows my own recent weeks`() {
        assertEquals(5.0, WeeklyRules.goalKm(listOf(0.0, 0.0, 0.0, 0.0)))
        // 뛴 주(10, 8)의 평균 9km × 1.1 = 9.9 → 10.0
        assertEquals(10.0, WeeklyRules.goalKm(listOf(10.0, 0.0, 8.0, 0.0)))
        assertEquals(50.0, WeeklyRules.goalKm(listOf(100.0)))
        assertEquals(ChestTier.SILVER, WeeklyRules.chest(1))
        assertEquals(ChestTier.GOLD, WeeklyRules.chest(3))
    }

    @Test
    fun `over 10km a day gives half rewards and raid damage follows weekly km`() {
        assertEquals(5_000, GameRules.rewardMeters(5_000, alreadyToday = 0))
        assertEquals(8_000 + 2_000, GameRules.rewardMeters(12_000, alreadyToday = 2_000))
        assertEquals(10_000 + 1_000, GameRules.rewardMeters(12_000, alreadyToday = 0))
        assertEquals(0.5, RaidBoss.runBonus(0.0))
        assertEquals(1.5, RaidBoss.runBonus(25.0))
        assertEquals(0, ExpeditionRules.requiredKm(1))
        assertEquals(225, ExpeditionRules.requiredKm(10))
    }
}
