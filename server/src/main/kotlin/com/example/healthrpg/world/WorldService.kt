package com.example.healthrpg.world

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.common.NotFoundException
import com.example.healthrpg.game.BattleRules
import com.example.healthrpg.game.ChestResponse
import com.example.healthrpg.game.ChestTier
import com.example.healthrpg.game.Grade
import com.example.healthrpg.game.Region
import com.example.healthrpg.hero.Hero
import com.example.healthrpg.hero.HeroResponse
import com.example.healthrpg.hero.HeroService
import com.example.healthrpg.item.ItemFactory
import com.example.healthrpg.item.ItemResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random

data class DungeonResponse(
    val name: String,
    val boss: String,
    val minLevel: Int,
    val minTotalStats: Int,
    val staminaCost: Int,
    val rewardStones: Int,
    val rewardGold: Long,
    val dropRate: Int,
    /** 예: "일반 70% · 고급 25% · 희귀 5%" */
    val dropGrades: String,
    val clears: Long,
    /** 입장 가능할 때 승률 % */
    val winChance: Int,
    val available: Boolean,
    val reasons: List<String>,
)

data class RegionResponse(
    val tier: Int,
    val name: String,
    val description: String,
    val dungeon: DungeonResponse,
)

data class WorldResponse(val totalStats: Int, val regions: List<RegionResponse>)

data class DungeonResultResponse(
    val dungeon: String,
    val boss: String,
    val victory: Boolean,
    val winChance: Int,
    val chest: ChestResponse?,
    val rewardStones: Int,
    val rewardGold: Long,
    val item: ItemResponse?,
    val hero: HeroResponse,
)

/** 세계 지도와 지역 던전 */
@Service
class WorldService(
    private val heroService: HeroService,
    private val dungeonClearRepository: DungeonClearRepository,
    private val itemFactory: ItemFactory,
    private val random: Random,
) {
    @Transactional(readOnly = true)
    fun world(accountId: Long): WorldResponse {
        val hero = heroService.require(accountId)
        val total = heroService.stats(hero).values.sum()
        return WorldResponse(
            totalStats = total,
            regions = Region.entries.map { region ->
                val reasons = missing(hero, total, region)
                RegionResponse(
                    tier = region.tier,
                    name = region.displayName,
                    description = region.description,
                    dungeon = DungeonResponse(
                        name = region.dungeonName,
                        boss = region.dungeonBoss,
                        minLevel = region.dungeonMinLevel,
                        minTotalStats = region.dungeonMinTotalStats,
                        staminaCost = region.dungeonStaminaCost,
                        rewardStones = region.dungeonStones,
                        rewardGold = region.dungeonGold,
                        dropRate = Region.DROP_RATE,
                        dropGrades = region.gradeWeights.withIndex()
                            .filter { it.value > 0 }
                            .joinToString(" · ") { "${Grade.entries[it.index].displayName} ${it.value}%" },
                        clears = dungeonClearRepository.countByHeroIdAndRegion(hero.id!!, region.tier),
                        winChance = BattleRules.percent(BattleRules.dungeonWinChance(total, region.dungeonMinTotalStats)),
                        available = reasons.isEmpty(),
                        reasons = reasons,
                    ),
                )
            },
        )
    }

    /** 던전 공략: 조건을 만족하면 클리어, 강화석·골드 + 확률적으로 장비 */
    @Transactional
    fun enterDungeon(accountId: Long, tier: Int): DungeonResultResponse {
        val region = Region.byTier(tier) ?: throw NotFoundException("없는 지역입니다")
        val hero = heroService.requireForUpdate(accountId)
        val total = heroService.stats(hero).values.sum()
        val reasons = missing(hero, total, region)
        if (reasons.isNotEmpty()) throw BadRequestException(reasons.joinToString(", "))

        hero.stamina -= region.dungeonStaminaCost
        val chance = BattleRules.dungeonWinChance(total, region.dungeonMinTotalStats)
        if (random.nextDouble() >= chance) {
            // 패배: 행동력만 소모
            return DungeonResultResponse(
                dungeon = region.dungeonName, boss = region.dungeonBoss, victory = false,
                winChance = BattleRules.percent(chance), chest = null, rewardStones = 0, rewardGold = 0, item = null,
                hero = heroService.response(hero),
            )
        }

        val chest = ChestTier.roll(random)
        val stones = (region.dungeonStones * chest.multiplier).toInt() + chest.bonusStones
        val gold = (region.dungeonGold * chest.multiplier).toLong()
        hero.enhanceStones += stones
        hero.gold += gold
        dungeonClearRepository.save(DungeonClear(hero, region.tier))
        val item = if (random.nextInt(100) < chest.itemChance) itemFactory.dungeonDrop(hero, region) else null

        return DungeonResultResponse(
            dungeon = region.dungeonName,
            boss = region.dungeonBoss,
            victory = true,
            winChance = BattleRules.percent(chance),
            chest = ChestResponse.of(chest),
            rewardStones = stones,
            rewardGold = gold,
            item = item?.let(ItemResponse::from),
            hero = heroService.response(hero),
        )
    }

    private fun missing(hero: Hero, totalStats: Int, region: Region): List<String> = buildList {
        if (hero.level < region.dungeonMinLevel) add("레벨 ${region.dungeonMinLevel} 필요")
        if (totalStats < region.dungeonMinTotalStats) add("능력치 합계 ${region.dungeonMinTotalStats} 필요")
        if (hero.stamina < region.dungeonStaminaCost) add("행동력 ${region.dungeonStaminaCost} 필요")
    }
}

@RestController
class WorldController(private val worldService: WorldService) {
    /** 10개 지역과 던전 입장 조건 */
    @GetMapping("/api/me/world")
    fun world(@LoginAccount accountId: Long): WorldResponse = worldService.world(accountId)

    @PostMapping("/api/me/world/{tier}/dungeon")
    fun enter(@LoginAccount accountId: Long, @PathVariable tier: Int): DungeonResultResponse =
        worldService.enterDungeon(accountId, tier)
}
