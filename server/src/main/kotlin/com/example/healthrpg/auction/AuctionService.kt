package com.example.healthrpg.auction

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.common.BadRequestException
import com.example.healthrpg.common.NotFoundException
import com.example.healthrpg.game.AuctionRules
import com.example.healthrpg.game.ItemSlot
import com.example.healthrpg.hero.HeroRepository
import com.example.healthrpg.hero.HeroResponse
import com.example.healthrpg.hero.HeroService
import com.example.healthrpg.item.ItemRepository
import com.example.healthrpg.item.ItemResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class ListingResponse(
    val id: Long,
    val item: ItemResponse,
    val price: Long,
    val sellerName: String,
    /** 내가 올린 물건 */
    val mine: Boolean,
    val status: ListingStatus,
    val buyerName: String?,
    /** 팔렸을 때 판매자가 받는 금액 (수수료 제외) */
    val sellerReceives: Long,
    val createdAt: Instant,
)

data class AuctionBoardResponse(val feePercent: Int, val listings: List<ListingResponse>)

data class MyAuctionResponse(
    val feePercent: Int,
    val maxListings: Int,
    val listings: List<ListingResponse>,
    /** 등록 가능한 내 장비 (미장착, 미등록) */
    val sellable: List<ItemResponse>,
    val hero: HeroResponse,
)

data class CreateListingRequest(val itemId: Long, @field:Positive val price: Long)

data class PurchaseResponse(val item: ItemResponse, val price: Long, val hero: HeroResponse)

/**
 * 경매장: 유저끼리 장비를 즉시 구매가로 거래.
 * 등록한 장비는 판매자 소유로 남되 장착·강화·판매가 잠기고, 팔리면 구매자에게 넘어간다.
 * 판매자는 가격에서 수수료 5% 를 뺀 금액을 받는다.
 */
