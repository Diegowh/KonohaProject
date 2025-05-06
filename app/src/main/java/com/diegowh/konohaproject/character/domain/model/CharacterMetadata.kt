package com.diegowh.konohaproject.character.domain.model

import androidx.annotation.DrawableRes

data class CharacterMetadata(
    val id: Int,
    val name: String,
    @DrawableRes val iconRes: Int
)
