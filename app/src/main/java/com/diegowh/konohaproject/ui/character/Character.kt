package com.diegowh.konohaproject.ui.character

import androidx.annotation.DrawableRes

data class Character(
    val id: Int,
    @DrawableRes val iconResId: Int,
    val name: String
)