package com.diegowh.konohaproject.feature.character.domain.repository

import com.diegowh.konohaproject.feature.character.domain.model.Character
import com.diegowh.konohaproject.feature.character.domain.model.CharacterMetadata

interface CharacterSettingsRepository {
    fun getAllMetadata(): List<CharacterMetadata>
    fun getById(id: Int): Character
    fun getSelectedCharacterId(): Int
    fun setSelectedCharacterId(id: Int)
}