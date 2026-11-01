package com.example.healthrpg.shop

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.game.Grade
import com.example.healthrpg.game.Region
import com.example.healthrpg.game.ShopCategory
import com.example.healthrpg.game.ShopRules
import com.example.healthrpg.hero.HeroResponse
import com.example.healthrpg.hero.HeroService
import com.example.healthrpg.item.ItemFactory
import com.example.healthrpg.item.ItemResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

data class ShopOffer(val category: ShopCategory, val name: String, val slots: List<String>, val price: Long)

data class GradeChance(val grade: Grade, val gradeName: String, val percent: Double)

data class ShopResponse(
    /** 상품 수치 기준 지역 티어 (내 레벨 지역) */
    val tier: Int,
    val regionName: String,
    val offers: List<ShopOffer>,
    val chances: List<GradeChance>,
)

data class DrawResultResponse(val item: ItemResponse, val price: Long, val hero: HeroResponse)

/** 상점: 골드로 무기 / 방어구(갑옷·장화) 무작위 뽑기. 수치는 내 레벨 지역 티어 기준 */
@Service
class ShopService(
    private val heroService: HeroService,
    private val itemFactory: ItemFactory,
) {
    @Transactional(readOnly = true)
    fun shop(accountId: Long): ShopResponse {
        val region = Region.forLevel(heroService.require(accountId).level)
        val total = ShopRules.gradeWeights.sum().toDouble()
        return ShopResponse(
            tier = region.tier,
            regionName = region.displayName,
            offers = ShopCategory.entries.map {
                ShopOffer(it, it.displayName, it.slots.map { s -> s.displayName }, ShopRules.price(region.tier))
            },
            chances = Grade.entries.mapIndexed { i, g -> GradeChance(g, g.displayName, ShopRules.gradeWeights[i] * 100 / total) },
        )
    }

    @Transactional
    fun draw(accountId: Long, category: ShopCategory): DrawResultResponse {
        val hero = heroService.requireForUpdate(accountId)
        val region = Region.forLevel(hero.level)
        val price = ShopRules.price(region.tier)
        if (hero.gold < price) throw BadRequestException("골드가 부족합니다 (보유 ${hero.gold}, 필요 $price)")
        hero.gold -= price
        val item = itemFactory.shopDraw(hero, category.slots, region.tier, ShopRules.gradeWeights, category.prefix)
        return DrawResultResponse(ItemResponse.from(item), price, heroService.response(hero))
    }
}

@RestController
class ShopController(private val shopService: ShopService) {
    @GetMapping("/api/me/shop")
    fun shop(@LoginAccount accountId: Long): ShopResponse = shopService.shop(accountId)

    /** category = WEAPON | ARMOR */
    @PostMapping("/api/me/shop/{category}/draw")
    fun draw(@LoginAccount accountId: Long, @PathVariable category: ShopCategory): DrawResultResponse =
        shopService.draw(accountId, category)
}
