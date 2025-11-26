package com.currency.exchanger.domain.usecase

import com.currency.exchanger.data.remote.api.CurrencyApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Use case for handling currency exchange operations
 * @property currencyApiService The API service for fetching exchange rates
 */
class CurrencyExchangeUseCase @Inject constructor(
    private val currencyApiService: CurrencyApiService
) {
    private var lastFetchedTime: Long = 0
    private var cachedRates: Map<String, Double> = emptyMap()
    private val CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(30) // 30 minutes cache

    /**
     * Fetches exchange rates from the API or returns cached rates if they're still valid
     * @param forceRefresh If true, ignores the cache and fetches fresh data
     * @return Flow containing the exchange rates
     */
    fun getExchangeRates(forceRefresh: Boolean = false): Flow<Map<String, Double>> = flow {
        try {
            val currentTime = System.currentTimeMillis()
            val isCacheValid = !forceRefresh && 
                cachedRates.isNotEmpty() && 
                (currentTime - lastFetchedTime <= CACHE_DURATION_MS)
            
            if (isCacheValid) {
                emit(cachedRates)
                return@flow
            }

            val response = currencyApiService.getExchangeRates()
            cachedRates = response.rates
            lastFetchedTime = currentTime
            emit(response.rates)
        } catch (e: Exception) {
            // In a real app, you might want to handle specific exceptions differently
            throw ExchangeRateException("Failed to fetch exchange rates", e)
        }
    }

    /**
     * Calculates the exchange amount from one currency to another
     * @param fromCurrency The source currency code
     * @param toCurrency The target currency code
     * @param amount The amount to convert
     * @param rates The current exchange rates
     * @return The converted amount
     */
    fun calculateExchangeAmount(
        fromCurrency: String,
        toCurrency: String,
        amount: Double,
        rates: Map<String, Double>
    ): Double {
        if (rates.isEmpty()) throw ExchangeRateException("No exchange rates available")
        
        // If same currency, no conversion needed
        if (fromCurrency == toCurrency) return amount
        
        // Calculate exchange rate (convert both to EUR first, then to target)
        val fromRate = rates[fromCurrency] ?: 1.0  // If fromCurrency is EUR, rate is 1.0
        val toRate = rates[toCurrency] ?: 1.0      // If toCurrency is EUR, rate is 1.0
        
        // Calculate amount in EUR first, then to target currency
        val amountInEur = amount / fromRate
        return amountInEur * toRate
    }
}

/**
 * Exception thrown when there's an error with exchange rate operations
 */
class ExchangeRateException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
