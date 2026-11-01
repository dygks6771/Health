package com.example.healthrpg.friend

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.common.ConflictException
import com.example.healthrpg.common.NotFoundException
import com.example.healthrpg.hero.Hero
import com.example.healthrpg.hero.HeroRepository
import com.example.healthrpg.hero.HeroService
import com.example.healthrpg.hero.HeroSummary
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class FriendRequest(@field:NotBlank val name: String)

data class FriendEntry(val friendshipId: Long, val hero: HeroSummary)

data class FriendsResponse(
    val friends: List<FriendEntry>,
    /** 나에게 온 요청 (수락 대기) */
    val incoming: List<FriendEntry>,
    /** 내가 보낸 요청 */
    val outgoing: List<FriendEntry>,
)

@Service
@Transactional(readOnly = true)
class FriendService(
    private val heroService: HeroService,
    private val heroRepository: HeroRepository,
    private val friendshipRepository: FriendshipRepository,
) {
    fun list(accountId: Long): FriendsResponse {
        val me = heroService.require(accountId).id!!
        val all = friendshipRepository.findAllInvolving(me)
        fun entry(f: Friendship) = FriendEntry(f.id!!, heroService.summary(f.other(me)))
        return FriendsResponse(
            friends = all.filter { it.status == FriendshipStatus.ACCEPTED }.map(::entry),
            incoming = all.filter { it.status == FriendshipStatus.PENDING && it.addressee.id == me }.map(::entry),
            outgoing = all.filter { it.status == FriendshipStatus.PENDING && it.requester.id == me }.map(::entry),
        )
    }

    /** 영웅 이름으로 친구 요청. 상대가 이미 나에게 요청했다면 바로 친구가 됨 */
    @Transactional
    fun request(accountId: Long, name: String): FriendsResponse {
        val me = heroService.require(accountId)
        val target = heroRepository.findByName(name.trim()) ?: throw NotFoundException("'${name.trim()}' 영웅을 찾을 수 없습니다")
        if (target.id == me.id) throw BadRequestException("자기 자신에게는 요청할 수 없습니다")

        val existing = friendshipRepository.findBetween(me.id!!, target.id!!)
        when {
            existing == null -> friendshipRepository.save(Friendship(me, target))
            existing.status == FriendshipStatus.ACCEPTED -> throw ConflictException("이미 친구입니다")
            existing.requester.id == target.id -> existing.status = FriendshipStatus.ACCEPTED
            else -> throw ConflictException("이미 요청을 보냈습니다")
        }
        return list(accountId)
    }

    @Transactional
    fun accept(accountId: Long, friendshipId: Long): FriendsResponse {
        val me = heroService.require(accountId).id!!
        val friendship = friendshipRepository.findById(friendshipId).orElse(null)
        if (friendship == null || friendship.addressee.id != me) throw NotFoundException("친구 요청을 찾을 수 없습니다")
        friendship.status = FriendshipStatus.ACCEPTED
        return list(accountId)
    }

    /** 친구 삭제, 요청 거절·취소 */
    @Transactional
    fun remove(accountId: Long, friendshipId: Long): FriendsResponse {
        val me = heroService.require(accountId).id!!
        val friendship = friendshipRepository.findById(friendshipId).orElse(null)
        if (friendship == null || (friendship.requester.id != me && friendship.addressee.id != me)) {
            throw NotFoundException("친구를 찾을 수 없습니다")
        }
        friendshipRepository.delete(friendship)
        return list(accountId)
    }

    fun acceptedFriends(heroId: Long): List<Hero> =
        friendshipRepository.findAllInvolving(heroId)
            .filter { it.status == FriendshipStatus.ACCEPTED }
            .map { it.other(heroId) }
}

@RestController
class FriendController(private val friendService: FriendService) {
    @GetMapping("/api/me/friends")
    fun list(@LoginAccount accountId: Long): FriendsResponse = friendService.list(accountId)

    @PostMapping("/api/me/friends")
    fun request(@LoginAccount accountId: Long, @Valid @RequestBody request: FriendRequest): FriendsResponse =
        friendService.request(accountId, request.name)

    @PostMapping("/api/me/friends/{friendshipId}/accept")
    fun accept(@LoginAccount accountId: Long, @PathVariable friendshipId: Long): FriendsResponse =
        friendService.accept(accountId, friendshipId)

    @DeleteMapping("/api/me/friends/{friendshipId}")
    fun remove(@LoginAccount accountId: Long, @PathVariable friendshipId: Long): FriendsResponse =
        friendService.remove(accountId, friendshipId)
}
