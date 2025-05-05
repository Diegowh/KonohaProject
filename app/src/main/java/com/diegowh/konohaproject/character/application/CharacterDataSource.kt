package com.diegowh.konohaproject.character.application

import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.character.domain.models.Character
import com.diegowh.konohaproject.character.domain.models.CharacterMetadata

object CharacterDataSource {

    private val allCharacters = listOf(
        Character(
            1,
            "Sakura",
            R.drawable.sakura_miniatura,
            R.array.test_sakura_focus_frames,
            R.array.test_sakura_break_frames,
            R.array.test_sakura_focus_palette,
            R.array.test_sakura_break_palette
        ),
        Character(
            2,
            "Guy",
            R.drawable.guy_miniatura,
            R.array.test_guy_focus_frames,
            R.array.test_guy_break_frames,
            R.array.test_guy_focus_palette,
            R.array.test_guy_break_palette
        ),
        Character(
            3,
            "Naruto",
            R.drawable.naruto_miniatura,
            R.array.test_naruto_focus_frames,
            R.array.test_naruto_break_frames,
            R.array.test_naruto_focus_palette,
            R.array.test_naruto_break_palette
        ),
        Character(
            4,
            "Kakashi",
            R.drawable.kakashi_miniatura,
            R.array.test_kakashi_focus_frames,
            R.array.test_kakashi_break_frames,
            R.array.test_kakashi_focus_palette,
            R.array.test_kakashi_break_palette
        )
    )

    fun getAll(): List<Character> = allCharacters

    fun getAllMetadata(): List<CharacterMetadata> =
        allCharacters.map { CharacterMetadata(it.id, it.name, it.iconRes) }

    fun getById(id: Int): Character =
        allCharacters.first { it.id == id }
}