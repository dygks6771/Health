package com.example.healthrpg.item

import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.common.NotFoundException
import com.example.healthrpg.game.ItemRules
import com.example.healthrpg.hero.HeroResponse
import com.example.healthrpg.hero.HeroService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

data class InventoryResponse(val items: List<ItemResponse>, val hero: HeroResponse)

data class EnhanceResultResponse(
    val success: Boolean,
    val rate: Int,
    val enhanceBefore: Int,
    val enhanceAfter: Int,
    val item: ItemResponse,
    val hero: HeroResponse,
)

/** 가방(장착·해제·판매)과 대장간(강화) */
@Service
@Transactional(readOnly = true)
class ItemService(
    private val heroService: HeroService,
    private val itemRepository: ItemRepository,
    private val random: Random,
) {
    fun inventory(accountId: Long): InventoryResponse {
        val hero = heroService.require(accountId)
        return InventoryResponse(
            itemRepository.findAllByHeroIdOrderByEquippedDescIdDesc(hero.id!!).map(ItemResponse::from),
            heroService.response(hero),
        )
    }

    /** 같은 부위의 다른 장비는 자동 해제 */
    @Transactional
    fun equip(accountId: Long, itemId: Long): InventoryResponse {
        val hero = heroService.requireForUpdate(accountId)
        val item = findOwned(hero.id!!, itemId)
        itemRepository.findAllByHeroIdAndEquippedTrue(hero.id!!)
            .filter { it.slot == item.slot && it.id != item.id }
            .forEach { it.equipped = false }
        item.equipped = true
        return inventory(accountId)
    }

    @Transactional
    fun unequip(accountId: Long, itemId: Long): InventoryResponse {
        val hero = heroService.requireForUpdate(accountId)
        findOwned(hero.id!!, itemId).equipped = false
        return inventory(accountId)
    }

    @Transactional
    fun sell(accountId: Long, itemId: Long): InventoryResponse {
        val hero = heroService.requireForUpdate(accountId)
        val item = findOwned(hero.id!!, itemId)
        if (item.equipped) throw BadRequestException("장착 중인 장비는 팔 수 없습니다")
        hero.gold += ItemRules.sellPrice(item.tier, item.gradeEnum, item.enhance)
        itemRepository.delete(item)
        return inventory(accountId)
    }

    /** 대장간 강화: 강화석과 골드를 쓰고 확률로 +1. 실패해도 단계는 유지 */
    @Transactional
    fun enhance(accountId: Long, itemId: Long): EnhanceResultResponse {
        val hero = heroService.requireForUpdate(accountId)
        val item = findOwned(hero.id!!, itemId)
        val cost = ItemRules.enhanceCost(item.tier, item.enhance)
            ?: throw BadRequestException("이미 최대 강화(+${ItemRules.MAX_ENHANCE})입니다")
        val rate = ItemRules.successRate(item.enhance)!!
        if (hero.enhanceStones < cost.stones) {
            throw BadRequestException("강화석이 부족합니다 (보유 ${hero.enhanceStones}, 필요 ${cost.stones})")
        }
        if (hero.gold < cost.gold) throw BadRequestException("골드가 부족합니다 (보유 ${hero.gold}, 필요 ${cost.gold})")

        hero.enhanceStones -= cost.stones
        hero.gold -= cost.gold
        val before = item.enhance
        val success = random.nextInt(100) < rate
        if (success) item.enhance += 1

        return EnhanceResultResponse(
            success = success,
            rate = rate,
            enhanceBefore = before,
            enhanceAfter = item.enhance,
            item = ItemResponse.from(item),
            hero = heroService.response(hero),
        )
    }

    private fun findOwned(heroId: Long, itemId: Long): Item =
        itemRepository.findByIdAndHeroId(itemId, heroId) ?: throw NotFoundException("장비를 찾을 수 없습니다")
}
