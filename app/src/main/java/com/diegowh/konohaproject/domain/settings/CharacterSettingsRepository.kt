package com.diegowh.konohaproject.domain.settings

import com.diegowh.konohaproject.domain.character.CharacterDetail
import com.diegowh.konohaproject.domain.character.CharacterMetadata

interface CharacterSettingsRepository {
    fun getAllMetada(): List<CharacterMetadata>
    fun getDetailById(id: Int): CharacterDetail
}