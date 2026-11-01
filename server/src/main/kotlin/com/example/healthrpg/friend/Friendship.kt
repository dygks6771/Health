package com.example.healthrpg.friend

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
import org.springframework.data.jpa.repository.Query
import java.time.Instant

enum class FriendshipStatus { PENDING, ACCEPTED }

@Entity
@Table(name = "friendships")
class Friendship(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    val requester: Hero,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id", nullable = false)
    val addressee: Hero,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var status: FriendshipStatus = FriendshipStatus.PENDING,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()

    fun other(heroId: Long): Hero = if (requester.id == heroId) addressee else requester
}

interface FriendshipRepository : JpaRepository<Friendship, Long> {
    @Query(
        "select f from Friendship f join fetch f.requester join fetch f.addressee " +
            "where f.requester.id = :heroId or f.addressee.id = :heroId",
    )
    fun findAllInvolving(heroId: Long): List<Friendship>

    @Query(
        "select f from Friendship f where (f.requester.id = :a and f.addressee.id = :b) " +
            "or (f.requester.id = :b and f.addressee.id = :a)",
    )
    fun findBetween(a: Long, b: Long): Friendship?
}
