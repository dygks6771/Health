package com.example.healthrpg

import com.example.healthrpg.game.GameRules
import com.example.healthrpg.game.LevelProgress
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRulesTest {

    @Test
    fun `100 steps per exp, capped per day`() {
        assertEquals(0, GameRules.expForSteps(99))
        assertEquals(100, GameRules.expForSteps(10_000))
        assertEquals(300, GameRules.expForSteps(30_000))
        assertEquals(300, GameRules.expForSteps(100_000))
    }

    @Test
    fun `level requirement grows by 50 per level`() {
        assertEquals(LevelProgress(level = 1, expInLevel = 0, expToNextLevel = 100), GameRules.progress(0))
        assertEquals(LevelProgress(level = 1, expInLevel = 99, expToNextLevel = 100), GameRules.progress(99))
        assertEquals(LevelProgress(level = 2, expInLevel = 0, expToNextLevel = 150), GameRules.progress(100))
        // 100 + 150 + 200 = 450 → Lv4 시작
        assertEquals(LevelProgress(level = 4, expInLevel = 10, expToNextLevel = 250), GameRules.progress(460))
    }
}
