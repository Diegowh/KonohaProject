package com.diegowh.konohaproject.feature.xp.data

import android.content.Context
import com.diegowh.konohaproject.feature.xp.domain.XpRepository

class XpPrefsRepository(context: Context) : XpRepository {

    companion object {
        const val XP_KEY = "xp_prefs"
    }
    private val prefs = context.applicationContext.getSharedPreferences(XP_KEY, Context.MODE_PRIVATE)

    // Este metodo lo dejo aqui por si lo necesito para hacer pruebas
    private fun resetXp() {
        prefs.edit().putLong("total_xp", 0).apply()
    }

    override fun addXp(amount: Long) {
        val totalXp = getTotalXp() + amount
        prefs.edit().putLong("total_xp", totalXp).apply()
        println("Añadida XP: $amount. Actual XP: $totalXp.")
    }

    override fun removeXp(amount: Long) {
        val totalXp = (getTotalXp() - amount).coerceAtLeast(0)
        prefs.edit().putLong("total_xp", totalXp).apply()
        println("Eliminada XP: $amount. Actual XP: $totalXp")
    }

    override fun getTotalXp(): Long = prefs.getLong("total_xp", 0)


}