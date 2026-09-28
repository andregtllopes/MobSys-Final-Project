package com.example.fitpocket

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitpocket.data.AppDatabase
import com.example.fitpocket.data.Workout
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class ProgressActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        val rv = findViewById<RecyclerView>(R.id.rvHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val tvStreak = findViewById<TextView>(R.id.tvStreak)
        val tvTotalReps = findViewById<TextView>(R.id.tvTotalReps)
        val tvTotalWorkouts = findViewById<TextView>(R.id.tvTotalWorkouts)
        val barChart = findViewById<BarChartView>(R.id.barChart)
        rv.layoutManager = LinearLayoutManager(this)
        rv.isNestedScrollingEnabled = false

        lifecycleScope.launch {
            val list = AppDatabase.get(this@ProgressActivity).workoutDao().getAll()

            tvTotalWorkouts.text = list.size.toString()
            tvTotalReps.text = list.sumOf { it.reps }.toString()
            tvStreak.text = currentStreak(list).toString()
            barChart.setData(weeklyChartData(list))

            if (list.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
            } else {
                tvEmpty.visibility = View.GONE
                rv.adapter = WorkoutAdapter(list)
            }
        }
    }

    private fun dayOf(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

    /** Dias consecutivos treinando até hoje. Se ainda não treinou hoje, conta a partir de ontem
     *  (o dia de hoje só "quebra" a sequência à meia-noite, não antes). */
    private fun currentStreak(workouts: List<Workout>): Int {
        val trainedDays = workouts.map { dayOf(it.timestamp) }.toSet()
        var cursor = LocalDate.now()
        if (!trainedDays.contains(cursor)) cursor = cursor.minusDays(1)
        var streak = 0
        while (trainedDays.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** Total de repetições por dia nos últimos 7 dias (incluindo hoje), para o gráfico de barras. */
    private fun weeklyChartData(workouts: List<Workout>): List<BarChartEntry> {
        val today = LocalDate.now()
        val repsByDay = workouts.groupBy { dayOf(it.timestamp) }
            .mapValues { (_, dayWorkouts) -> dayWorkouts.sumOf { it.reps } }

        return (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            BarChartEntry(
                label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                value = repsByDay[day] ?: 0,
                highlighted = day == today
            )
        }
    }
}
