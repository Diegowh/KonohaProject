package com.diegowh.konohaproject.feature.character.domain.model

import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes

data class Character(
    val id: Int,
    val name: String,
    @DrawableRes val iconRes: Int,
    @ArrayRes val focusFrames: Int,
    @ArrayRes val breakFrames: Int,
    @ArrayRes val focusPalette: Int,
    @ArrayRes val breakPalette: Int
)