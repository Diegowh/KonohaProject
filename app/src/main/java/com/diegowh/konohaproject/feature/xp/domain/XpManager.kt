package com.diegowh.konohaproject.feature.xp.domain

import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType
import kotlin.math.roundToLong

class XpManager (
    private val repo: XpRepository,
    private val config: XpConfig = XpConfig()
){

    private var currentSessionXp = 0L

    fun addXpForIntervalCompleted(intervalType: IntervalType, durationMs: Long) {
        val gained = config.calculateXp(intervalType, durationMs)
        currentSessionXp += gained
        repo.addXp(gained)
        println("Current session XP: $currentSessionXp.")
    }

    fun applySkipPenalty(intervalType: IntervalType, durationMs: Long) {
        val loss = config.calculateXp(intervalType, durationMs) *
                config.skipPenaltyFactor.roundToLong()
        repo.removeXp(loss)
    }

    fun addXpForSession() {
        val bonus = config.sessionBonus(currentSessionXp)
        repo.addXp(bonus)
    }

    fun resetSessionXp() {
        currentSessionXp = 0L
        println("Session XP reseted: $currentSessionXp")
    }
}