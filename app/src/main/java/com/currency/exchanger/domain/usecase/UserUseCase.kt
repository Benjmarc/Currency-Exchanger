package com.currency.exchanger.domain.usecase

import com.currency.exchanger.data.local.entity.Balance
import com.currency.exchanger.data.local.entity.User
import com.currency.exchanger.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for all user-related operations
 * @property userRepository The repository to interact with user data
 */
class UserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Creates a new user with initial balances
     * @param firstName User's first name
     * @param lastName User's last name
     * @param initialBalances Map of currency to initial balance amounts
     * @return The ID of the created user
     */
    suspend fun createUserWithInitialBalances(
        firstName: String,
        lastName: String,
        initialBalances: Map<String, Double>
    ): Long {
        val userId = userRepository.createUser(firstName, lastName)
        userRepository.setInitialBalances(userId, initialBalances)
        return userId
    }

    /**
     * Updates the user's information
     * @param userId The ID of the user to update
     * @param firstName New first name
     * @param lastName New last name
     */
    suspend operator fun invoke(userId: Long, firstName: String, lastName: String) {
        userRepository.updateUser(userId, firstName, lastName)
    }

    /**
     * Gets a user by ID
     * @param userId The ID of the user to retrieve
     * @return Flow emitting the user or null if not found
     */
    fun getUser(userId: Long): Flow<User?> {
        return userRepository.getUser(userId)
    }

    /**
     * Gets all balances for a user
     * @param userId The ID of the user
     * @return Flow emitting the list of balances
     */
    fun getBalances(userId: Long): Flow<List<Balance>> {
        return userRepository.getBalances(userId)
    }

    /**
     * Updates user balances
     * @param userId The ID of the user
     * @param newBalances Map of currency to new balance amounts
     */
    suspend fun updateBalances(userId: Long, newBalances: Map<String, Double>) {
        userRepository.updateBalances(userId, newBalances)
    }

    /**
     * Gets a specific balance for a user and currency
     * @param userId The ID of the user
     * @param currency The currency code
     * @return The balance or null if not found
     */
    suspend fun getBalance(userId: Long, currency: String): Balance? {
        return userRepository.getBalance(userId, currency)
    }
}
