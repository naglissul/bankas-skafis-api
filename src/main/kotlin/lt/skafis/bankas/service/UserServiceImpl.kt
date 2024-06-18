package lt.skafis.bankas.service

import lt.skafis.bankas.dto.UserViewDto
import lt.skafis.bankas.model.Role
import lt.skafis.bankas.repository.FirestoreUserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.webjars.NotFoundException

@Service
class UserServiceImpl(private val userRepository: FirestoreUserRepository) : UserService {

    val log: Logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    override fun getUserById(userId: String): UserViewDto? {
        log.info("Getting user by id: $userId")
        val user = userRepository.getUserById(userId) ?: throw NotFoundException("User not found")
        log.info("User found")
        return UserViewDto(userId, user.email, user.username, user.role)
    }

    override fun getUsernameById(userId: String): String? {
        log.info("Getting username by id: $userId")
        val user = userRepository.getUserById(userId) ?: throw NotFoundException("User not found")
        log.info("Username found")
        return user.username
    }

    override fun getRoleById(userId: String): Role? {
        log.info("Getting role by id: $userId")
        val user = userRepository.getUserById(userId) ?: throw NotFoundException("User not found")
        log.info("Role found")
        return user.role
    }

    override fun updateBio(bio: String, userId: String): Boolean {
        log.info("Updating bio for user: $userId")
        val success = userRepository.updateUserBio(userId, bio)
        if (!success) {
            throw NotFoundException("User not found")
        }
        log.info("Bio updated successfully")
        return true
    }
}
