package com.currency.exchanger.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.currency.exchanger.data.remote.api.CurrencyApiService
import com.currency.exchanger.data.remote.model.UserWithBalances
import com.currency.exchanger.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class CurrencyExchangerViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val currencyApiService: CurrencyApiService
) : ViewModel() {

    private val _userWithBalances = MutableStateFlow(UserWithBalances())
    val userWithBalances: StateFlow<UserWithBalances> = _userWithBalances
    
    private var currentUserId: Long? = null

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates
    
    private var lastFetchedTime: Long = 0
    private val CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(30) // 30 minutes cache

    init {
        Log.d(TAG, "Init")
        createUserWithInitialBalances()
        fetchExchangeRates()
    }

    private fun fetchExchangeRates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val currentRates = _exchangeRates.value
                val isCacheValid = !forceRefresh && 
                    currentRates.isNotEmpty() && 
                    (currentTime - lastFetchedTime <= CACHE_DURATION_MS)
                
                if (isCacheValid) {
                    Log.d(TAG, "Using cached exchange rates")
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    currencyApiService.getExchangeRates()
                }
                
                _exchangeRates.value = response.rates
                lastFetchedTime = currentTime
                Log.d(TAG, "Fetched exchange rates: ${response.rates}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching exchange rates", e)
                // You might want to show an error message to the user here
            }
        }
    }
    
    fun createUserWithInitialBalances() {
        viewModelScope.launch {
            try {
                // Run database operations on IO dispatcher
                val userId = withContext(Dispatchers.IO) {
                    // Create user
                    val id = userRepository.createUser("Benjmarc", "Somigao")
                    // Set initial balances
                    val initialBalances = mapOf(
                        "EUR" to 1000.0
                    )
                    userRepository.setInitialBalances(id, initialBalances)
                    id
                }
                currentUserId = userId
                observeUserAndBalances(userId)
                Log.d(TAG, "Successfully created user with ID: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating user or setting initial balances", e)
            }
        }
    }
    
    private fun observeUserAndBalances(userId: Long) {
        viewModelScope.launch {
            try {
                // Collect user data
                userRepository.getUser(userId).collectLatest { user ->
                    _userWithBalances.value = _userWithBalances.value.copy(user = user)
                    Log.d(TAG, "Updated user: $user")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing user", e)
            }
        }
        
        viewModelScope.launch {
            try {
                // Collect balances
                userRepository.getBalances(userId).collectLatest { balances ->
                    _userWithBalances.value = _userWithBalances.value.copy(balances = balances)
                    Log.d(TAG, "Updated balances: $balances")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing balances", e)
            }
        }
    }

    /**
     * Performs a currency exchange operation
     * @param fromCurrency The currency code to sell (e.g., "EUR")
     * @param toCurrency The currency code to receive (e.g., "USD")
     * @param amount The amount to exchange
     */
    fun performExchange(fromCurrency: String, toCurrency: String, amount: Double) {
        viewModelScope.launch {
            try {
                val userId = currentUserId ?: run {
                    Log.e(TAG, "No user ID available for exchange")
                    return@launch
                }

                // Get current exchange rates
                val rates = _exchangeRates.value
                if (rates.isEmpty()) {
                    Log.e(TAG, "No exchange rates available")
                    return@launch
                }

                // Get current balances
                val currentBalances = _userWithBalances.value.balances.associateBy { it.currency }
                val fromBalance = currentBalances[fromCurrency]?.amount ?: 0.0
                var toBalance = currentBalances[toCurrency]?.amount ?: 0.0

                // Calculate exchange rate (convert both to EUR first, then to target)
                val fromRate = rates[fromCurrency] ?: 1.0  // If fromCurrency is EUR, rate is 1.0
                val toRate = rates[toCurrency] ?: 1.0      // If toCurrency is EUR, rate is 1.0

                // Calculate amount in EUR first, then to target currency
                val amountInEur = amount / fromRate
                val receivedAmount = amountInEur * toRate

                // Update balances
                val updatedFromBalance = fromBalance - amount
                val updatedToBalance = toBalance + receivedAmount

                // Ensure we don't go negative
                if (updatedFromBalance < 0) {
                    Log.e(TAG, "Insufficient balance for exchange")
                    return@launch
                }

                // Update balances in database
                withContext(Dispatchers.IO) {
                    val newBalances = mutableMapOf<String, Double>()
                    newBalances[fromCurrency] = updatedFromBalance
                    newBalances[toCurrency] = updatedToBalance
                    userRepository.updateBalances(userId, newBalances)
                }

                Log.d(TAG, "Exchange successful: $amount $fromCurrency -> $receivedAmount $toCurrency")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error performing exchange", e)
                // In a production app, you might want to show an error message to the user
            }
        }
    }

    companion object {
        const val TAG = "CurrencyExchangerVM"
    }
}
