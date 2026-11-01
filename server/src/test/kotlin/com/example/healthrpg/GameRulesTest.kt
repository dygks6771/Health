package com.example.healthrpg

import com.example.healthrpg.game.GameRules
import com.example.healthrpg.game.Grade
import com.example.healthrpg.game.GuildRules
import com.example.healthrpg.game.HuntDifficulty
import com.example.healthrpg.game.ItemRules
import com.example.healthrpg.game.LevelProgress
import com.example.healthrpg.game.RaidBoss
import com.example.healthrpg.game.Region
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameRulesTest {

    @Test
    fun `running rewards and levels`() {
        assertEquals(250, GameRules.rewardFor(5_000, GameRules.EXP_PER_KM))
        assertEquals(LevelProgress(level = 2, expInLevel = 0, expToNextLevel = 150), GameRules.progress(100))
        assertEquals(LevelProgress(level = 4, expInLevel = 10, expToNextLevel = 250), GameRules.progress(460))
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
        assertNull(ItemRules.successRate(15))
        assertNull(ItemRules.enhanceCost(1, 15))
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
    fun `raid boss evolves in 4 stages and grades F to S`() {
        assertEquals(1, RaidBoss.stageFor(0).stage)
        assertEquals(2, RaidBoss.stageFor(3_000).stage)
        assertEquals(4, RaidBoss.stageFor(1_000_000).stage)
        assertEquals("F", RaidBoss.gradeFor(0).grade)
        assertEquals("C", RaidBoss.gradeFor(7_000).grade)
        assertEquals("S", RaidBoss.gradeFor(30_000).grade)
    }
}
