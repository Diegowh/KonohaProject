package com.diegowh.konohaproject.timer.domain.models

data class SessionState(
    var round: Int = 0,
    var intervalType: IntervalType = IntervalType.FOCUS
)
