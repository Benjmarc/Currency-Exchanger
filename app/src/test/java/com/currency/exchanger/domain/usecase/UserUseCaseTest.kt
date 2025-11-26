package com.currency.exchanger.domain.usecase

import com.currency.exchanger.data.local.entity.Balance
import com.currency.exchanger.data.local.entity.User
import com.currency.exchanger.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserUseCaseTest {
    
    private lateinit var userUseCase: UserUseCase
    private val mockUserRepository: UserRepository = mockk()
    
    private val testUserId = 1L
    private val testUser = User(
        id = testUserId,
        firstName = "Test",
        lastName = "User"
    )
    private val testBalances = listOf(
        Balance(id = 1, userId = testUserId, currency = "EUR", amount = 1000.0),
        Balance(id = 2, userId = testUserId, currency = "USD", amount = 0.0)
    )
    
    @Before
    fun setUp() {
        userUseCase = UserUseCase(mockUserRepository)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `createUserWithInitialBalances should call repository with correct parameters`() = runTest {
        // Given
        val firstName = "John"
        val lastName = "Doe"
        val initialBalances = mapOf("EUR" to 1000.0, "USD" to 0.0)
        
        coEvery { mockUserRepository.createUser(firstName, lastName) } returns testUserId
        coEvery { mockUserRepository.setInitialBalances(testUserId, initialBalances) } returns Unit
        
        // When
        val result = userUseCase.createUserWithInitialBalances(firstName, lastName, initialBalances)
        
        // Then
        assertEquals(testUserId, result)
        coVerify(exactly = 1) { mockUserRepository.createUser(firstName, lastName) }
        coVerify(exactly = 1) { mockUserRepository.setInitialBalances(testUserId, initialBalances) }
    }
    
    @Test
    fun `invoke should call updateUser on repository`() = runTest {
        // Given
        val firstName = "Updated"
        val lastName = "Name"
        
        coEvery { mockUserRepository.updateUser(testUserId, firstName, lastName) } returns Unit
        
        // When
        userUseCase(testUserId, firstName, lastName)
        
        // Then
        coVerify(exactly = 1) { mockUserRepository.updateUser(testUserId, firstName, lastName) }
    }
    
    @Test
    fun `getUser should return user from repository`() = runTest {
        // Given
        coEvery { mockUserRepository.getUser(testUserId) } returns flowOf(testUser)
        
        // When
        val result = userUseCase.getUser(testUserId)
        
        // Then
        result.collect { user ->
            assertEquals(testUser, user)
        }
        coVerify(exactly = 1) { mockUserRepository.getUser(testUserId) }
    }
    
    @Test
    fun `getBalances should return balances from repository`() = runTest {
        // Given
        coEvery { mockUserRepository.getBalances(testUserId) } returns flowOf(testBalances)
        
        // When
        val result = userUseCase.getBalances(testUserId)
        
        // Then
        result.collect { balances ->
            assertEquals(testBalances, balances)
        }
        coVerify(exactly = 1) { mockUserRepository.getBalances(testUserId) }
    }
    
    @Test
    fun `updateBalances should call updateBalances on repository`() = runTest {
        // Given
        val newBalances = mapOf("EUR" to 900.0, "USD" to 110.0)
        coEvery { mockUserRepository.updateBalances(testUserId, newBalances) } returns Unit
        
        // When
        userUseCase.updateBalances(testUserId, newBalances)
        
        // Then
        coVerify(exactly = 1) { mockUserRepository.updateBalances(testUserId, newBalances) }
    }
    
    @Test
    fun `getBalance should return balance from repository`() = runTest {
        // Given
        val currency = "EUR"
        val expectedBalance = testBalances.first()
        coEvery { mockUserRepository.getBalance(testUserId, currency) } returns expectedBalance
        
        // When
        val result = userUseCase.getBalance(testUserId, currency)
        
        // Then
        assertEquals(expectedBalance, result)
        coVerify(exactly = 1) { mockUserRepository.getBalance(testUserId, currency) }
    }
    
    @Test
    fun `getBalance should return null when balance not found`() = runTest {
        // Given
        val currency = "JPY"
        coEvery { mockUserRepository.getBalance(testUserId, currency) } returns null
        
        // When
        val result = userUseCase.getBalance(testUserId, currency)
        
        // Then
        assertEquals(null, result)
        coVerify(exactly = 1) { mockUserRepository.getBalance(testUserId, currency) }
    }
}
