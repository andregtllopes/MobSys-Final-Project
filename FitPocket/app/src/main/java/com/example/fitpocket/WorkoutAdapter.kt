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

/** Contrato do clique (padrão de delegação do Tutorial 5): o Adapter só avisa, a Activity decide. */
fun interface OnWorkoutClickListener {
    fun onWorkoutClick(workout: Workout)
}

class WorkoutAdapter(
    private val items: List<Workout>,
    private val listener: OnWorkoutClickListener
) : RecyclerView.Adapter<WorkoutAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDetail: TextView = view.findViewById(R.id.tvDetail)
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
        holder.itemView.setOnClickListener { listener.onWorkoutClick(w) }
    }

    override fun getItemCount() = items.size
}
