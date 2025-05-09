package com.diegowh.konohaproject.feature.timer.presentation.model

import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType

data class IntervalDialogState(
    val showDialog: Boolean = false,
    val intervalType: IntervalType? = null,
    val continueNext: Boolean? = null
)