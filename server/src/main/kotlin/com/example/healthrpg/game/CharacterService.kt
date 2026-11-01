package com.example.healthrpg.game

import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.run.RunSessionRepository
import com.example.healthrpg.user.User
import com.example.healthrpg.user.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** 캐릭터 조회, 경험치·레벨업, 스탯 찍기, 무기 강화 */
@Service
@Transactional(readOnly = true)
class CharacterService(
    private val userService: UserService,
    private val accessoryRepository: AccessoryRepository,
    private val runSessionRepository: RunSessionRepository,
    @Value("\${game.zone:Asia/Seoul}") zone: String,
) {
    val zoneId: ZoneId = ZoneId.of(zone)

    fun today(): LocalDate = LocalDate.now(zoneId)

    /** 경험치를 더하고 레벨이 오르면 스탯 포인트 지급. 반환: (이전 레벨, 이후 레벨) */
    fun addExp(user: User, exp: Long): Pair<Int, Int> {
        val before = user.level
        user.exp += exp
        user.level = GameRules.progress(user.exp).level
        user.statPoints += GameRules.STAT_POINTS_PER_LEVEL * (user.level - before)
        return before to user.level
    }

    /** 찍은 스탯 + 무기 + 장착 장신구 */
    fun totalStats(user: User): Map<Stat, Int> {
        val bonus = bonusStats(user)
        return Stat.entries.associateWith { baseStat(user, it) + (bonus[it] ?: 0) }
    }

    fun character(userId: Long): CharacterResponse {
        val user = userService.get(userId)
        val progress = GameRules.progress(user.exp)
        val bonus = bonusStats(user)
        val today = today()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val nextCost = WeaponRules.nextCost(user.weaponEnhance)
        val weaponGrade = WeaponRules.grade(user.weaponEnhance)

        return CharacterResponse(
            id = userId,
            nickname = user.nickname,
            level = progress.level,
            totalExp = user.exp,
            expInLevel = progress.expInLevel,
            expToNextLevel = progress.expToNextLevel,
            gold = user.gold,
            stamina = user.stamina,
            maxStamina = GameRules.MAX_STAMINA,
            statPoints = user.statPoints,
            stats = Stat.entries.map {
                val base = baseStat(user, it)
                val b = bonus[it] ?: 0
                StatResponse(it, it.displayName, base, b, base + b)
            },
            weapon = WeaponResponse(
                enhance = user.weaponEnhance,
                grade = weaponGrade,
                gradeName = weaponGrade.displayName,
                attackBonus = WeaponRules.attackBonus(user.weaponEnhance),
                maxEnhance = WeaponRules.MAX_ENHANCE,
                nextCostStones = nextCost?.stones,
                nextCostGold = nextCost?.gold,
            ),
            enhanceStones = user.enhanceStones,
            accessories = accessoryRepository.findAllByUserIdOrderByIdDesc(userId).map(AccessoryResponse::from),
            todayKm = runSessionRepository.sumDistanceMeters(userId, today, today) / 1000.0,
            weekKm = runSessionRepository.sumDistanceMeters(userId, weekStart, today) / 1000.0,
        )
    }

    @Transactional
    fun allocate(userId: Long, request: AllocateStatsRequest): CharacterResponse {
        val user = userService.getForUpdate(userId)
        val total = request.attack + request.defense + request.agility + request.spirit
        if (total == 0) throw BadRequestException("찍을 스탯을 1 이상 입력하세요")
        if (total > user.statPoints) throw BadRequestException("스탯 포인트가 부족합니다 (보유 ${user.statPoints}, 필요 $total)")

        user.attack += request.attack
        user.defense += request.defense
        user.agility += request.agility
        user.spirit += request.spirit
        user.statPoints -= total
        return character(userId)
    }

    @Transactional
    fun enhanceWeapon(userId: Long): EnhanceResponse {
        val user = userService.getForUpdate(userId)
        val cost = WeaponRules.nextCost(user.weaponEnhance)
            ?: throw BadRequestException("이미 최대 강화(+${WeaponRules.MAX_ENHANCE})입니다")
        if (user.enhanceStones < cost.stones) {
            throw BadRequestException("강화석이 부족합니다 (보유 ${user.enhanceStones}, 필요 ${cost.stones})")
        }
        if (user.gold < cost.gold) throw BadRequestException("골드가 부족합니다 (보유 ${user.gold}, 필요 ${cost.gold})")

        user.enhanceStones -= cost.stones
        user.gold -= cost.gold
        user.weaponEnhance += 1
        return EnhanceResponse(user.weaponEnhance, character(userId))
    }

    private fun baseStat(user: User, stat: Stat): Int = when (stat) {
        Stat.ATTACK -> user.attack
        Stat.DEFENSE -> user.defense
        Stat.AGILITY -> user.agility
        Stat.SPIRIT -> user.spirit
    }

    private fun bonusStats(user: User): Map<Stat, Int> {
        val bonus = mutableMapOf(Stat.ATTACK to WeaponRules.attackBonus(user.weaponEnhance))
        accessoryRepository.findAllByUserIdOrderByIdDesc(user.id!!)
            .filter { it.equipped }
            .forEach { bonus.merge(it.type.stat, it.statBonus, Int::plus) }
        return bonus
    }
}
