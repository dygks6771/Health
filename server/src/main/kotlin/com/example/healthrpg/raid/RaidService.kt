package com.example.healthrpg.raid

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.friend.FriendService
import com.example.healthrpg.game.Job
import com.example.healthrpg.game.RaidBoss
import com.example.healthrpg.game.RaidGrade
import com.example.healthrpg.game.RaidStage
import com.example.healthrpg.game.Stat
import com.example.healthrpg.hero.HeroResponse
import com.example.healthrpg.hero.HeroService
import com.example.healthrpg.hero.HeroSummary
import com.example.healthrpg.item.ItemFactory
import com.example.healthrpg.item.ItemResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

data class RaidInfoResponse(
    val bossName: String,
    val partySize: Int,
    val turns: Int,
    val staminaCost: Int,
    val stages: List<RaidStage>,
    val grades: List<RaidGrade>,
    /** 파티에 넣을 수 있는 친구 */
    val friends: List<HeroSummary>,
    val bestGrade: String?,
    val bestDamage: Long?,
    /** 이번 주 남은 도전 횟수 (주 1회, 월요일 0시 초기화) */
    val remainingThisWeek: Int,
    val resetsAt: Instant,
)

data class RaidAttackRequest(
    /** 함께할 친구 영웅 ID 4명 */
    @field:Size(min = 4, max = 4)
    val friendHeroIds: List<Long>,
)

data class RaidTurn(
    val turn: Int,
    val damage: Long,
    val totalDamage: Long,
    val stage: Int,
    val stageName: String,
    /** 이번 턴에 보스가 진화했는지 */
    val evolved: Boolean,
)

data class RaidMemberResult(
    val name: String,
    val job: Job,
    val jobName: String,
    val appearance: Int,
    val damage: Long,
)

data class RaidResultResponse(
    val totalDamage: Long,
    val finalStage: Int,
    val finalStageName: String,
    val grade: String,
    val turns: List<RaidTurn>,
    val members: List<RaidMemberResult>,
    val rewardGold: Long,
    val rewardStones: Int,
    val item: ItemResponse?,
    val hero: HeroResponse,
)

/**
 * 레이드: 본인 + 친구 4명. 10턴 동안 매 턴 5명이 한 번씩 공격.
 * 누적 데미지가 기준을 넘을 때마다 보스가 진화(최대 4단계)하며 받는 데미지가 줄어든다.
 * 총 데미지로 F~S 등급과 보상이 정해짐. 친구 영웅은 지금 능력치로 함께 싸움(친구가 접속해 있을 필요 없음).
 */
@Service
class RaidService(
    private val heroService: HeroService,
    private val friendService: FriendService,
    private val raidRecordRepository: RaidRecordRepository,
    private val itemFactory: ItemFactory,
    private val random: Random,
) {
    @Transactional(readOnly = true)
    fun info(accountId: Long): RaidInfoResponse {
        val hero = heroService.require(accountId)
        val best = raidRecordRepository.findFirstByHeroIdOrderByTotalDamageDesc(hero.id!!)
        return RaidInfoResponse(
            bossName = RaidBoss.NAME,
            partySize = RaidBoss.PARTY_SIZE,
            turns = RaidBoss.TURNS,
            staminaCost = RaidBoss.STAMINA_COST,
            stages = RaidBoss.stages,
            grades = RaidBoss.grades,
            friends = friendService.acceptedFriends(hero.id!!).map(heroService::summary).sortedByDescending { it.power },
            bestGrade = best?.grade,
            bestDamage = best?.totalDamage,
            remainingThisWeek = remaining(hero.id!!),
            resetsAt = weekStart().plus(Duration.ofDays(7)),
        )
    }

    @Transactional
    fun attack(accountId: Long, request: RaidAttackRequest): RaidResultResponse {
        val hero = heroService.requireForUpdate(accountId)
        val ids = request.friendHeroIds.distinct()
        if (ids.size != RaidBoss.PARTY_SIZE - 1) throw BadRequestException("서로 다른 친구 4명을 골라 주세요")
        val friends = friendService.acceptedFriends(hero.id!!).associateBy { it.id!! }
        val party = listOf(hero) + ids.map { friends[it] ?: throw BadRequestException("친구가 아닌 영웅이 있습니다") }
        if (remaining(hero.id!!) <= 0) throw BadRequestException("레이드는 주 ${RaidBoss.WEEKLY_LIMIT}회입니다. 월요일 0시에 초기화됩니다")
        if (hero.stamina < RaidBoss.STAMINA_COST) throw BadRequestException("행동력 ${RaidBoss.STAMINA_COST} 필요")
        hero.stamina -= RaidBoss.STAMINA_COST

        val partyStats = party.map { heroService.stats(it) }
        val buff = RaidBoss.spiritBuff(partyStats.sumOf { it[Stat.SPIRIT] ?: 0 })
        val memberDamage = LongArray(party.size)
        val turns = mutableListOf<RaidTurn>()
        var total = 0L
        var stage = RaidBoss.stages.first()

        for (turn in 1..RaidBoss.TURNS) {
            var turnDamage = 0L
            val stageBefore = stage
            party.forEachIndexed { i, member ->
                val stats = partyStats[i]
                var hit = RaidBoss.baseHit(stats, member.job, member.level) * buff * stage.damageRate
                if (random.nextDouble() < RaidBoss.critChance(stats)) hit *= 1.5
                val damage = hit.toLong()
                memberDamage[i] += damage
                turnDamage += damage
                total += damage
                stage = RaidBoss.stageFor(total)
            }
            turns += RaidTurn(turn, turnDamage, total, stage.stage, stage.name, evolved = stage != stageBefore)
        }

        val grade = RaidBoss.gradeFor(total)
        hero.gold += grade.gold
        hero.enhanceStones += grade.stones
        val tier = (party.map { it.level }.average().toInt() / 5 + 1).coerceIn(1, 10)
        val item = grade.itemGrade?.let { itemFactory.raidReward(hero, tier, it) }
        raidRecordRepository.save(RaidRecord(hero, total, stage.stage, grade.grade, party.joinToString(",") { it.name }))

        return RaidResultResponse(
            totalDamage = total,
            finalStage = stage.stage,
            finalStageName = stage.name,
            grade = grade.grade,
            turns = turns,
            members = party.mapIndexed { i, m -> RaidMemberResult(m.name, m.job, m.job.displayName, m.appearance, memberDamage[i]) },
            rewardGold = grade.gold,
            rewardStones = grade.stones,
            item = item?.let(ItemResponse::from),
            hero = heroService.response(hero),
        )
    }

    /** 이번 주 월요일 0시 (게임 시간대 기준) */
    fun weekStart(): Instant =
        heroService.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(heroService.zoneId).toInstant()

    private fun remaining(heroId: Long): Int =
        (RaidBoss.WEEKLY_LIMIT - raidRecordRepository.countByHeroIdAndCreatedAtGreaterThanEqual(heroId, weekStart()).toInt())
            .coerceAtLeast(0)
}

@RestController
class RaidController(private val raidService: RaidService) {
    @GetMapping("/api/me/raid")
    fun info(@LoginAccount accountId: Long): RaidInfoResponse = raidService.info(accountId)

    @PostMapping("/api/me/raid")
    fun attack(@LoginAccount accountId: Long, @Valid @RequestBody request: RaidAttackRequest) =
        raidService.attack(accountId, request)
}
