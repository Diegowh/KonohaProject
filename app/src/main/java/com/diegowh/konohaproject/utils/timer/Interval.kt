package com.diegowh.konohaproject.utils.timer

data class Interval(
    val currentRound: Int,
    val type: IntervalType,
    val nextDuration: Long
)