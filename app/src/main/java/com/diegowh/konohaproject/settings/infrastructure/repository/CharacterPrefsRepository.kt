package com.diegowh.konohaproject.settings.infrastructure.repository

import android.content.Context
import com.diegowh.konohaproject.character.domain.models.Character
import com.diegowh.konohaproject.character.application.CharacterDataSource
import com.diegowh.konohaproject.character.domain.models.CharacterMetadata
import com.diegowh.konohaproject.settings.domain.repository.CharacterSettingsRepository

class CharacterPrefsRepository(
    private val context: Context
) : CharacterSettingsRepository {

    private val prefs = context.getSharedPreferences("character_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SELECTED_ID = "character_selected_id"
        private const val DEFAULT_ID = 1
    }

    override fun getAllMetadata(): List<CharacterMetadata> =
        CharacterDataSource.getAllMetadata()

    override fun getById(id: Int): Character =
        CharacterDataSource.getById(id)

    override fun getSelectedCharacterId(): Int =
        prefs.getInt(KEY_SELECTED_ID, DEFAULT_ID)

    override fun setSelectedCharacterId(id: Int) {
        prefs.edit()
            .putInt(KEY_SELECTED_ID, id)
            .apply()
    }


}