package com.example.healthrpg.world

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
@Table(name = "dungeon_clears")
class DungeonClear(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_id", nullable = false)
    val hero: Hero,

    /** Region 티어 1~10 */
    @Column(nullable = false)
    val region: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "cleared_at", nullable = false)
    val clearedAt: Instant = Instant.now()
}

interface DungeonClearRepository : JpaRepository<DungeonClear, Long> {
    fun countByHeroIdAndRegion(heroId: Long, region: Int): Long
}
