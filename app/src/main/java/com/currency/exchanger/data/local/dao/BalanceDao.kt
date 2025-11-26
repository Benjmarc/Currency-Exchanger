package com.currency.exchanger.data.local.dao

import androidx.room.*
import com.currency.exchanger.data.local.entity.Balance
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBalance(balance: Balance)

    @Update
    fun updateBalance(balance: Balance)

    @Query("SELECT * FROM balances WHERE userId = :userId")
    fun getBalancesForUser(userId: Long): Flow<List<Balance>>

    @Query("SELECT * FROM balances WHERE userId = :userId AND currency = :currency LIMIT 1")
    fun getBalanceForUserAndCurrency(userId: Long, currency: String): Balance?

    @Query("UPDATE balances SET amount = amount + :amount WHERE userId = :userId AND currency = :currency")
    fun updateBalanceAmount(userId: Long, currency: String, amount: Double)

    @Query("DELETE FROM balances WHERE userId = :userId")
    fun deleteBalancesForUser(userId: Long)
}
