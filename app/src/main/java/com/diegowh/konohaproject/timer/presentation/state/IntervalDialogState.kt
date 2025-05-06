package com.diegowh.konohaproject.timer.presentation.state

import com.diegowh.konohaproject.timer.domain.model.IntervalType

data class IntervalDialogState(
    val showDialog: Boolean = false,
    val intervalType: IntervalType? = null,
    val continueNext: Boolean? = null
)