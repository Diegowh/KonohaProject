package com.diegowh.konohaproject.domain.settings

import android.content.Context
import com.diegowh.konohaproject.domain.character.CharacterDetail
import com.diegowh.konohaproject.domain.character.CharacterMetadata

class CharacterPrefsRepository(
    private val context: Context
) : CharacterSettingsRepository {

    private val prefs = context.getSharedPreferences("character_prefs", Context.MODE_PRIVATE)
    override fun getAllMetada(): List<CharacterMetadata> {
        TODO("Not yet implemented")
    }

    override fun getDetailById(id: Int): CharacterDetail {
        TODO("Not yet implemented")
    }


}