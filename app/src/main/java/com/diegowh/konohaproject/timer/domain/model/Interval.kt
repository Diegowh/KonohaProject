package com.diegowh.konohaproject.timer.domain.model

data class Interval(
    val currentRound: Int,
    val type: IntervalType,
    val nextDuration: Long
)