package com.diegowh.konohaproject.feature.xp.data

import android.content.Context
import com.diegowh.konohaproject.feature.xp.domain.XpRepository

class XpPrefsRepository(context: Context) : XpRepository {

    private val prefs = context.applicationContext.getSharedPreferences("xp_prefs", Context.MODE_PRIVATE)


    override fun addXp(amount: Long) {
        val totalXp = getTotalXp() + amount
        prefs.edit().putLong("total_xp", totalXp).apply()
    }

    override fun removeXp(amount: Long) {
        val totalXp = (getTotalXp() - amount).coerceAtLeast(0)
        prefs.edit().putLong("total_xp", totalXp).apply()
    }

    override fun getTotalXp(): Long = prefs.getLong("total_xp", 0)


}