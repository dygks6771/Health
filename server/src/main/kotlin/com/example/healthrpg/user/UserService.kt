package com.example.healthrpg.user

import com.example.healthrpg.common.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {
    /** 로그인 기능 전까지는 기기 ID로 사용자 식별. 이미 있으면 기존 사용자를 반환 */
    @Transactional
    fun register(request: RegisterUserRequest): User =
        userRepository.findByDeviceId(request.deviceId)
            ?: userRepository.save(User(deviceId = request.deviceId, nickname = request.nickname))

    fun get(userId: Long): User =
        userRepository.findById(userId).orElseThrow { NotFoundException("user $userId not found") }
}
