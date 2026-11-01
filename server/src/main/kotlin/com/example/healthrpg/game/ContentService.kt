package com.example.healthrpg.game

import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.user.User
import com.example.healthrpg.user.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

/** 토벌 / 던전 / 레이드 입장 조건 확인과 보상 */
@Service
@Transactional(readOnly = true)
class ContentService(
    private val userService: UserService,
    private val characterService: CharacterService,
    private val accessoryRepository: AccessoryRepository,
    private val contentClearRepository: ContentClearRepository,
    private val random: Random,
) {
    fun list(userId: Long): List<ContentResponse> {
        val user = userService.get(userId)
        val stats = characterService.totalStats(user)
        return ContentType.entries.map { type ->
            ContentResponse(
                type = type,
                name = type.displayName,
                staminaCost = type.staminaCost,
                difficulties = ContentType.DIFFICULTIES.map { d ->
                    val req = type.requirement(d)
                    val reasons = missing(user, stats, type, d)
                    DifficultyResponse(
                        difficulty = d,
                        minLevel = req.minLevel,
                        minGrade = req.minGrade,
                        minGradeName = req.minGrade.displayName,
                        minStats = req.minStats.map { (stat, value) -> StatValue(stat, stat.displayName, value) },
                        reward = type.rewardDescription(d),
                        available = reasons.isEmpty(),
                        reasons = reasons,
                    )
                },
            )
        }
    }

    /**
     * 조건을 모두 만족하면 행동력을 쓰고 클리어 + 보상.
     * 지금은 조건만 맞으면 항상 성공 (전투 확률은 추후 추가).
     */
    @Transactional
    fun attempt(userId: Long, type: ContentType, difficulty: Int): ContentResultResponse {
        if (difficulty !in ContentType.DIFFICULTIES) throw BadRequestException("난이도는 1~20 단계입니다")
        val user = userService.getForUpdate(userId)
        val reasons = missing(user, characterService.totalStats(user), type, difficulty)
        if (reasons.isNotEmpty()) throw BadRequestException(reasons.joinToString(", "))

        user.stamina -= type.staminaCost
        contentClearRepository.save(ContentClear(user, type, difficulty))

        val reward = when (type) {
            ContentType.HUNT -> {
                val gold = ContentType.huntGold(difficulty)
                user.gold += gold
                ContentRewardResponse(gold = gold, enhanceStones = 0, accessory = null)
            }
            ContentType.DUNGEON -> {
                val stones = ContentType.dungeonStones(difficulty)
                user.enhanceStones += stones
                ContentRewardResponse(gold = 0, enhanceStones = stones, accessory = null)
            }
            ContentType.RAID -> ContentRewardResponse(gold = 0, enhanceStones = 0, accessory = dropAccessory(user, difficulty))
        }

        return ContentResultResponse(
            content = "${type.displayName} ${difficulty}단계",
            reward = reward,
            character = characterService.character(userId),
        )
    }

    private fun missing(user: User, stats: Map<Stat, Int>, type: ContentType, difficulty: Int): List<String> {
        val req = type.requirement(difficulty)
        val reasons = mutableListOf<String>()
        if (user.level < req.minLevel) reasons += "레벨 ${req.minLevel} 필요"
        val weaponGrade = WeaponRules.grade(user.weaponEnhance)
        if (weaponGrade < req.minGrade) reasons += "${req.minGrade.displayName} 등급 무기 필요"
        req.minStats.forEach { (stat, value) ->
            if ((stats[stat] ?: 0) < value) reasons += "${stat.displayName} $value 필요"
        }
        if (user.stamina < type.staminaCost) reasons += "행동력 ${type.staminaCost} 필요"
        return reasons
    }

    /** 레이드 난이도 등급의 장신구 1개. 같은 종류 장착품보다 좋으면 자동으로 바꿔 낌 */
    private fun dropAccessory(user: User, difficulty: Int): AccessoryResponse {
        val type = AccessoryType.entries[random.nextInt(AccessoryType.entries.size)]
        val grade = Grade.forDifficulty(difficulty)
        val bonus = AccessoryType.bonusFor(grade)

        val current = accessoryRepository.findAllByUserIdOrderByIdDesc(user.id!!)
            .firstOrNull { it.type == type && it.equipped }
        val equip = current == null || bonus > current.statBonus
        if (equip) current?.equipped = false

        val saved = accessoryRepository.save(Accessory(user, type, grade.ordinal, bonus, equipped = equip))
        return AccessoryResponse.from(saved)
    }
}
