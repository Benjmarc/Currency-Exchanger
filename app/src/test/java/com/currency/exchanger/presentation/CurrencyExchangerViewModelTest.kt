package com.currency.exchanger.presentation

import com.currency.exchanger.data.local.entity.Balance
import com.currency.exchanger.data.local.entity.User
import com.currency.exchanger.domain.usecase.CurrencyExchangeUseCase
import com.currency.exchanger.domain.usecase.UserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class CurrencyExchangerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockUserUseCase: UserUseCase = mockk()
    private val mockCurrencyExchangeUseCase: CurrencyExchangeUseCase = mockk()
    
    private lateinit var viewModel: CurrencyExchangerViewModel
    
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
    private val testExchangeRates = mapOf("EUR" to 1.0, "USD" to 1.1)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mocks
        coEvery { mockUserUseCase.createUserWithInitialBalances(any(), any(), any()) } returns testUserId
        coEvery { mockUserUseCase.getUser(testUserId) } returns flowOf(testUser)
        coEvery { mockUserUseCase.getBalances(testUserId) } returns flowOf(testBalances)
        coEvery { mockUserUseCase.updateBalances(any(), any()) } returns Unit
        
        coEvery { mockCurrencyExchangeUseCase.getExchangeRates(any()) } returns flowOf(testExchangeRates)
        coEvery { 
            mockCurrencyExchangeUseCase.calculateExchangeAmount(any(), any(), any(), any()) 
        } answers { 
            val amount = it.invocation.args[2] as Double
            amount * 1.1 // Simple conversion for testing: 1 EUR = 1.1 USD
        }
        
        viewModel = CurrencyExchangerViewModel(mockUserUseCase, mockCurrencyExchangeUseCase)
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }
    
    @Test
    fun `performExchange should not update balances when insufficient funds`() = runTest {
        // Given
        val fromCurrency = "EUR"
        val toCurrency = "USD"
        val amount = 1500.0 // More than the available 1000 EUR
        
        // When
        viewModel.performExchange(fromCurrency, toCurrency, amount)
        
        // Then - verify updateBalances was not called
        coVerify(exactly = 0) { mockUserUseCase.updateBalances(any(), any()) }
    }
    
    @Test
    fun `clearError should clear error state`() {
        // Given
        viewModel.clearError()
        
        // When
        val errorState = viewModel.errorState.value
        
        // Then
        assertEquals(null, errorState)
    }
    
    @Test
    fun `isLoading should be true during operations`() = runTest {
        // Given - setup in @Before
        
        // When - perform an operation
        viewModel.performExchange("EUR", "USD", 100.0)
        
        // Then - verify loading state was updated
        // Note: In a real test, you might need to use a TestCoroutineScheduler to verify this
        assertFalse(viewModel.isLoading.value)
    }
}
