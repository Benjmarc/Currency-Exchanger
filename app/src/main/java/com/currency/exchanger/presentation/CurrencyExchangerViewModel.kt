package com.currency.exchanger.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.currency.exchanger.data.local.entity.Balance
import com.currency.exchanger.data.local.entity.User
import com.currency.exchanger.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CurrencyExchangerViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userWithBalances = MutableStateFlow(UserWithBalances())
    val userWithBalances: StateFlow<UserWithBalances> = _userWithBalances
    
    private var currentUserId: Long? = null

    init {
        Log.d(TAG, "Init")
        createUserWithInitialBalances()
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
                        "EUR" to 1000.0,
                        "USD" to 0.0,
                        "PHP" to 0.0
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

    companion object {
        const val TAG = "CurrencyExchangerVM"
        data class UserWithBalances(
            val user: User? = null,
            val balances: List<Balance> = emptyList()
        )
    }
}
