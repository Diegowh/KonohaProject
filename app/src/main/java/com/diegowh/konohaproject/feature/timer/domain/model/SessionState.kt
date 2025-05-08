package com.diegowh.konohaproject.feature.timer.domain.model

data class SessionState(
    var round: Int = 0,
    var intervalType: IntervalType = IntervalType.FOCUS
)
