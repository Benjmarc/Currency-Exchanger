package com.currency.exchanger.data.remote.api

import com.currency.exchanger.data.remote.model.ExchangeRatesResponse
import retrofit2.http.GET

interface CurrencyApiService {
    @GET("currency-exchange-rates")
    suspend fun getExchangeRates(): ExchangeRatesResponse
}
