package com.example.healthrpg.guild

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.common.NotFoundException
import com.example.healthrpg.game.BattleRules
import com.example.healthrpg.game.ChestResponse
import com.example.healthrpg.game.ChestTier
import com.example.healthrpg.game.GameRules
import com.example.healthrpg.game.GuildRules
import com.example.healthrpg.game.HuntDifficulty
import com.example.healthrpg.game.Region
import com.example.healthrpg.hero.Hero
import com.example.healthrpg.hero.HeroResponse
import com.example.healthrpg.hero.HeroService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import kotlin.random.Random

data class HuntResponse(
    val slot: Int,
    val difficulty: HuntDifficulty,
    val difficultyName: String,
    val monster: String,
    val regionName: String,
    val targetLevel: Int,
    val requiredPower: Int,
    val rewardGold: Int,
    val staminaCost: Int,
    /** 승률 % (질 수도 있음) */
    val winChance: Int,
    val cleared: Boolean,
    val available: Boolean,
    val reasons: List<String>,
)

data class GuildBoardResponse(val date: LocalDate, val power: Int, val hunts: List<HuntResponse>)

data class HuntResultResponse(
    val monster: String,
    val victory: Boolean,
    val winChance: Int,
    /** 승리 시 상자 보상 (패배면 0 / null) */
    val rewardGold: Int,
    val rewardStones: Int,
    val chest: ChestResponse?,
    val hero: HeroResponse,
)

/**
 * 모험자 길드: 매일 바뀌는 토벌 의뢰 5개.
 * 그날 처음 게시판을 볼 때 영웅 레벨 기준으로 쉬움 2, 적정 2, 도전 1 을 무작위 몬스터로 생성.
 */
@Service
class GuildService(
    private val heroService: HeroService,
    private val dailyHuntRepository: DailyHuntRepository,
    private val random: Random,
) {
    @Transactional
    fun board(accountId: Long): GuildBoardResponse {
        val hero = heroService.requireForUpdate(accountId)
        val power = heroService.power(hero)
        val hunts = huntsFor(hero, heroService.today())
        return GuildBoardResponse(heroService.today(), power, hunts.map { toResponse(hero, power, it) })
    }

    @Transactional
    fun hunt(accountId: Long, slot: Int): HuntResultResponse {
        val hero = heroService.requireForUpdate(accountId)
        val hunt = huntsFor(hero, heroService.today()).firstOrNull { it.slot == slot }
            ?: throw NotFoundException("의뢰를 찾을 수 없습니다")
        val reasons = missing(hero, heroService.power(hero), hunt)
        if (reasons.isNotEmpty()) throw BadRequestException(reasons.joinToString(", "))

        hero.stamina -= GuildRules.HUNT_STAMINA_COST
        val chance = BattleRules.huntWinChance(heroService.power(hero), hunt.requiredPower)
        val victory = random.nextDouble() < chance
        if (!victory) {
            // 패배: 행동력만 쓰고 보상 없음. 의뢰는 남아 있어 다시 도전 가능
            return HuntResultResponse(hunt.monster, false, BattleRules.percent(chance), 0, 0, null, heroService.response(hero))
        }

        val chest = ChestTier.roll(random)
        val gold = (hunt.rewardGold * chest.multiplier * (0.9 + random.nextDouble() * 0.2)).toInt()
        hero.gold += gold
        hero.enhanceStones += chest.bonusStones
        hunt.cleared = true
        return HuntResultResponse(
            hunt.monster, true, BattleRules.percent(chance), gold, chest.bonusStones, ChestResponse.of(chest),
            heroService.response(hero),
        )
    }

    private fun huntsFor(hero: Hero, date: LocalDate): List<DailyHunt> {
        val existing = dailyHuntRepository.findAllByHeroIdAndBoardDateOrderBySlot(hero.id!!, date)
        if (existing.isNotEmpty()) return existing

        return GuildRules.targetLevels(hero.level).shuffled(random).mapIndexed { slot, (difficulty, level) ->
            val region = Region.forLevel(level)
            dailyHuntRepository.save(
                DailyHunt(
                    hero = hero,
                    boardDate = date,
                    slot = slot,
                    difficulty = difficulty,
                    monster = region.monsters[random.nextInt(region.monsters.size)],
                    region = region.tier,
                    targetLevel = level,
                    requiredPower = GameRules.recommendedPower(level),
                    rewardGold = GuildRules.rewardGold(level, difficulty),
                ),
            )
        }
    }

    private fun missing(hero: Hero, power: Int, hunt: DailyHunt): List<String> = buildList {
        if (hunt.cleared) add("오늘 이미 완료한 의뢰")
        val minimum = (hunt.requiredPower * BattleRules.HUNT_MIN_RATIO).toInt()
        if (power < minimum) add("전투력 최소 $minimum 필요")
        if (hero.stamina < GuildRules.HUNT_STAMINA_COST) add("행동력 ${GuildRules.HUNT_STAMINA_COST} 필요")
    }

    private fun toResponse(hero: Hero, power: Int, hunt: DailyHunt): HuntResponse {
        val reasons = missing(hero, power, hunt)
        return HuntResponse(
            slot = hunt.slot,
            difficulty = hunt.difficulty,
            difficultyName = hunt.difficulty.displayName,
            monster = hunt.monster,
            regionName = Region.byTier(hunt.region)?.displayName ?: "",
            targetLevel = hunt.targetLevel,
            requiredPower = hunt.requiredPower,
            rewardGold = hunt.rewardGold,
            staminaCost = GuildRules.HUNT_STAMINA_COST,
            winChance = BattleRules.percent(BattleRules.huntWinChance(power, hunt.requiredPower)),
            cleared = hunt.cleared,
            available = reasons.isEmpty(),
            reasons = reasons,
        )
    }
}

@RestController
class GuildController(private val guildService: GuildService) {
    /** 오늘의 토벌 의뢰 5개 */
    @GetMapping("/api/me/guild/hunts")
    fun board(@LoginAccount accountId: Long): GuildBoardResponse = guildService.board(accountId)

    @PostMapping("/api/me/guild/hunts/{slot}")
    fun hunt(@LoginAccount accountId: Long, @PathVariable slot: Int): HuntResultResponse =
        guildService.hunt(accountId, slot)
}
