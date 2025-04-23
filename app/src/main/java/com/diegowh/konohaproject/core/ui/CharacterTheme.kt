package com.diegowh.konohaproject.core.ui

import android.graphics.drawable.Drawable

data class CharacterTheme(
    val focusPalette: IntArray,
    val breakPalette: IntArray,
    val focusFrames: List<Drawable>,
    val breakFrames: List<Drawable>,
    val frameDuration: Int
)