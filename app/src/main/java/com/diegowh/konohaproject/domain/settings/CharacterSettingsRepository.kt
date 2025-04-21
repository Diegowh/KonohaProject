package com.diegowh.konohaproject.domain.settings

import com.diegowh.konohaproject.domain.character.Character
import com.diegowh.konohaproject.domain.character.CharacterMetadata

interface CharacterSettingsRepository {
    fun getAllMetadata(): List<CharacterMetadata>
    fun getById(id: Int): Character
    fun getSelectedCharacterId(): Int
    fun setSelectedCharacterId(id: Int)
}