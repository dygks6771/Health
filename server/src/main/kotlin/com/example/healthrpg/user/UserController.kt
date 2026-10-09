package com.example.healthrpg.user

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @PostMapping
    fun register(@Valid @RequestBody request: RegisterUserRequest): UserResponse =
        UserResponse.from(userService.register(request))

    @GetMapping("/{userId}")
    fun get(@PathVariable userId: Long): UserResponse =
        UserResponse.from(userService.get(userId))
}
