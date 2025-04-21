package com.diegowh.konohaproject.domain.character
import androidx.annotation.ArrayRes


data class CharacterDetail(
    val metadata: CharacterMetadata,
    @ArrayRes val focusFrames: Int,
    @ArrayRes val breakFrames: Int,
    @ArrayRes val focusPalette: Int,
    @ArrayRes val breakPalette: Int
)