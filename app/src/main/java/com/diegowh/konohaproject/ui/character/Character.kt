package com.diegowh.konohaproject.ui.character

import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.IntegerRes

data class Character(
    val id: Int,
    val name: String,
    @DrawableRes val iconRes: Int,
    @ArrayRes val focusFrames: Int,
    @ArrayRes val breakFrames: Int,
    @ArrayRes val focusPalette: Int,
    @ArrayRes val breakPalette: Int
)