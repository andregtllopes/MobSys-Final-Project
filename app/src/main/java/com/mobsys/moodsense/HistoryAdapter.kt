package com.mobsys.moodsense

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobsys.moodsense.databinding.ItemMoodEntryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private var entries: MutableList<MoodEntry>,
    private val onItemClick: (MoodEntry) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val moodColors = intArrayOf(
        Color.parseColor("#E53935"),
        Color.parseColor("#FB8C00"),
        Color.parseColor("#FDD835"),
        Color.parseColor("#7CB342"),
        Color.parseColor("#43A047")
    )
    private val sdf = SimpleDateFormat("EEE, d MMM yyyy • HH:mm", Locale.getDefault())

    inner class ViewHolder(val binding: ItemMoodEntryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMoodEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val b = holder.binding
        b.textEmoji.text = MoodEntry.MOOD_EMOJI[entry.moodLevel - 1]
        b.circleBackground.backgroundTintList = ColorStateList.valueOf(moodColors[entry.moodLevel - 1])
        b.textTitle.text = MoodEntry.MOOD_LABELS[entry.moodLevel - 1]
        b.textTimestamp.text = sdf.format(entry.timestamp)
        b.textNotePreview.text = entry.note.ifBlank { "(no note)" }
        b.ratingEnergy.rating = entry.energy.toFloat()
        b.textContext.text = "${entry.lightCategory} · ${entry.motionCategory}"
        holder.itemView.setOnClickListener { onItemClick(entry) }
    }

    override fun getItemCount(): Int = entries.size

    fun updateData(newEntries: List<MoodEntry>) {
        entries = newEntries.toMutableList()
        notifyDataSetChanged()
    }
}
