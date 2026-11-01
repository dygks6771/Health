package com.example.healthrpg.dev

import com.example.healthrpg.auth.Account
import com.example.healthrpg.auth.AccountRepository
import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.friend.FriendService
import com.example.healthrpg.friend.Friendship
import com.example.healthrpg.friend.FriendshipRepository
import com.example.healthrpg.friend.FriendshipStatus
import com.example.healthrpg.friend.FriendsResponse
import com.example.healthrpg.game.GameRules
import com.example.healthrpg.game.Job
import com.example.healthrpg.hero.HeroRepository
import com.example.healthrpg.hero.HeroService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.random.Random

/**
 * 개발·테스트 전용. 레이드에 친구 4명이 필요하므로 내 레벨의 친구 봇을 만들어 준다.
 * game.dev-tools.enabled=false 면 이 API 자체가 등록되지 않음 (실서비스에서는 반드시 끌 것).
 */
@RestController
@ConditionalOnProperty(name = ["game.dev-tools.enabled"], havingValue = "true")
class DevController(
    private val heroService: HeroService,
    private val heroRepository: HeroRepository,
    private val accountRepository: AccountRepository,
    private val friendService: FriendService,
    private val friendshipRepository: FriendshipRepository,
    private val random: Random,
) {
    /** 친구가 4명이 될 때까지 내 레벨의 봇 영웅을 만들어 친구로 추가 */
    @PostMapping("/api/dev/friend-bots")
    @Transactional
    fun friendBots(@LoginAccount accountId: Long): FriendsResponse {
        val me = heroService.require(accountId)
        val need = 4 - friendService.acceptedFriends(me.id!!).size
        repeat(need.coerceAtLeast(0)) { i ->
            val account = accountRepository.save(Account("bot_" + UUID.randomUUID().toString().take(12).replace("-", ""), "!"))
            var name: String
            do {
                name = "용병" + (1000 + random.nextInt(9000))
            } while (heroRepository.existsByName(name))
            val job = Job.entries[(me.id!!.toInt() + i) % Job.entries.size]
            val bot = heroService.createHero(account, name, job, appearance = random.nextInt(6), bot = true)

            // 내 레벨까지 성장시키고 포인트는 직업 주 능력치에 투자
            val expForLevel = (1 until me.level).sumOf { GameRules.expToNextLevel(it) }
            heroService.addExp(bot, expForLevel)
            bot.addStat(job.primary, bot.statPoints)
            bot.statPoints = 0
            friendshipRepository.save(Friendship(me, bot, FriendshipStatus.ACCEPTED))
        }
        return friendService.list(accountId)
    }
}
