package com.diegowh.konohaproject.core.ui

import android.graphics.drawable.Drawable
import com.diegowh.konohaproject.domain.character.Character

data class CharacterTheme(
    val focusPalette: IntArray,
    val breakPalette: IntArray,
    val focusFrames: List<Drawable>,
    val breakFrames: List<Drawable>,
    val frameDuration: Int,
    val character: Character? = null
)