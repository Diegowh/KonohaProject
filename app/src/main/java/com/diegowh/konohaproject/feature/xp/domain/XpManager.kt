package com.diegowh.konohaproject.feature.xp.domain

import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType

class XpManager (private val repo: XpRepository){

    companion object {
        private const val FOCUS_XP = 10L
        private const val SHORT_BREAK_XP = 5L
        private const val LONG_BREAK_XP = 8L
        private const val SKIP_PENALTY = 2L
        private const val SESSION_ROUND_BONUS = 20L

    }

    fun addXpForInterval(intervalType: IntervalType) {
        val xp = when (intervalType) {
            IntervalType.FOCUS -> FOCUS_XP
            IntervalType.SHORT_BREAK -> SHORT_BREAK_XP
            IntervalType.LONG_BREAK -> LONG_BREAK_XP
        }
        repo.addXp(xp)
    }

    fun applySkipPenalty(intervalType: IntervalType) {
        var xp = when (intervalType) {
            IntervalType.FOCUS -> FOCUS_XP
            IntervalType.SHORT_BREAK -> SHORT_BREAK_XP
            IntervalType.LONG_BREAK -> LONG_BREAK_XP
        }
        xp += SKIP_PENALTY
        repo.removeXp(xp)
    }

    fun addXpForSession(rounds: Int) {
        val xp = rounds * SESSION_ROUND_BONUS
        repo.addXp(xp)
    }
}