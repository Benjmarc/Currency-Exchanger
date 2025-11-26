package com.currency.exchanger.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.currency.exchanger.data.remote.model.UserWithBalances
import com.currency.exchanger.domain.usecase.CurrencyExchangeUseCase
import com.currency.exchanger.domain.usecase.UserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.sql.SQLException
import javax.inject.Inject

@HiltViewModel
class CurrencyExchangerViewModel @Inject constructor(
    private val userUseCase: UserUseCase,
    private val currencyExchangeUseCase: CurrencyExchangeUseCase
) : ViewModel() {
    
    private val _errorState = MutableStateFlow<ErrorState?>(null)
    val errorState: StateFlow<ErrorState?> = _errorState.asStateFlow()
    
    private fun handleError(throwable: Throwable, errorMessage: String): ErrorState {
        return when (throwable) {
            is IOException -> ErrorState.NetworkError(
                message = "Network error: $errorMessage"
            )
            is SQLException -> ErrorState.DatabaseError(
                message = "Database error: $errorMessage"
            )
            else -> ErrorState.UnknownError(
                message = "An error occurred: ${throwable.message ?: "Unknown error"}"
            )
        }.also { errorState ->
            _errorState.value = errorState
            Log.e(TAG, errorMessage, throwable)
        }
    }

    private val _userWithBalances = MutableStateFlow(UserWithBalances())
    val userWithBalances: StateFlow<UserWithBalances> = _userWithBalances

    private var currentUserId: Long? = null

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        Log.d(TAG, "Init")
        createUserWithInitialBalances()
        fetchExchangeRates()
    }

    private fun fetchExchangeRates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            while (true) {
                try {
                    _isLoading.value = true
                    currencyExchangeUseCase.getExchangeRates(forceRefresh)
                        .collect { rates ->
                            _exchangeRates.value = rates
                            Log.d(TAG, "Updated exchange rates: $rates")
                        }
                } catch (e: Exception) {
                    handleError(e, "Failed to fetch exchange rates")
                } finally {
                    _isLoading.value = false
                }
                
                // Wait for 5 seconds before next refresh
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    fun createUserWithInitialBalances() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = withContext(Dispatchers.IO) {
                    userUseCase.createUserWithInitialBalances(
                        "Benjmarc",
                        "Somigao",
                        mapOf("EUR" to 1000.0)
                    )
                }
                currentUserId = userId
                observeUserAndBalances(userId)
                Log.d(TAG, "Successfully created user with ID: $userId")
            } catch (e: Exception) {
                handleError(e, "Failed to create user or set initial balances")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun observeUserAndBalances(userId: Long) {
        viewModelScope.launch {
            try {
                // Collect user data
                userUseCase.getUser(userId).collectLatest { user ->
                    _userWithBalances.value = _userWithBalances.value.copy(user = user)
                    Log.d(TAG, "Updated user: $user")
                }
            } catch (e: Exception) {
                handleError(e, "Failed to load user data")
            }
        }

        viewModelScope.launch {
            try {
                // Collect balances
                userUseCase.getBalances(userId).collectLatest { balances ->
                    _userWithBalances.value = _userWithBalances.value.copy(balances = balances)
                    Log.d(TAG, "Updated balances: $balances")
                }
            } catch (e: Exception) {
                handleError(e, "Failed to load balance data")
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

                // Calculate the received amount using the use case
                val receivedAmount = currencyExchangeUseCase.calculateExchangeAmount(
                    fromCurrency = fromCurrency,
                    toCurrency = toCurrency,
                    amount = amount,
                    rates = rates
                )

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
                    userUseCase.updateBalances(userId, newBalances)
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
        sealed class ErrorState(
            open val message: String
        ) {
            data class NetworkError(override val message: String) : ErrorState(message)
            data class DatabaseError(override val message: String) : ErrorState(message)
            data class UnknownError(override val message: String) : ErrorState(message)
        }
    }
}
