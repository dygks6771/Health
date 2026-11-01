package com.example.healthrpg.game

import org.springframework.data.jpa.repository.JpaRepository

interface AccessoryRepository : JpaRepository<Accessory, Long> {
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<Accessory>
}

interface ContentClearRepository : JpaRepository<ContentClear, Long>
