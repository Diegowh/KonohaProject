package com.diegowh.konohaproject.feature.xp.domain

sealed class PurchaseResult {
    data object Success: PurchaseResult()
    data object InsufficientCoins: PurchaseResult()
}