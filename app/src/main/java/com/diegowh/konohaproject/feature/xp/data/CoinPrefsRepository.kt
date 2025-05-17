package com.diegowh.konohaproject.feature.xp.data

import android.content.Context
import com.diegowh.konohaproject.feature.xp.domain.CoinRepository

class CoinPrefsRepository(context: Context) : CoinRepository {

    companion object {
        const val COINS_KEY = "coins_prefs"
    }
    private val prefs =
        context.applicationContext.getSharedPreferences(COINS_KEY, Context.MODE_PRIVATE)

    override fun getCoins(): Int =
        prefs.getInt("coins", 0)


    override fun setCoins(amount: Int) {
        prefs.edit().putInt("coins", amount).apply()
    }

    override fun addCoins(amount: Int) {
        val coins = getCoins() + amount
        prefs.edit().putInt("coins", coins).apply()
        println("Añadidas monedas: $amount. Monedas actuales: $coins.")
    }
}