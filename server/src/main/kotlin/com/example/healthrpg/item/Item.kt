package com.example.healthrpg.item

import com.example.healthrpg.game.Grade
import com.example.healthrpg.game.ItemRules
import com.example.healthrpg.game.ItemSlot
import com.example.healthrpg.game.Stat
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
import java.time.Instant

@Entity
@Table(name = "items")
class Item(
    /** 경매장에서 팔리면 구매자로 바뀜 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_id", nullable = false)
    var hero: Hero,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val slot: ItemSlot,

    @Column(nullable = false, length = 50)
    val name: String,

    /** Grade.ordinal */
    @Column(nullable = false)
    val grade: Int,

    /** 얻은 지역 티어 1~10 */
    @Column(nullable = false)
    val tier: Int,

    /** 강화 전 주 능력치 (부위별 능력치) */
    @Column(name = "main_value", nullable = false)
    val mainValue: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_stat", length = 20)
    val subStat: Stat?,

    @Column(name = "sub_value", nullable = false)
    val subValue: Int,

    @Column(nullable = false)
    var enhance: Int = 0,

    @Column(nullable = false)
    var equipped: Boolean = false,

    /** 경매장에 등록 중 */
    @Column(nullable = false)
    var listed: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "obtained_at", nullable = false)
    val obtainedAt: Instant = Instant.now()

    val gradeEnum: Grade get() = Grade.entries[grade]

    /** 강화가 반영된 능력치 */
    fun bonuses(): Map<Stat, Int> = buildMap {
        put(slot.stat, ItemRules.enhanced(mainValue, enhance))
        subStat?.let { if (subValue > 0) merge(it, ItemRules.enhanced(subValue, enhance), Int::plus) }
    }
}

interface ItemRepository : JpaRepository<Item, Long> {
    /** 랭킹 계산용: 모든 영웅의 장착 장비를 한 번에 */
    fun findAllByEquippedTrue(): List<Item>

    fun findAllByHeroIdOrderByEquippedDescIdDesc(heroId: Long): List<Item>

    fun findAllByHeroIdAndEquippedTrue(heroId: Long): List<Item>

    fun findByIdAndHeroId(id: Long, heroId: Long): Item?
}
