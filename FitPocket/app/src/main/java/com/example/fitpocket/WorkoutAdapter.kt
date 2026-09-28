package com.example.fitpocket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitpocket.data.Workout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutAdapter(
    private val items: List<Workout>
) : RecyclerView.Adapter<WorkoutAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDetail: TextView = view.findViewById(R.id.tvDetail)
        val tvNote: TextView = view.findViewById(R.id.tvNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val w = items[position]
        val label = Exercise.valueOf(w.exercise).label
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(w.timestamp))
        val time = "${w.durationSec / 60}:${"%02d".format(w.durationSec % 60)}"

        holder.tvTitle.text = "$label — ${w.reps}/${w.goal} reps"
        holder.tvDetail.text = "$date · $time · effort ${w.effort.toInt()}/5"

        // Nota exibida direto no card, sem precisar de clique/Snackbar para revelar.
        if (w.note.isBlank()) {
            holder.tvNote.visibility = View.GONE
        } else {
            holder.tvNote.visibility = View.VISIBLE
            holder.tvNote.text = "“${w.note}”"
        }
    }

    override fun getItemCount() = items.size
}
