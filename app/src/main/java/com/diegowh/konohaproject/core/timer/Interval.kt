package com.diegowh.konohaproject.core.timer

data class Interval(
    val currentRound: Int,
    val type: IntervalType,
    val nextDuration: Long
)