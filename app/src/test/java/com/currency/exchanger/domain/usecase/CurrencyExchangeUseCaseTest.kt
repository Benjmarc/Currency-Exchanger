package com.currency.exchanger.domain.usecase

import com.currency.exchanger.data.remote.api.CurrencyApiService
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyExchangeUseCaseTest {

    private lateinit var useCase: CurrencyExchangeUseCase
    private val mockApiService: CurrencyApiService = mockk()
    
    @Before
    fun setUp() {
        useCase = CurrencyExchangeUseCase(mockApiService)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test(expected = ExchangeRateException::class)
    fun `getExchangeRates should throw ExchangeRateException on API error`() = runTest {
        // Given
        coEvery { mockApiService.getExchangeRates() } throws Exception("Network error")
        
        // When/Then
        useCase.getExchangeRates().first()
    }
    
    @Test
    fun `calculateExchangeAmount should convert between different currencies`() {
        // Given
        val rates = mapOf("EUR" to 1.0, "USD" to 1.1, "GBP" to 0.85)
        
        // When
        val result = useCase.calculateExchangeAmount("USD", "GBP", 100.0, rates)
        
        // Then
        // 100 USD = (100 / 1.1) EUR = 90.909... EUR
        // 90.909... EUR * 0.85 = 77.272... GBP
        assertEquals(77.27, result, 0.01)
    }
    
    @Test
    fun `calculateExchangeAmount should return same amount for same currency`() {
        // Given
        val rates = mapOf("EUR" to 1.0, "USD" to 1.1)
        
        // When
        val result = useCase.calculateExchangeAmount("USD", "USD", 100.0, rates)
        
        // Then
        assertEquals(100.0, result, 0.001)
    }
    
    @Test
    fun `calculateExchangeAmount should handle EUR as from currency`() {
        // Given
        val rates = mapOf("EUR" to 1.0, "USD" to 1.1)
        
        // When
        val result = useCase.calculateExchangeAmount("EUR", "USD", 100.0, rates)
        
        // Then
        assertEquals(110.0, result, 0.001)  // 100 * 1.1
    }
    
    @Test
    fun `calculateExchangeAmount should handle EUR as to currency`() {
        // Given
        val rates = mapOf("EUR" to 1.0, "USD" to 1.1)
        
        // When
        val result = useCase.calculateExchangeAmount("USD", "EUR", 110.0, rates)
        
        // Then
        assertEquals(100.0, result, 0.001)  // 110 / 1.1
    }
    
    @Test(expected = ExchangeRateException::class)
    fun `calculateExchangeAmount should throw when rates are empty`() {
        useCase.calculateExchangeAmount("USD", "EUR", 100.0, emptyMap())
    }
    
    @Test
    fun `calculateExchangeAmount should handle missing to currency by using EUR rate`() {
        // Given
        val rates = mapOf("EUR" to 1.0, "USD" to 1.1)  // No GBP
        
        // When - since GBP is not in rates, it should default to EUR rate (1.0)
        val result = useCase.calculateExchangeAmount("USD", "GBP", 100.0, rates)
        
        // Then - should convert USD to EUR (100/1.1) and then to GBP (which defaults to EUR rate 1.0)
        assertEquals(90.91, result, 0.01)
    }
}
