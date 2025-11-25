package com.currency.exchanger.di

import android.content.Context
import androidx.room.Room
import com.currency.exchanger.data.local.AppDatabase
import com.currency.exchanger.data.local.dao.BalanceDao
import com.currency.exchanger.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "currency_exchanger_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
    
    @Provides
    fun provideBalanceDao(database: AppDatabase): BalanceDao = database.balanceDao()
}
