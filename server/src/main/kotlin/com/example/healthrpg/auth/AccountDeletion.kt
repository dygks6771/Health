package com.example.healthrpg.auth

import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.common.UnauthorizedException
import com.example.healthrpg.friend.FriendService
import com.example.healthrpg.hero.HeroRepository
import jakarta.persistence.EntityManager
import jakarta.servlet.http.HttpServletRequest
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

    /** 웹 페이지(앱 없이)에서 아이디·비밀번호로 탈퇴 */
    @Transactional
    fun deleteByLogin(loginId: String, password: String) {
        val account = accountRepository.findByLoginId(loginId.trim().lowercase())
            ?: throw UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다")
        if (!passwordEncoder.matches(password, account.passwordHash)) throw UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다")
        delete(account.id!!, password)
    }

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
class AccountDeletionController(
    private val accountDeletionService: AccountDeletionService,
    private val limits: AuthLimits,
) {
    /** 앱 설정 → 회원 탈퇴 */
    @PostMapping("/api/me/account/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@LoginAccount accountId: Long, @Valid @RequestBody request: DeleteAccountRequest) {
        accountDeletionService.delete(accountId, request.password)
    }

    /** 웹 탈퇴 페이지(/delete-account.html)용. 앱을 지운 사용자도 탈퇴할 수 있어야 함 (스토어 정책). 로그인과 같은 시도 제한 */
    @PostMapping("/api/auth/delete-account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteByLogin(@Valid @RequestBody request: AuthRequest, http: HttpServletRequest) {
        val loginId = request.loginId.trim().lowercase()
        limits.loginPerId.check(loginId, limits::waitMessage)
        limits.loginPerIp.check(http.remoteAddr, limits::waitMessage)
        try {
            accountDeletionService.deleteByLogin(loginId, request.password)
        } catch (e: UnauthorizedException) {
            limits.loginPerId.record(loginId)
            limits.loginPerIp.record(http.remoteAddr)
            throw e
        }
    }
}