@Service
class AuctionService(
    private val heroService: HeroService,
    private val heroRepository: HeroRepository,
    private val itemRepository: ItemRepository,
    private val listingRepository: AuctionListingRepository,
) {
    @Transactional(readOnly = true)
    fun board(accountId: Long, slot: ItemSlot?): AuctionBoardResponse {
        val me = heroService.require(accountId).id!!
        val listings = listingRepository.findActive()
            .filter { slot == null || it.item.slot == slot }
            .map { toResponse(it, me) }
        return AuctionBoardResponse(AuctionRules.FEE_PERCENT, listings)
    }

    @Transactional(readOnly = true)
    fun mine(accountId: Long): MyAuctionResponse {
        val hero = heroService.require(accountId)
        val heroId = hero.id!!
        return MyAuctionResponse(
            feePercent = AuctionRules.FEE_PERCENT,
            maxListings = AuctionRules.MAX_LISTINGS,
            listings = listingRepository.findBySeller(heroId).take(50).map { toResponse(it, heroId) },
            sellable = itemRepository.findAllByHeroIdOrderByEquippedDescIdDesc(heroId)
                .filter { !it.equipped && !it.listed }
                .map(ItemResponse::from),
            hero = heroService.response(hero),
        )
    }

    @Transactional
    fun create(accountId: Long, request: CreateListingRequest): MyAuctionResponse {
        val hero = heroService.requireForUpdate(accountId)
        val item = itemRepository.findByIdAndHeroId(request.itemId, hero.id!!) ?: throw NotFoundException("장비를 찾을 수 없습니다")
        if (item.equipped) throw BadRequestException("장착 중인 장비는 등록할 수 없습니다")
        if (item.listed) throw BadRequestException("이미 등록된 장비입니다")
        if (request.price !in AuctionRules.MIN_PRICE..AuctionRules.MAX_PRICE) {
            throw BadRequestException("가격은 ${AuctionRules.MIN_PRICE}~${AuctionRules.MAX_PRICE} 골드입니다")
        }
        if (listingRepository.countBySellerIdAndStatus(hero.id!!, ListingStatus.ACTIVE) >= AuctionRules.MAX_LISTINGS) {
            throw BadRequestException("동시에 ${AuctionRules.MAX_LISTINGS}개까지 등록할 수 있습니다")
        }
        item.listed = true
        listingRepository.save(AuctionListing(hero, item, request.price))
        return mine(accountId)
    }

    @Transactional
    fun buy(accountId: Long, listingId: Long): PurchaseResponse {
        val listing = listingRepository.findByIdForUpdate(listingId) ?: throw NotFoundException("판매 글을 찾을 수 없습니다")
        if (listing.status != ListingStatus.ACTIVE) throw BadRequestException("이미 판매가 끝난 물건입니다")
        val buyer = heroService.requireForUpdate(accountId)
        if (listing.seller.id == buyer.id) throw BadRequestException("내가 올린 물건은 살 수 없습니다")
        if (buyer.gold < listing.price) throw BadRequestException("골드가 부족합니다 (보유 ${buyer.gold}, 필요 ${listing.price})")
        val seller = heroRepository.findByIdForUpdate(listing.seller.id!!) ?: throw NotFoundException("판매자를 찾을 수 없습니다")

        buyer.gold -= listing.price
        seller.gold += AuctionRules.sellerReceives(listing.price)
        val item = listing.item
        item.hero = buyer
        item.listed = false
        item.equipped = false
        listing.status = ListingStatus.SOLD
        listing.buyer = buyer
        listing.closedAt = Instant.now()
        return PurchaseResponse(ItemResponse.from(item), listing.price, heroService.response(buyer))
    }

    @Transactional
    fun cancel(accountId: Long, listingId: Long): MyAuctionResponse {
        val hero = heroService.requireForUpdate(accountId)
        val listing = listingRepository.findByIdForUpdate(listingId)
        if (listing == null || listing.seller.id != hero.id) throw NotFoundException("판매 글을 찾을 수 없습니다")
        if (listing.status != ListingStatus.ACTIVE) throw BadRequestException("이미 끝난 판매입니다")
        listing.status = ListingStatus.CANCELLED
        listing.closedAt = Instant.now()
        listing.item.listed = false
        return mine(accountId)
    }

    private fun toResponse(l: AuctionListing, me: Long) = ListingResponse(
        id = l.id!!,
        item = ItemResponse.from(l.item),
        price = l.price,
        sellerName = l.seller.name,
        mine = l.seller.id == me,
        status = l.status,
        buyerName = l.buyer?.name,
        sellerReceives = AuctionRules.sellerReceives(l.price),
        createdAt = l.createdAt,
    )
}

@RestController
class AuctionController(private val auctionService: AuctionService) {
    /** 판매 중인 장비. slot 으로 부위 필터 (WEAPON / ARMOR / BOOTS / ACCESSORY) */
    @GetMapping("/api/me/auction")
    fun board(@LoginAccount accountId: Long, @RequestParam(required = false) slot: ItemSlot?): AuctionBoardResponse =
        auctionService.board(accountId, slot)

    @GetMapping("/api/me/auction/mine")
    fun mine(@LoginAccount accountId: Long): MyAuctionResponse = auctionService.mine(accountId)

    @PostMapping("/api/me/auction")
    fun create(@LoginAccount accountId: Long, @Valid @RequestBody request: CreateListingRequest): MyAuctionResponse =
        auctionService.create(accountId, request)

    @PostMapping("/api/me/auction/{listingId}/buy")
    fun buy(@LoginAccount accountId: Long, @PathVariable listingId: Long): PurchaseResponse =
        auctionService.buy(accountId, listingId)

    @PostMapping("/api/me/auction/{listingId}/cancel")
    fun cancel(@LoginAccount accountId: Long, @PathVariable listingId: Long): MyAuctionResponse =
        auctionService.cancel(accountId, listingId)
}
