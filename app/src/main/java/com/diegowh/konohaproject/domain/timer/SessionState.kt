package com.diegowh.konohaproject.domain.timer

import com.diegowh.konohaproject.utils.timer.IntervalType

data class SessionState(
    var round: Int = 0,
    var intervalType: IntervalType = IntervalType.FOCUS
)
