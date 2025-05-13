package com.diegowh.konohaproject.feature.character.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.activityViewModels
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.core.ui.GridSpacingItemDecoration
import com.diegowh.konohaproject.databinding.FragmentCharacterSelectionBinding
import com.diegowh.konohaproject.feature.character.data.local.CharacterDataSource
import com.diegowh.konohaproject.feature.character.presentation.controller.CharactersAdapter
import com.diegowh.konohaproject.feature.timer.presentation.viewmodel.TimerEvent
import com.diegowh.konohaproject.feature.timer.presentation.viewmodel.TimerViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CharacterSelectionFragment :
    BottomSheetDialogFragment(R.layout.fragment_character_selection) {

    private var _binding: FragmentCharacterSelectionBinding? = null
    private val binding get() = _binding!!


    private val timerViewModel: TimerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCharacterSelectionBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val metadatas = CharacterDataSource.getAllMetadata()
        val characters = metadatas.map { CharacterDataSource.getById(it.id) }
        val currentId = timerViewModel.characterState.value.character.id

        // ItemDecoration para espaciado uniforme
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_item_spacing)
        binding.charactersRecycler.addItemDecoration(GridSpacingItemDecoration(4, spacing, true))

        binding.charactersRecycler.adapter =
            CharactersAdapter(characters, currentId) { character ->
                timerViewModel.onEvent(TimerEvent.CharacterAction.Select(character))
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}