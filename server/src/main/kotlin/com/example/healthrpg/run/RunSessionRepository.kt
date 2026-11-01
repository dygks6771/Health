package com.example.healthrpg.run

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface RunSessionRepository : JpaRepository<RunSession, Long> {
    fun existsByUserIdAndExternalId(userId: Long, externalId: String): Boolean

    @Query(
        "select coalesce(sum(r.countedMeters), 0) from RunSession r " +
            "where r.user.id = :userId and r.runDate between :from and :to",
    )
    fun sumCountedMeters(userId: Long, from: LocalDate, to: LocalDate): Long

    @Query(
        "select coalesce(sum(r.distanceMeters), 0) from RunSession r " +
            "where r.user.id = :userId and r.runDate between :from and :to",
    )
    fun sumDistanceMeters(userId: Long, from: LocalDate, to: LocalDate): Long
}
