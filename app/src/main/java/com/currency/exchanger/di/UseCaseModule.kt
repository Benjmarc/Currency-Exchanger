package com.currency.exchanger.di

import com.currency.exchanger.data.remote.api.CurrencyApiService
import com.currency.exchanger.data.repository.UserRepository
import com.currency.exchanger.domain.usecase.CurrencyExchangeUseCase
import com.currency.exchanger.domain.usecase.UserUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    
    @Provides
    @Singleton
    fun provideUpdateUserUseCase(userRepository: UserRepository): UserUseCase {
        return UserUseCase(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideCurrencyExchangeUseCase(currencyApiService: CurrencyApiService): CurrencyExchangeUseCase {
        return CurrencyExchangeUseCase(currencyApiService)
    }
}
