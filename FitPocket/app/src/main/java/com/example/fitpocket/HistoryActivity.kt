package com.example.fitpocket

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitpocket.data.AppDatabase
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val rv = findViewById<RecyclerView>(R.id.rvHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        rv.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val list = AppDatabase.get(this@HistoryActivity).workoutDao().getAll()
            if (list.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
            } else {
                rv.adapter = WorkoutAdapter(list) { workout ->
                    val msg = if (workout.note.isBlank()) "No note for this workout." else workout.note
                    Snackbar.make(rv, msg, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}
