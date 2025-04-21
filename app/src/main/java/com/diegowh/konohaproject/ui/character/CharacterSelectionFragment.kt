package com.diegowh.konohaproject.ui.character

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.databinding.FragmentCharacterSelectionBinding
import com.diegowh.konohaproject.domain.character.CharacterDataSource
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CharacterSelectionFragment : BottomSheetDialogFragment(R.layout.fragment_character_selection) {

    private var _binding: FragmentCharacterSelectionBinding? = null
    private val binding get() = _binding!!

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

        // Esto tengo que moverlo a un viewmodel o controlador que se encargue no solo de la seleccion
        // del personaje sino de contener todos los datos del personaje para utilizarlos en el resto
        // de la app
        val characters = CharacterDataSource.getAll()
        binding.charactersRecycler.adapter = CharactersAdapter(characters) { character ->
            println(character.name)
            setCharacterTheme(character)
        }
    }

    private fun setCharacterTheme(character: Character) {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}