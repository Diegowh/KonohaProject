package com.diegowh.konohaproject.settings.domain.repository

import com.diegowh.konohaproject.character.domain.models.Character
import com.diegowh.konohaproject.character.domain.models.CharacterMetadata

interface CharacterSettingsRepository {
    fun getAllMetadata(): List<CharacterMetadata>
    fun getById(id: Int): Character
    fun getSelectedCharacterId(): Int
    fun setSelectedCharacterId(id: Int)
}