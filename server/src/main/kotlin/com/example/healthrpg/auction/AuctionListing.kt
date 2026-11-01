package com.example.healthrpg.auction

import com.example.healthrpg.hero.Hero
import com.example.healthrpg.item.Item
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.LockModeType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.Instant

enum class ListingStatus { ACTIVE, SOLD, CANCELLED }

@Entity
@Table(name = "auction_listings")
class AuctionListing(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    val seller: Hero,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    val item: Item,

    @Column(nullable = false)
    val price: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var status: ListingStatus = ListingStatus.ACTIVE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    var buyer: Hero? = null

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()

    @Column(name = "closed_at")
    var closedAt: Instant? = null
}

interface AuctionListingRepository : JpaRepository<AuctionListing, Long> {
    @Query(
        "select l from AuctionListing l join fetch l.item join fetch l.seller " +
            "where l.status = com.example.healthrpg.auction.ListingStatus.ACTIVE order by l.createdAt desc",
    )
    fun findActive(): List<AuctionListing>

    @Query(
        "select l from AuctionListing l join fetch l.item left join fetch l.buyer " +
            "where l.seller.id = :sellerId order by l.createdAt desc",
    )
    fun findBySeller(sellerId: Long): List<AuctionListing>

    fun countBySellerIdAndStatus(sellerId: Long, status: ListingStatus): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from AuctionListing l where l.id = :id")
    fun findByIdForUpdate(id: Long): AuctionListing?
}
