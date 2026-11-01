package com.example.healthrpg.item

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.game.Grade
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/me")
class ItemController(
    private val itemService: ItemService,
) {
    @GetMapping("/items")
    fun inventory(@LoginAccount accountId: Long): InventoryResponse = itemService.inventory(accountId)

    @PostMapping("/items/{itemId}/equip")
    fun equip(@LoginAccount accountId: Long, @PathVariable itemId: Long) = itemService.equip(accountId, itemId)

    @PostMapping("/items/{itemId}/unequip")
    fun unequip(@LoginAccount accountId: Long, @PathVariable itemId: Long) = itemService.unequip(accountId, itemId)

    @PostMapping("/items/{itemId}/sell")
    fun sell(@LoginAccount accountId: Long, @PathVariable itemId: Long) = itemService.sell(accountId, itemId)

    /** 일괄 판매: maxGrade 이하 (COMMON = 일반만, UNCOMMON = 일반·고급) */
    @PostMapping("/items/sell-bulk")
    fun sellBulk(@LoginAccount accountId: Long, @RequestParam maxGrade: Grade): BulkSellResponse =
        itemService.sellBulk(accountId, maxGrade)

    /** 대장간 강화 */
    @PostMapping("/smith/{itemId}/enhance")
    fun enhance(@LoginAccount accountId: Long, @PathVariable itemId: Long): EnhanceResultResponse =
        itemService.enhance(accountId, itemId)
}
