package com.example.healthrpg.steps

import com.example.healthrpg.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "daily_steps",
    uniqueConstraints = [UniqueConstraint(name = "uk_daily_steps_user_date", columnNames = ["user_id", "step_date"])],
)
class DailySteps(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "step_date", nullable = false)
    val date: LocalDate,

    @Column(nullable = false)
    var steps: Long,

    @Column(length = 100)
    var source: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    /** 이 날 걸음수로 이미 지급한 경험치 (중복 지급 방지) */
    @Column(name = "exp_awarded", nullable = false)
    var expAwarded: Int = 0

    @Column(name = "synced_at", nullable = false)
    var syncedAt: Instant = Instant.now()
}
