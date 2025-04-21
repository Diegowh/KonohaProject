package com.diegowh.konohaproject.domain.character

import androidx.annotation.DrawableRes

data class CharacterMetadata(
    val id: Int,
    val name: String,
    @DrawableRes val iconRes: Int
)
