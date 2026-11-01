package com.example.healthrpg.hero

import com.example.healthrpg.auth.Account
import com.example.healthrpg.game.Job
import com.example.healthrpg.game.Stat
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.LockModeType
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.Instant

/** 계정당 1명의 영웅 (플레이어 캐릭터) */
@Entity
@Table(name = "heroes")
class Hero(
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    val account: Account,

    @Column(nullable = false, unique = true, length = 12)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val job: Job,

    @Column(nullable = false)
    val appearance: Int,

    /** 테스트용 친구 봇 */
    @Column(name = "is_bot", nullable = false)
    val bot: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(nullable = false)
    var level: Int = 1

    @Column(nullable = false)
    var exp: Long = 0

    @Column(nullable = false)
    var gold: Long = 0

    /** 던전·토벌·레이드 입장에 쓰는 행동력. 러닝으로 충전 */
    @Column(nullable = false)
    var stamina: Int = 0

    @Column(name = "stat_points", nullable = false)
    var statPoints: Int = 0

    /** 찍은 능력치 (기본값·직업 보너스 포함, 장비 제외) */
    @Column(nullable = false)
    var attack: Int = 0

    @Column(nullable = false)
    var defense: Int = 0

    @Column(nullable = false)
    var agility: Int = 0

    @Column(nullable = false)
    var spirit: Int = 0

    /** 대장간 강화 재료 */
    @Column(name = "enhance_stones", nullable = false)
    var enhanceStones: Int = 0

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    fun baseStat(stat: Stat): Int = when (stat) {
        Stat.ATTACK -> attack
        Stat.DEFENSE -> defense
        Stat.AGILITY -> agility
        Stat.SPIRIT -> spirit
    }

    fun addStat(stat: Stat, amount: Int) {
        when (stat) {
            Stat.ATTACK -> attack += amount
            Stat.DEFENSE -> defense += amount
            Stat.AGILITY -> agility += amount
            Stat.SPIRIT -> spirit += amount
        }
    }

    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

interface HeroRepository : JpaRepository<Hero, Long> {
    fun findByAccountId(accountId: Long): Hero?

    fun findByName(name: String): Hero?

    fun existsByName(name: String): Boolean

    /** 대소문자만 다른 이름(Arthur / arthur)도 같은 이름으로 취급 */
    fun existsByNameIgnoreCase(name: String): Boolean

    fun findAllByBotFalse(): List<Hero>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hero h where h.id = :id")
    fun findByIdForUpdate(id: Long): Hero?

    /** 보상·재화가 오가는 요청끼리 겹쳐서 중복 지급되지 않도록 행 잠금 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hero h where h.account.id = :accountId")
    fun findByAccountIdForUpdate(accountId: Long): Hero?
}
