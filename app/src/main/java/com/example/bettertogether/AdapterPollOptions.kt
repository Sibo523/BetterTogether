package com.example.bettertogether

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterPollOptions(
    private val options: MutableList<String>
) : RecyclerView.Adapter<AdapterPollOptions.PollOptionViewHolder>() {

    class PollOptionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val optionText: TextView = view.findViewById(R.id.option_text)
        val removeButton: ImageButton = view.findViewById(R.id.remove_option_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollOptionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poll_option, parent, false)
        return PollOptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PollOptionViewHolder, position: Int) {
        holder.optionText.text = options[position]
        holder.removeButton.setOnClickListener {
            options.removeAt(position)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return options.size
    }
}
