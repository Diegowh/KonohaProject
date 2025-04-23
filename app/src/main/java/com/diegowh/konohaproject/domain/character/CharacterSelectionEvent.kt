package com.diegowh.konohaproject.domain.character

sealed class CharacterSelectionEvent {
    data class CharacterSelected(val character: Character) : CharacterSelectionEvent()
}