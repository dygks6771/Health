package com.example.healthrpg.game

import com.example.healthrpg.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "accessories")
class Accessory(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: AccessoryType,

    /** Grade.ordinal */
    @Column(nullable = false)
    val grade: Int,

    @Column(name = "stat_bonus", nullable = false)
    val statBonus: Int,

    @Column(nullable = false)
    var equipped: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "obtained_at", nullable = false)
    val obtainedAt: Instant = Instant.now()
}
