package com.diegowh.konohaproject.ui.character

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.databinding.FragmentCharacterSelectorBinding

class CharacterSelectorFragment : BottomSheetDialogFragment(R.layout.fragment_character_selector) {

    private var _binding: FragmentCharacterSelectorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCharacterSelectorBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val characters = listOf(
            Character(1, R.drawable.guy_miniatura, "Guy 1"),
            Character(2, R.drawable.guy_miniatura, "Guy 2"),
            Character(3, R.drawable.guy_miniatura, "Guy 3"),
            Character(3, R.drawable.guy_miniatura, "Guy 4")
        )

        binding.charactersRecycler.adapter = CharactersAdapter(characters) { character ->
            println(character.name)
            dismiss()
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}