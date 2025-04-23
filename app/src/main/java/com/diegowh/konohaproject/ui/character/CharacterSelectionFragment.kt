package com.diegowh.konohaproject.ui.character

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.databinding.FragmentCharacterSelectionBinding
import com.diegowh.konohaproject.domain.character.CharacterDataSource
import com.diegowh.konohaproject.domain.timer.TimerScreenEvent
import com.diegowh.konohaproject.ui.timer.TimerViewModel
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
        val characters = metadatas.map { meta ->
            CharacterDataSource.getById(meta.id)
        }

        binding.charactersRecycler.adapter =
            CharactersAdapter(characters) { character ->
                timerViewModel.onEvent(TimerScreenEvent.CharacterEvent.Select(character))
//                dismiss()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}