package com.diegowh.konohaproject.domain.settings

data class DefaultSettings(
    val focusIdx: Int,
    val shortBreakIdx: Int,
    val longBreakIdx: Int,
    val roundsIdx: Int,
    val autorun: Boolean,
    val mute: Boolean
)
