package com.example.text2speech

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SpeechItem(val text: String)

class HistoryAdapter(
    private val items: List<SpeechItem>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.historyItemText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Truncate text for display if too long
        val displayText = if (item.text.length > 50) {
            item.text.substring(0, 50) + "..."
        } else {
            item.text
        }

        holder.textView.text = displayText
        holder.itemView.setOnClickListener {
            onItemClick(item.text)
        }
    }

    override fun getItemCount() = items.size
}

