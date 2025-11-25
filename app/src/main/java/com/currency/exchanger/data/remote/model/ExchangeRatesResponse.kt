package com.currency.exchanger.data.remote.model

data class ExchangeRatesResponse(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)
