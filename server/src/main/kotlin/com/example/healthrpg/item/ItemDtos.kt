package com.example.healthrpg.item

import com.example.healthrpg.game.Grade
import com.example.healthrpg.game.ItemRules
import com.example.healthrpg.game.ItemSlot
import com.example.healthrpg.game.Stat

data class ItemResponse(
    val id: Long,
    val slot: ItemSlot,
    val slotName: String,
    val name: String,
    val grade: Grade,
    val gradeName: String,
    val tier: Int,
    val stat: Stat,
    val statName: String,
    /** 강화 반영된 주 능력치 */
    val value: Int,
    val subStat: Stat?,
    val subStatName: String?,
    val subValue: Int,
    val enhance: Int,
    val equipped: Boolean,
    /** 경매장 등록 중 */
    val listed: Boolean,
    val sellPrice: Long,
    /** 다음 강화 성공률(%)과 비용. 최대 강화면 null */
    val nextEnhanceRate: Int?,
    val nextEnhanceStones: Int?,
    val nextEnhanceGold: Long?,
    /** 다음 강화가 실패하면 떨어지는 단계 수 */
    val failDrop: Int,
) {
    companion object {
        fun from(item: Item): ItemResponse {
            val grade = item.gradeEnum
            val cost = ItemRules.enhanceCost(item.tier, item.enhance)
            return ItemResponse(
                id = item.id!!,
                slot = item.slot,
                slotName = item.slot.displayName,
                name = item.name,
                grade = grade,
                gradeName = grade.displayName,
                tier = item.tier,
                stat = item.slot.stat,
                statName = item.slot.stat.displayName,
                value = ItemRules.enhanced(item.mainValue, item.enhance),
                subStat = item.subStat,
                subStatName = item.subStat?.displayName,
                subValue = ItemRules.enhanced(item.subValue, item.enhance),
                enhance = item.enhance,
                equipped = item.equipped,
                listed = item.listed,
                sellPrice = ItemRules.sellPrice(item.tier, grade, item.enhance),
                nextEnhanceRate = ItemRules.successRate(item.enhance),
                nextEnhanceStones = cost?.stones,
                nextEnhanceGold = cost?.gold,
                failDrop = ItemRules.failDrop(item.enhance),
            )
        }
    }
}
