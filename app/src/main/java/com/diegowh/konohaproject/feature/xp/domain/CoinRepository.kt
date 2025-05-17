package com.diegowh.konohaproject.feature.xp.domain

interface CoinRepository {
    fun getCoins(): Int
    fun setCoins(amount: Int)
    fun addCoins(amount: Int)
}