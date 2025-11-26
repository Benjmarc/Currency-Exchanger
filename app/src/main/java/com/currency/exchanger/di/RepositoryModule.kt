package com.currency.exchanger.di

import com.currency.exchanger.data.local.dao.BalanceDao
import com.currency.exchanger.data.local.dao.UserDao
import com.currency.exchanger.data.repository.UserRepository
import com.currency.exchanger.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryProviderModule {
    
    @Provides
    @Singleton
    fun provideUserRepositoryImpl(
        userDao: UserDao,
        balanceDao: BalanceDao
    ): UserRepositoryImpl {
        return UserRepositoryImpl(userDao, balanceDao)
    }
}
