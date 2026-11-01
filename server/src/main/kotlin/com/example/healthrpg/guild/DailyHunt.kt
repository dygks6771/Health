package com.example.healthrpg.guild

import com.example.healthrpg.game.HuntDifficulty
import com.example.healthrpg.hero.Hero
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
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

/** 모험자 길드 일일 토벌 의뢰 */
@Entity
@Table(name = "daily_hunts")
class DailyHunt(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_id", nullable = false)
    val hero: Hero,

    @Column(name = "board_date", nullable = false)
    val boardDate: LocalDate,

    @Column(nullable = false)
    val slot: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val difficulty: HuntDifficulty,

    @Column(nullable = false, length = 50)
    val monster: String,

    /** Region 티어 1~10 */
    @Column(nullable = false)
    val region: Int,

    @Column(name = "target_level", nullable = false)
    val targetLevel: Int,

    @Column(name = "required_power", nullable = false)
    val requiredPower: Int,

    @Column(name = "reward_gold", nullable = false)
    val rewardGold: Int,

    @Column(nullable = false)
    var cleared: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}

interface DailyHuntRepository : JpaRepository<DailyHunt, Long> {
    fun findAllByHeroIdAndBoardDateOrderBySlot(heroId: Long, boardDate: LocalDate): List<DailyHunt>
}
