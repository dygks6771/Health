package com.example.healthrpg.ranking

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.game.GameRules
import com.example.healthrpg.game.Job
import com.example.healthrpg.game.RankingRules
import com.example.healthrpg.hero.Hero
import com.example.healthrpg.hero.HeroRepository
import com.example.healthrpg.hero.HeroService
import com.example.healthrpg.item.ItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class RankingEntry(
    val rank: Int,
    val heroId: Long,
    val name: String,
    val job: Job,
    val jobName: String,
    val appearance: Int,
    val level: Int,
    val power: Int,
    val score: Long,
)

data class RankingResponse(
    /** 점수 = 레벨 × 100 + 전투력 */
    val formula: String,
    val totalHeroes: Int,
    val entries: List<RankingEntry>,
    /** 내 순위 (상위 목록 밖이어도 표시) */
    val me: RankingEntry?,
)

/** 가입한 모든 영웅의 랭킹 (테스트용 봇 제외) */
@Service
class RankingService(
    private val heroService: HeroService,
    private val heroRepository: HeroRepository,
    private val itemRepository: ItemRepository,
) {
    @Transactional(readOnly = true)
    fun ranking(accountId: Long, limit: Int): RankingResponse {
        val myHeroId = heroService.require(accountId).id
        val equipped = itemRepository.findAllByEquippedTrue().groupBy { it.hero.id }
        val scored = heroRepository.findAllByBotFalse().map { hero ->
            val power = GameRules.power(heroService.statsWith(hero, equipped[hero.id].orEmpty()), hero.level)
            hero to power
        }.sortedWith(
            compareByDescending<Pair<Hero, Int>> { RankingRules.score(it.first.level, it.second) }
                .thenBy { it.first.id },
        )

        val entries = scored.mapIndexed { i, (hero, power) ->
            RankingEntry(
                rank = i + 1,
                heroId = hero.id!!,
                name = hero.name,
                job = hero.job,
                jobName = hero.job.displayName,
                appearance = hero.appearance,
                level = hero.level,
                power = power,
                score = RankingRules.score(hero.level, power),
            )
        }
        return RankingResponse(
            formula = "레벨 × ${RankingRules.LEVEL_WEIGHT} + 전투력",
            totalHeroes = entries.size,
            entries = entries.take(limit.coerceIn(1, 200)),
            me = entries.firstOrNull { it.heroId == myHeroId },
        )
    }
}

@RestController
class RankingController(private val rankingService: RankingService) {
    @GetMapping("/api/ranking")
    fun ranking(@LoginAccount accountId: Long, @RequestParam(defaultValue = "100") limit: Int): RankingResponse =
        rankingService.ranking(accountId, limit)
}
