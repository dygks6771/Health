package com.example.healthrpg.run

import com.example.healthrpg.hero.Hero
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
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.time.LocalDate

/** Health Connect 러닝 기록 1건과 그 보상 */
@Entity
@Table(name = "run_sessions")
class RunSession(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_id", nullable = false)
    val hero: Hero,

    @Column(name = "external_id", nullable = false, length = 200)
    val externalId: String,

    @Column(name = "run_date", nullable = false)
    val runDate: LocalDate,

    @Column(name = "start_time", nullable = false)
    val startTime: Instant,

    @Column(name = "end_time", nullable = false)
    val endTime: Instant,

    @Column(name = "distance_meters", nullable = false)
    val distanceMeters: Int,

    @Column(name = "counted_meters", nullable = false)
    val countedMeters: Int,

    @Column(name = "exp_awarded", nullable = false)
    val expAwarded: Int,

    @Column(name = "gold_awarded", nullable = false)
    val goldAwarded: Int,

    @Column(name = "stamina_awarded", nullable = false)
    val staminaAwarded: Int,

    /** 보상하지 않은 이유 (속도 초과, 하루 상한 등). 정상이면 null */
    @Column(name = "rejected_reason", length = 100)
    val rejectedReason: String?,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "synced_at", nullable = false)
    val syncedAt: Instant = Instant.now()
}

interface RunSessionRepository : JpaRepository<RunSession, Long> {
    fun existsByHeroIdAndExternalId(heroId: Long, externalId: String): Boolean

    @Query(
        "select coalesce(sum(r.countedMeters), 0) from RunSession r " +
            "where r.hero.id = :heroId and r.runDate between :from and :to",
    )
    fun sumCountedMeters(heroId: Long, from: LocalDate, to: LocalDate): Long

    @Query(
        "select coalesce(sum(r.distanceMeters), 0) from RunSession r " +
            "where r.hero.id = :heroId and r.runDate between :from and :to",
    )
    fun sumDistanceMeters(heroId: Long, from: LocalDate, to: LocalDate): Long
}
