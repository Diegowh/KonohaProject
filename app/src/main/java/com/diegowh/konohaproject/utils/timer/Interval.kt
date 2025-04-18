package com.diegowh.konohaproject.utils.timer

data class Interval(
    val currentRound: Int,
    val isFocus: Boolean,
    val nextDuration: Long
)