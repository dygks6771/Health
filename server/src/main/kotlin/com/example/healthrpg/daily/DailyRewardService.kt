package com.example.healthrpg.daily

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.game.ChestResponse
import com.example.healthrpg.game.ChestTier
import com.example.healthrpg.game.DailyRewardRules
import com.example.healthrpg.game.Region
import com.example.healthrpg.hero.Hero
import com.example.healthrpg.hero.HeroResponse
import com.example.healthrpg.hero.HeroService
import com.example.healthrpg.item.ItemFactory
import com.example.healthrpg.item.ItemResponse
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

@Entity
@Table(name = "daily_rewards")
class DailyReward(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_id", nullable = false)
    val hero: Hero,

    @Column(name = "reward_date", nullable = false)
    val rewardDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val chest: ChestTier,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "claimed_at", nullable = false)
    val claimedAt: Instant = Instant.now()
}

interface DailyRewardRepository : JpaRepository<DailyReward, Long> {
    fun existsByHeroIdAndRewardDate(heroId: Long, rewardDate: LocalDate): Boolean
}

data class DailyStatusResponse(val claimedToday: Boolean)

data class DailyClaimResponse(
    val chest: ChestResponse,
    val gold: Long,
    val stones: Int,
    /** 황금 상자면 장비 */
    val item: ItemResponse?,
    val hero: HeroResponse,
)

/** 출석 보상: 하루 한 번 상자 (나무 / 은 / 황금). 보상은 내 레벨 지역 티어에 비례 */
@Service
class DailyRewardService(
    private val heroService: HeroService,
    private val dailyRewardRepository: DailyRewardRepository,
    private val itemFactory: ItemFactory,
    private val random: Random,
) {
    @Transactional(readOnly = true)
    fun status(accountId: Long): DailyStatusResponse {
        val hero = heroService.require(accountId)
        return DailyStatusResponse(dailyRewardRepository.existsByHeroIdAndRewardDate(hero.id!!, heroService.today()))
    }

    @Transactional
    fun claim(accountId: Long): DailyClaimResponse {
        val hero = heroService.requireForUpdate(accountId)
        val today = heroService.today()
        if (dailyRewardRepository.existsByHeroIdAndRewardDate(hero.id!!, today)) {
            throw BadRequestException("오늘 출석 보상은 이미 받았습니다. 내일 다시 오세요!")
        }
        val chest = ChestTier.roll(random)
        val region = Region.forLevel(hero.level)
        val gold = DailyRewardRules.gold(region.tier, chest)
        val stones = DailyRewardRules.stones(chest)
        hero.gold += gold
        hero.enhanceStones += stones
        val item = if (chest == ChestTier.GOLD) itemFactory.dungeonDrop(hero, region) else null
        dailyRewardRepository.save(DailyReward(hero, today, chest))
        return DailyClaimResponse(ChestResponse.of(chest), gold, stones, item?.let(ItemResponse::from), heroService.response(hero))
    }
}

@RestController
class DailyRewardController(private val dailyRewardService: DailyRewardService) {
    @GetMapping("/api/me/daily")
    fun status(@LoginAccount accountId: Long): DailyStatusResponse = dailyRewardService.status(accountId)

    @PostMapping("/api/me/daily/claim")
    fun claim(@LoginAccount accountId: Long): DailyClaimResponse = dailyRewardService.claim(accountId)
}
