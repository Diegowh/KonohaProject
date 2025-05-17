package com.diegowh.konohaproject.feature.xp.domain

interface XpRepository {
    fun addXp(amount: Long)
    fun removeXp(amount: Long)
    fun getTotalXp(): Long
}