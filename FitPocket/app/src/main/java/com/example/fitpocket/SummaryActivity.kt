package com.example.fitpocket

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.fitpocket.data.AppDatabase
import com.example.fitpocket.data.Workout
import kotlinx.coroutines.launch

class SummaryActivity : AppCompatActivity() {

    private lateinit var ratingEffort: RatingBar
    private lateinit var etNote: EditText
    private lateinit var tvCounter: TextView
    private lateinit var btnSave: Button
    private var effort = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        // "Abrir a mochila": Activity de destino só lê o que veio na Intent
        val exercise = intent.getSerializableExtra(Extras.EXERCISE, Exercise::class.java)
            ?: Exercise.PUSH_UP
        val reps = intent.getIntExtra(Extras.REPS, 0)
        val goal = intent.getIntExtra(Extras.GOAL, 0)
        val duration = intent.getIntExtra(Extras.DURATION, 0)

        findViewById<TextView>(R.id.tvExercise).text = exercise.label
        findViewById<TextView>(R.id.tvReps).text = "$reps of $goal reps"
        findViewById<TextView>(R.id.tvTime).text =
            "Time: ${duration / 60}:${"%02d".format(duration % 60)}"

        ratingEffort = findViewById(R.id.ratingEffort)
        etNote = findViewById(R.id.etNote)
        tvCounter = findViewById(R.id.tvCounter)
        btnSave = findViewById(R.id.btnSave)

        // Só habilita salvar depois que o usuário avaliou o esforço
        ratingEffort.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                effort = rating
                btnSave.isEnabled = rating > 0f
            }
        }

        etNote.doOnTextChanged { text, _, _, _ ->
            tvCounter.text = "${text?.length ?: 0}/120"
        }

        btnSave.setOnClickListener {
            btnSave.isEnabled = false
            val workout = Workout(
                timestamp = System.currentTimeMillis(),
                exercise = exercise.name,
                reps = reps,
                goal = goal,
                durationSec = duration,
                effort = effort,
                note = etNote.text.toString().trim()   // .text.toString(), nunca o EditText direto
            )
            lifecycleScope.launch {
                AppDatabase.get(this@SummaryActivity).workoutDao().insert(workout)
                // devolve o resultado para a Activity anterior
                setResult(RESULT_OK, Intent().putExtra(Extras.SAVED, true))
                finish()
            }
        }
    }
}
