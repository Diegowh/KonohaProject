package com.diegowh.konohaproject.feature.character.presentation.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.feature.character.domain.model.Character
import com.google.android.material.card.MaterialCardView

class CharactersAdapter(
    private val characters: List<Character>,
    private val initialSelectedId: Int,
    private val onItemClick: (Character) -> Unit
) : RecyclerView.Adapter<CharactersAdapter.ViewHolder>() {

    private var selectedId: Int = initialSelectedId

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.card)
        val icon: ImageView = view.findViewById(R.id.character_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_character, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val character = characters[position]
        holder.icon.setImageResource(character.iconRes)

        // Destacar selección
        val isSelected = character.id == selectedId
        holder.card.strokeColor = holder.card.context.getColor(
            if (isSelected) R.color.accent_color else android.R.color.transparent
        )

        holder.itemView.setOnClickListener {
            val previousId = selectedId
            if (character.id != previousId) {
                selectedId = character.id
                // refresca antiguo y nuevo
                val oldIndex = characters.indexOfFirst { it.id == previousId }
                notifyItemChanged(oldIndex)
                notifyItemChanged(position)
                onItemClick(character)
            }
        }
    }

    override fun getItemCount(): Int = characters.size
}
