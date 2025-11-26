package com.currency.exchanger.data.remote.model

import com.currency.exchanger.data.local.entity.Balance
import com.currency.exchanger.data.local.entity.User

data class UserWithBalances(
    val user: User? = null,
    val balances: List<Balance> = emptyList()
)
