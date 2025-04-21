package com.diegowh.konohaproject.ui.character

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.diegowh.konohaproject.R

class CharactersAdapter(
    private val characters: List<Character>,
    private val onItemClick: (Character) -> Unit
) : RecyclerView.Adapter<CharactersAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.character_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_character, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val character = characters[position]
        holder.icon.setImageResource(character.iconRes)
        holder.itemView.setOnClickListener { onItemClick(character) }
    }

    override fun getItemCount() = characters.size
}