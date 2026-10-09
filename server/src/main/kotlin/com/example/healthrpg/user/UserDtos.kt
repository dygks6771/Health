package com.example.healthrpg.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class RegisterUserRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val deviceId: String,

    @field:NotBlank
    @field:Size(max = 30)
    val nickname: String,
)

data class UserResponse(
    val id: Long,
    val nickname: String,
    val level: Int,
    val exp: Long,
    val createdAt: Instant,
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id!!,
            nickname = user.nickname,
            level = user.level,
            exp = user.exp,
            createdAt = user.createdAt,
        )
    }
}
