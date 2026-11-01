package com.example.healthrpg

import com.example.healthrpg.game.ContentType
import com.example.healthrpg.game.GameRules
import com.example.healthrpg.game.Grade
import com.example.healthrpg.game.LevelProgress
import com.example.healthrpg.game.Stat
import com.example.healthrpg.game.WeaponRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameRulesTest {

    @Test
    fun `running rewards per km`() {
        assertEquals(250, GameRules.rewardFor(5_000, GameRules.EXP_PER_KM))
        assertEquals(100, GameRules.rewardFor(5_000, GameRules.GOLD_PER_KM))
        assertEquals(50, GameRules.rewardFor(5_000, GameRules.STAMINA_PER_KM))
        assertEquals(0, GameRules.rewardFor(15, GameRules.EXP_PER_KM))
    }

    @Test
    fun `level requirement grows by 50 per level`() {
        assertEquals(LevelProgress(level = 1, expInLevel = 0, expToNextLevel = 100), GameRules.progress(0))
        assertEquals(LevelProgress(level = 2, expInLevel = 0, expToNextLevel = 150), GameRules.progress(100))
        // 100 + 150 + 200 = 450 → Lv4 시작
        assertEquals(LevelProgress(level = 4, expInLevel = 10, expToNextLevel = 250), GameRules.progress(460))
    }

    @Test
    fun `grades by difficulty and weapon enhance`() {
        assertEquals(Grade.COMMON, Grade.forDifficulty(4))
        assertEquals(Grade.UNCOMMON, Grade.forDifficulty(5))
        assertEquals(Grade.LEGENDARY, Grade.forDifficulty(20))
        assertEquals(Grade.COMMON, WeaponRules.grade(3))
        assertEquals(Grade.UNCOMMON, WeaponRules.grade(4))
        assertEquals(Grade.LEGENDARY, WeaponRules.grade(20))
        assertNull(WeaponRules.nextCost(20))
    }

    @Test
    fun `content requirements scale with difficulty`() {
        val hunt = ContentType.HUNT.requirement(1)
        assertEquals(1, hunt.minLevel)
        assertEquals(mapOf(Stat.ATTACK to 5, Stat.AGILITY to 5), hunt.minStats)

        val dungeon = ContentType.DUNGEON.requirement(10)
        assertEquals(21, dungeon.minLevel)
        assertEquals(Grade.RARE, dungeon.minGrade)
        assertEquals(mapOf(Stat.ATTACK to 23, Stat.DEFENSE to 23), dungeon.minStats)

        val raid = ContentType.RAID.requirement(20)
        assertEquals(43, raid.minLevel)
        assertEquals(Grade.LEGENDARY, raid.minGrade)
        assertEquals(Stat.entries.associateWith { 24 }, raid.minStats)
    }
}
