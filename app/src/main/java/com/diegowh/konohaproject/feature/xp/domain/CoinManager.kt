package com.diegowh.konohaproject.feature.xp.domain

import kotlin.math.roundToInt

class CoinManager(
    private val repo: CoinRepository,
) {

    // 1_000 xp = 1 moneda
    private fun xpToCoins(xp: Long): Int = (xp.toFloat() / 1000f)
        .toInt()
        .coerceAtLeast(0)

    fun addCoinsFromXp(xp: Long) = repo.addCoins(xpToCoins(xp))

    fun getBalance(): Int = repo.getCoins()

    fun addBonusCoins(amount: Int) {
        repo.addCoins(amount)
    }

    fun spendCoins(amount: Int): PurchaseResult {
        val current = repo.getCoins()
        return if (current >= amount) {
            repo.setCoins(current - amount)
            PurchaseResult.Success
        } else {
            PurchaseResult.InsufficientCoins
        }
    }

    fun resetCoins() {
        repo.setCoins(0)
    }
}