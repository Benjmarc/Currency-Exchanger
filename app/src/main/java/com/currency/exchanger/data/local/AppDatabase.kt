package com.currency.exchanger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.currency.exchanger.data.local.dao.BalanceDao
import com.currency.exchanger.data.local.dao.UserDao
import com.currency.exchanger.data.local.entity.Balance
import com.currency.exchanger.data.local.entity.User
import javax.inject.Singleton

@Database(
    entities = [User::class, Balance::class],
    version = 1,
    exportSchema = false
)
@Singleton
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun balanceDao(): BalanceDao
}
