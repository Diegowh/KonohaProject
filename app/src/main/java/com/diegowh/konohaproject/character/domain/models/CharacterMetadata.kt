package com.diegowh.konohaproject.character.domain.models

import androidx.annotation.DrawableRes

data class CharacterMetadata(
    val id: Int,
    val name: String,
    @DrawableRes val iconRes: Int
)
