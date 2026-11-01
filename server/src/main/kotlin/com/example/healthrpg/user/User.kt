package com.example.healthrpg.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
class User(
    @Column(name = "device_id", nullable = false, unique = true, length = 100)
    val deviceId: String,

    @Column(nullable = false, length = 30)
    var nickname: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(nullable = false)
    var level: Int = 1

    /** 누적 경험치. 레벨은 이 값으로 계산 (GameRules) */
    @Column(nullable = false)
    var exp: Long = 0

    @Column(nullable = false)
    var gold: Long = 0

    /** 컨텐츠 입장에 쓰는 행동력. 러닝으로 충전 */
    @Column(nullable = false)
    var stamina: Int = 0

    /** 아직 찍지 않은 스탯 포인트. 레벨업마다 지급 */
    @Column(name = "stat_points", nullable = false)
    var statPoints: Int = 0

    /** 찍은 스탯 (기본값 포함, 장비 보너스 제외) */
    @Column(nullable = false)
    var attack: Int = 5

    @Column(nullable = false)
    var defense: Int = 5

    @Column(nullable = false)
    var agility: Int = 5

    @Column(nullable = false)
    var spirit: Int = 5

    @Column(name = "weapon_enhance", nullable = false)
    var weaponEnhance: Int = 0

    /** 던전 보상: 무기 강화 재료 */
    @Column(name = "enhance_stones", nullable = false)
    var enhanceStones: Int = 0

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

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
