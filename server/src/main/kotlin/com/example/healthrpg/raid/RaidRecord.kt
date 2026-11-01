package com.example.healthrpg.raid

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
import java.time.Instant

@Entity
@Table(name = "raid_records")
class RaidRecord(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_id", nullable = false)
    val hero: Hero,

    @Column(name = "total_damage", nullable = false)
    val totalDamage: Long,

    @Column(name = "final_stage", nullable = false)
    val finalStage: Int,

    @Column(nullable = false, length = 2)
    val grade: String,

    /** 파티원 이름 (쉼표 구분) */
    @Column(nullable = false, length = 200)
    val party: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
}

interface RaidRecordRepository : JpaRepository<RaidRecord, Long> {
    fun findFirstByHeroIdOrderByTotalDamageDesc(heroId: Long): RaidRecord?
}
