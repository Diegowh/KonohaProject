package com.diegowh.konohaproject.feature.xp.domain

import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType
import kotlin.math.roundToLong

class XpConfig(

    val xpScale: Float = 75f,

    val focusXpPerMin: Float = 2.0f,
    val shortXpPerMin: Float = 0.6f,
    val longXpPerMin: Float = 0.8f,

    val shortCapMin: Int = 15,
    val longCapMin: Int = 30,

    val skipPenaltyFactor: Float = 0.5f,
    val sessionBonusPercent: Float = 0.18f
) {

    private val focusTarget = 25f
    private val shortTarget = 5f
    private val longTarget = 15f
    private val sweetSpotTol = 0.10f

    fun calculateXp(interval: IntervalType, durationMs: Long): Long {
        val minutes = durationMs.toFloat() / 60_000f
        val base = when (interval) {
            IntervalType.FOCUS ->
                minutes * focusXpPerMin * nearTargetBonus(minutes, focusTarget)
            IntervalType.SHORT_BREAK ->
                cappedLinear(minutes, shortCapMin, shortXpPerMin) *
                        nearTargetBonus(minutes, shortTarget)
            IntervalType.LONG_BREAK ->
                cappedLinear(minutes, longCapMin, longXpPerMin) *
                        nearTargetBonus(minutes, longTarget)

        }
        return (base * xpScale)
            .roundToLong()
            .coerceAtLeast(xpScale.toLong())
    }

    // n% del xp obtenido en la sesion completa
    fun sessionBonus(totalIntervalXp: Long): Long =
        (totalIntervalXp * sessionBonusPercent).roundToLong()

    private fun cappedLinear(mins: Float, cap: Int, rate: Float): Float =
        if (mins <= cap) mins * rate
        else cap * rate + (mins - cap) * rate * 0.25f

    private fun nearTargetBonus(mins: Float, target: Float): Float {
        val tol = target * sweetSpotTol
        val delta = kotlin.math.abs(mins - target)
        return when {
            delta <= tol -> 1.30f
            delta <= tol * 3 -> 1f + (1.30f - 1f) *
                    ((tol * 3 - delta) / (tol * 3))
            mins < target -> mins / target
            else -> 1f
        }
    }

}