package com.diegowh.konohaproject.domain.character

import com.diegowh.konohaproject.R

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
            "Guy 1",
            R.drawable.guy_miniatura,
            R.array.test_sakura_focus_frames,
            R.array.test_sakura_break_frames,
            R.array.test_sakura_focus_palette,
            R.array.test_sakura_break_palette
        ),
        Character(
            3,
            "Guy 2",
            R.drawable.guy_miniatura,
            R.array.test_sakura_focus_frames,
            R.array.test_sakura_break_frames,
            R.array.test_sakura_focus_palette,
            R.array.test_sakura_break_palette
        ),
        Character(
            3,
            "Guy 3",
            R.drawable.guy_miniatura,
            R.array.test_sakura_focus_frames,
            R.array.test_sakura_break_frames,
            R.array.test_sakura_focus_palette,
            R.array.test_sakura_break_palette
        )
    )

    fun getAll(): List<Character> = allCharacters

    fun getAllMetadata(): List<CharacterMetadata> =
        allCharacters.map { CharacterMetadata(it.id, it.name, it.iconRes) }

    fun getById(id: Int): Character =
        allCharacters.first { it.id == id }
}