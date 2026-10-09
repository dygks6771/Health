package com.example.healthrpg.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByDeviceId(deviceId: String): User?
}
