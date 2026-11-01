package com.example.healthrpg.auth

import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.common.ConflictException
import com.example.healthrpg.common.UnauthorizedException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Service
@Transactional(readOnly = true)
class AuthService(
    private val accountRepository: AccountRepository,
    private val tokenRepository: AuthTokenRepository,
) {
    private val passwordEncoder = BCryptPasswordEncoder()
    private val random = SecureRandom()

    @Transactional
    fun signup(loginId: String, password: String): Account {
        val id = loginId.trim().lowercase()
        if (!LOGIN_ID_PATTERN.matches(id)) throw BadRequestException("아이디는 영문 소문자·숫자·_ 4~20자입니다")
        if (password.length !in 8..64) throw BadRequestException("비밀번호는 8~64자입니다")
        if (accountRepository.existsByLoginId(id)) throw ConflictException("이미 사용 중인 아이디입니다")
        return accountRepository.save(Account(id, passwordEncoder.encode(password)!!))
    }

    /** 성공하면 토큰 원문을 돌려준다 (앱이 저장해 두고 Authorization: Bearer 로 보냄) */
    @Transactional
    fun login(loginId: String, password: String): String {
        val account = accountRepository.findByLoginId(loginId.trim().lowercase())
        if (account == null || !passwordEncoder.matches(password, account.passwordHash)) {
            throw UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다")
        }
        return issueToken(account)
    }

    @Transactional
    fun issueToken(account: Account): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        tokenRepository.save(AuthToken(account, hash(token), Instant.now().plus(TOKEN_LIFETIME)))
        return token
    }

    @Transactional
    fun logout(token: String) {
        tokenRepository.deleteByTokenHash(hash(token))
    }

    /** 토큰 → 계정 ID. 없거나 만료면 401 */
    fun accountIdFor(token: String): Long {
        val saved = tokenRepository.findByTokenHash(hash(token)) ?: throw UnauthorizedException()
        if (saved.expiresAt.isBefore(Instant.now())) throw UnauthorizedException("로그인이 만료되었습니다")
        return saved.account.id!!
    }

    private fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        private val LOGIN_ID_PATTERN = Regex("^[a-z0-9_]{4,20}$")
        private val TOKEN_LIFETIME: Duration = Duration.ofDays(30)
    }
}
