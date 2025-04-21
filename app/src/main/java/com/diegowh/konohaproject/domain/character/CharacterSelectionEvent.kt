package com.diegowh.konohaproject.domain.character

sealed class CharacterSelectionEvent {
    data class SelectCharacter(val character: Character) : CharacterSelectionEvent()
}