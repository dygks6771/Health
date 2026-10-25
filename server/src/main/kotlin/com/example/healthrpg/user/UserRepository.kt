package com.example.healthrpg.user

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<User, Long> {
    fun findByDeviceId(deviceId: String): User?

    /** 동기화 중 같은 사용자의 다른 동기화가 끼어들어 보상이 중복 지급되지 않도록 행 잠금 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    fun findByIdForUpdate(id: Long): User?
}
