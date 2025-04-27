package com.diegowh.konohaproject.ui.timer

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ArrayRes
import androidx.core.content.ContextCompat
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.core.ui.CharacterTheme
import com.diegowh.konohaproject.domain.character.Character

object ThemeManager {

    fun loadTheme(context: Context, character: Character) : CharacterTheme {

        val res = context.resources
        val frameDuration = res.getInteger(R.integer.frame_duration)

        val focusPalette = res.getIntArray(character.focusPalette)
        val breakPalette = res.getIntArray(character.breakPalette)

        fun loadFrames(@ArrayRes arrRes: Int) : List<Drawable> {
            val ta = res.obtainTypedArray(arrRes)
            val list = mutableListOf<Drawable>()
            for (i in 0 until ta.length()) {
                val id = ta.getResourceId(i, 0)
                ContextCompat.getDrawable(context, id)
                    ?.let(list::add)
            }
            ta.recycle()
            return list
        }
        val focusFrames = loadFrames(character.focusFrames)
        val breakFrames = loadFrames(character.breakFrames)

        return CharacterTheme(
            focusPalette = focusPalette,
            breakPalette = breakPalette,
            focusFrames = focusFrames,
            breakFrames = breakFrames,
            frameDuration = frameDuration,
            character = character
        )
    }
}