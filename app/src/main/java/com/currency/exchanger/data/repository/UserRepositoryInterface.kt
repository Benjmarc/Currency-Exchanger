package com.currency.exchanger.data.repository

import com.currency.exchanger.data.local.entity.Balance
import com.currency.exchanger.data.local.entity.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun createUser(firstName: String, lastName: String): Long
    fun getUser(userId: Long): Flow<User?>
    suspend fun setInitialBalances(userId: Long, initialBalances: Map<String, Double>)
    fun getBalances(userId: Long): Flow<List<Balance>>
    suspend fun updateBalance(userId: Long, currency: String, amount: Double)
    suspend fun updateBalances(userId: Long, newBalances: Map<String, Double>)
    suspend fun getBalance(userId: Long, currency: String): Balance?
    suspend fun updateUser(userId: Long, firstName: String, lastName: String)
}
