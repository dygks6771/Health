package com.example.healthrpg.item

import com.example.healthrpg.game.Grade
import com.example.healthrpg.game.ItemRules
import com.example.healthrpg.game.ItemSlot
import com.example.healthrpg.game.Region
import com.example.healthrpg.game.Stat
import com.example.healthrpg.hero.Hero
import org.springframework.stereotype.Component
import kotlin.random.Random

/** 장비 생성. 같은 부위를 비워 두고 있으면 새 장비를 바로 장착 */
@Component
class ItemFactory(
    private val itemRepository: ItemRepository,
    private val random: Random,
) {
    fun starterWeapon(hero: Hero): Item = create(hero, ItemSlot.WEAPON, tier = 1, Grade.COMMON, prefix = "견습생의")

    /** 던전 드롭: 부위는 무작위, 등급은 지역별 확률 */
    fun dungeonDrop(hero: Hero, region: Region): Item {
        val slot = ItemSlot.entries[random.nextInt(ItemSlot.entries.size)]
        return create(hero, slot, region.tier, rollGrade(region.gradeWeights), region.itemPrefix)
    }

    fun raidReward(hero: Hero, tier: Int, grade: Grade): Item =
        create(hero, ItemSlot.ACCESSORY, tier, grade, prefix = "마룡의")

    fun create(hero: Hero, slot: ItemSlot, tier: Int, grade: Grade, prefix: String): Item {
        val main = ItemRules.mainValue(tier, grade)
        val sub = ItemRules.subValue(main, grade)
        val subStat = if (sub > 0) Stat.entries.filter { it != slot.stat }.let { it[random.nextInt(it.size)] } else null
        val noun = if (slot == ItemSlot.WEAPON) hero.job.weaponName else slot.displayName
        val equip = itemRepository.findAllByHeroIdAndEquippedTrue(hero.id!!).none { it.slot == slot }
        return itemRepository.save(
            Item(
                hero = hero,
                slot = slot,
                name = "$prefix $noun",
                grade = grade.ordinal,
                tier = tier,
                mainValue = main,
                subStat = subStat,
                subValue = sub,
                equipped = equip,
            ),
        )
    }

    private fun rollGrade(weights: List<Int>): Grade {
        var roll = random.nextInt(weights.sum())
        weights.forEachIndexed { i, w ->
            if (roll < w) return Grade.entries[i]
            roll -= w
        }
        return Grade.COMMON
    }
}
