package com.diegowh.konohaproject.timer.domain.models

data class Interval(
    val currentRound: Int,
    val type: IntervalType,
    val nextDuration: Long
)