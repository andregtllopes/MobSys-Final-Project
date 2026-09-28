package com.example.fitpocket

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class MenuActivity : AppCompatActivity() {

    private lateinit var root: View
    private lateinit var goalGroup: RadioGroup

    // Cards/linhas clicáveis, não androidx.widget.Button — mantém a hierarquia visual:
    // os exercícios são ações primárias (cards grandes), Histórico é uma ação secundária
    // discreta no topo, não um botão do mesmo peso visual.
    private lateinit var btnPushUp: View
    private lateinit var btnSitUp: View
    private lateinit var btnSquat: View
    private lateinit var btnJumpingJack: View
    private lateinit var btnHistory: View

    // Volta da TrainingActivity (que por sua vez recebeu o resultado da SummaryActivity)
    private val trainingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val saved = result.data?.getBooleanExtra(Extras.SAVED, false) == true
        if (result.resultCode == RESULT_OK && saved) {
            Snackbar.make(root, "Workout saved to history!", Snackbar.LENGTH_LONG)
                .setAction("View") {
                    startActivity(Intent(this, HistoryActivity::class.java))
                }
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        root = findViewById(R.id.menuRoot)
        goalGroup = findViewById(R.id.goalGroup)
        btnPushUp = findViewById(R.id.btnPushUp)
        btnSitUp = findViewById(R.id.btnSitUp)
        btnSquat = findViewById(R.id.btnSquat)
        btnJumpingJack = findViewById(R.id.btnJumpingJack)
        btnHistory = findViewById(R.id.btnHistory)

        btnPushUp.setOnClickListener { startTraining(Exercise.PUSH_UP) }
        btnSitUp.setOnClickListener { startTraining(Exercise.SIT_UP) }
        btnSquat.setOnClickListener { startTraining(Exercise.SQUAT) }
        btnJumpingJack.setOnClickListener { startTraining(Exercise.JUMPING_JACK) }
        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Animação de entrada: botões sobem e aparecem em sequência
        listOf(btnPushUp, btnSitUp, btnSquat, btnJumpingJack, btnHistory).forEachIndexed { i, button ->
            button.alpha = 0f
            button.translationY = 80f
            button.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(150L * i)
                .start()
        }
    }

    private fun startTraining(exercise: Exercise) {
        val goal = when (goalGroup.checkedRadioButtonId) {
            R.id.goal10 -> 10
            R.id.goal20 -> 20
            else -> 30
        }
        val intent = Intent(this, TrainingActivity::class.java)
            .putExtra(Extras.EXERCISE, exercise)
            .putExtra(Extras.GOAL, goal)
        trainingLauncher.launch(intent)
    }
}
