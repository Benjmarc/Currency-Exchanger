package com.currency.exchanger.data.repository

import com.currency.exchanger.data.local.dao.BalanceDao
import com.currency.exchanger.data.local.dao.UserDao
import com.currency.exchanger.data.local.entity.Balance
import com.currency.exchanger.data.local.entity.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val balanceDao: BalanceDao
) : UserRepository {

    // User operations
    override suspend fun createUser(firstName: String, lastName: String): Long {
        val user = User(firstName = firstName, lastName = lastName)
        return userDao.insertUser(user)
    }

    override fun getUser(userId: Long): Flow<User?> = userDao.getUserById(userId)

    // Balance operations
    override suspend fun setInitialBalances(userId: Long, initialBalances: Map<String, Double>) {
        initialBalances.forEach { (currency, amount) ->
            val balance = Balance(userId = userId, currency = currency, amount = amount)
            balanceDao.insertBalance(balance)
        }
    }

    override fun getBalances(userId: Long): Flow<List<Balance>> {
        return balanceDao.getBalancesForUser(userId)
    }

    override suspend fun updateBalance(userId: Long, currency: String, amount: Double) {
        balanceDao.updateBalanceAmount(userId, currency, amount)
    }

    override suspend fun getBalance(userId: Long, currency: String): Balance? {
        return balanceDao.getBalanceForUserAndCurrency(userId, currency)
    }
}
