package com.diegowh.konohaproject.ui.character

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.databinding.FragmentCharacterSelectorBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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

        // Esto tengo que moverlo a un viewmodel o controlador que se encargue no solo de la seleccion
        // del personaje sino de contener todos los datos del personaje para utilizarlos en el resto
        // de la app
        val characters = listOf(
            Character(
                1,
                "Guy 1",
                R.drawable.guy_miniatura,
                R.array.test_sakura_focus_frames,
                R.array.test_sakura_break_frames,
                R.array.test_sakura_focus_palette,
                R.array.test_sakura_break_palette
            ),
            Character(
                2, "Guy 2", R.drawable.guy_miniatura,
                R.array.test_sakura_focus_frames,
                R.array.test_sakura_break_frames,
                R.array.test_sakura_focus_palette,
                R.array.test_sakura_break_palette
            ),
            Character(
                3, "Guy 3", R.drawable.guy_miniatura,
                R.array.test_sakura_focus_frames,
                R.array.test_sakura_break_frames,
                R.array.test_sakura_focus_palette,
                R.array.test_sakura_break_palette
            ),
            Character(
                3, "Guy 4", R.drawable.guy_miniatura,
                R.array.test_sakura_focus_frames,
                R.array.test_sakura_break_frames,
                R.array.test_sakura_focus_palette,
                R.array.test_sakura_break_palette
            )
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