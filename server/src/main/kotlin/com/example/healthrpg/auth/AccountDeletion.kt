package com.example.healthrpg.auth

import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.common.UnauthorizedException
import com.example.healthrpg.friend.FriendService
import com.example.healthrpg.hero.HeroRepository
import jakarta.persistence.EntityManager
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class DeleteAccountRequest(@field:NotBlank val password: String)

/**
 * 회원 탈퇴 (스토어 정책: 앱에서 계정을 만들 수 있으면 앱에서 삭제도 가능해야 함).
 * 비밀번호를 한 번 더 확인한 뒤 계정과 영웅·장비·러닝 기록·친구·로그인 토큰을 모두 지운다.
 * 이 계정 전용으로 만든 용병 친구(봇)도 함께 삭제.
 */
@Service
class AccountDeletionService(
    private val accountRepository: AccountRepository,
    private val heroRepository: HeroRepository,
    private val friendService: FriendService,
    private val entityManager: EntityManager,
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    @Transactional
    fun delete(accountId: Long, password: String) {
        val account = accountRepository.findById(accountId).orElseThrow { UnauthorizedException() }
        if (!passwordEncoder.matches(password, account.passwordHash)) throw BadRequestException("비밀번호가 올바르지 않습니다")

        val botAccountIds = heroRepository.findByAccountId(accountId)
            ?.let { hero -> friendService.acceptedFriends(hero.id!!).filter { it.bot }.map { it.account.id!! } }
            .orEmpty()
        // 아래는 DB 에서 바로 지우므로, 이미 읽어 둔 엔티티가 다시 저장되지 않도록 비움
        entityManager.flush()
        entityManager.clear()
        botAccountIds.forEach { accountRepository.hardDelete(it) }
        accountRepository.hardDelete(accountId)
    }
}

@RestController
class AccountDeletionController(private val accountDeletionService: AccountDeletionService) {
    @PostMapping("/api/me/account/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@LoginAccount accountId: Long, @Valid @RequestBody request: DeleteAccountRequest) {
        accountDeletionService.delete(accountId, request.password)
    }
}
