package com.example.healthrpg.hero

import com.example.healthrpg.auth.Account
import com.example.healthrpg.auth.AccountRepository
import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.common.ConflictException
import com.example.healthrpg.common.NotFoundException
import com.example.healthrpg.game.Appearances
import com.example.healthrpg.game.GameRules
import com.example.healthrpg.game.Job
import com.example.healthrpg.game.Stat
import com.example.healthrpg.item.ItemFactory
import com.example.healthrpg.item.ItemRepository
import com.example.healthrpg.item.ItemResponse
import com.example.healthrpg.run.RunSessionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@Service
@Transactional(readOnly = true)
class HeroService(
    private val heroRepository: HeroRepository,
    private val accountRepository: AccountRepository,
    private val itemRepository: ItemRepository,
    private val itemFactory: ItemFactory,
    private val runSessionRepository: RunSessionRepository,
    @Value("\${game.zone:Asia/Seoul}") zone: String,
) {
    val zoneId: ZoneId = ZoneId.of(zone)

    fun today(): LocalDate = LocalDate.now(zoneId)

    fun require(accountId: Long): Hero =
        heroRepository.findByAccountId(accountId) ?: throw NotFoundException("영웅을 먼저 만들어 주세요", "HERO_NOT_FOUND")

    /** 재화·보상이 바뀌는 요청에서 사용. 같은 영웅의 요청이 겹치지 않도록 잠금 */
    fun requireForUpdate(accountId: Long): Hero =
        heroRepository.findByAccountIdForUpdate(accountId)
            ?: throw NotFoundException("영웅을 먼저 만들어 주세요", "HERO_NOT_FOUND")

    @Transactional
    fun create(accountId: Long, request: CreateHeroRequest): HeroResponse {
        if (heroRepository.findByAccountId(accountId) != null) throw ConflictException("이미 영웅이 있습니다")
        val hero = createHero(accountRepository.getReferenceById(accountId), request.name, request.job, request.appearance)
        return response(hero)
    }

    /** 영웅 생성: 기본 능력치 5 + 직업 보너스, 견습생 무기 지급 */
    @Transactional
    fun createHero(account: Account, rawName: String, job: Job, appearance: Int, bot: Boolean = false): Hero {
        val name = rawName.trim()
        if (!NAME_PATTERN.matches(name)) throw BadRequestException("이름은 한글·영문·숫자 2~12자입니다")
        if (appearance !in Appearances.names.indices) throw BadRequestException("없는 외형입니다")
        if (heroRepository.existsByName(name)) throw ConflictException("이미 있는 이름입니다")

        val hero = Hero(account, name, job, appearance, bot)
        Stat.entries.forEach { hero.addStat(it, GameRules.BASE_STAT) }
        hero.addStat(job.primary, 3)
        hero.addStat(job.secondary, 2)
        heroRepository.save(hero)
        itemFactory.starterWeapon(hero)
        return hero
    }

    /** 경험치를 더하고 레벨업마다 스탯 포인트 3 + 직업 주 능력치 +1. 반환: (이전 레벨, 이후 레벨) */
    fun addExp(hero: Hero, exp: Long): Pair<Int, Int> {
        val before = hero.level
        hero.exp += exp
        hero.level = GameRules.progress(hero.exp).level
        val gained = hero.level - before
        hero.statPoints += GameRules.STAT_POINTS_PER_LEVEL * gained
        hero.addStat(hero.job.primary, gained)
        return before to hero.level
    }

    /** 찍은 능력치 + 장착 장비 */
    fun stats(hero: Hero): Map<Stat, Int> {
        val bonus = equipmentBonus(hero.id!!)
        return Stat.entries.associateWith { hero.baseStat(it) + (bonus[it] ?: 0) }
    }

    fun power(hero: Hero): Int = GameRules.power(stats(hero), hero.level)

    fun summary(hero: Hero) = HeroSummary(
        id = hero.id!!,
        name = hero.name,
        job = hero.job,
        jobName = hero.job.displayName,
        appearance = hero.appearance,
        level = hero.level,
        power = power(hero),
    )

    fun response(hero: Hero): HeroResponse {
        val heroId = hero.id!!
        val progress = GameRules.progress(hero.exp)
        val bonus = equipmentBonus(heroId)
        val stats = Stat.entries.associateWith { hero.baseStat(it) + (bonus[it] ?: 0) }
        val today = today()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return HeroResponse(
            id = heroId,
            name = hero.name,
            job = hero.job,
            jobName = hero.job.displayName,
            jobRole = hero.job.role,
            appearance = hero.appearance,
            level = progress.level,
            totalExp = hero.exp,
            expInLevel = progress.expInLevel,
            expToNextLevel = progress.expToNextLevel,
            gold = hero.gold,
            stamina = hero.stamina,
            maxStamina = GameRules.MAX_STAMINA,
            statPoints = hero.statPoints,
            enhanceStones = hero.enhanceStones,
            power = GameRules.power(stats, hero.level),
            stats = Stat.entries.map {
                StatResponse(it, it.displayName, hero.baseStat(it), bonus[it] ?: 0, stats.getValue(it))
            },
            equipment = itemRepository.findAllByHeroIdAndEquippedTrue(heroId).map(ItemResponse::from),
            todayKm = runSessionRepository.sumDistanceMeters(heroId, today, today) / 1000.0,
            weekKm = runSessionRepository.sumDistanceMeters(heroId, weekStart, today) / 1000.0,
        )
    }

    @Transactional
    fun allocate(accountId: Long, request: AllocateStatsRequest): HeroResponse {
        val hero = requireForUpdate(accountId)
        val total = request.attack + request.defense + request.agility + request.spirit
        if (total == 0) throw BadRequestException("찍을 능력치를 1 이상 입력하세요")
        if (total > hero.statPoints) throw BadRequestException("스탯 포인트가 부족합니다 (보유 ${hero.statPoints}, 필요 $total)")
        hero.addStat(Stat.ATTACK, request.attack)
        hero.addStat(Stat.DEFENSE, request.defense)
        hero.addStat(Stat.AGILITY, request.agility)
        hero.addStat(Stat.SPIRIT, request.spirit)
        hero.statPoints -= total
        return response(hero)
    }

    private fun equipmentBonus(heroId: Long): Map<Stat, Int> {
        val bonus = mutableMapOf<Stat, Int>()
        itemRepository.findAllByHeroIdAndEquippedTrue(heroId).forEach { item ->
            item.bonuses().forEach { (stat, value) -> bonus.merge(stat, value, Int::plus) }
        }
        return bonus
    }

    companion object {
        private val NAME_PATTERN = Regex("^[가-힣a-zA-Z0-9]{2,12}$")
    }
}
