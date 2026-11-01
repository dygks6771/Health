package com.example.healthrpg.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

@Entity
@Table(name = "accounts")
class Account(
    @Column(name = "login_id", nullable = false, unique = true, length = 20)
    val loginId: String,

    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
}

@Entity
@Table(name = "auth_tokens")
class AuthToken(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

    /** 토큰 원문의 SHA-256 (DB 가 유출돼도 토큰을 쓸 수 없게) */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
}

interface AccountRepository : JpaRepository<Account, Long> {
    fun findByLoginId(loginId: String): Account?

    fun existsByLoginId(loginId: String): Boolean
}

interface AuthTokenRepository : JpaRepository<AuthToken, Long> {
    @Query("select t from AuthToken t join fetch t.account where t.tokenHash = :tokenHash")
    fun findByTokenHash(tokenHash: String): AuthToken?

    @Modifying
    @Query("delete from AuthToken t where t.tokenHash = :tokenHash")
    fun deleteByTokenHash(tokenHash: String): Int
}
