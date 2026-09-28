package com.example.fitpocket

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Button
import android.widget.Chronometer
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Sensor 1: PROXIMIDADE  -> flexão (peito se aproxima do topo da tela)
 * Sensor 2: ACELERÔMETRO -> abdominal (inclinação do tronco)
 */
class TrainingActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val MIN_REP_INTERVAL_MS = 600L  // debounce: ignora "repetições" muito rápidas
        private const val UP_ANGLE = 40.0              // graus: passou disso = subiu
        private const val DOWN_ANGLE = 20.0            // graus: voltou abaixo disso = desceu (conta 1)
        private const val LOW_PASS = 0.85f             // filtro para isolar a gravidade
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var exercise: Exercise
    private lateinit var ring: ProgressRingView
    private lateinit var chrono: Chronometer
    private var sensor: Sensor? = null

    private var goal = 10
    private var reps = 0
    private var lastRepTime = 0L

    // estado da flexão
    private var wasNear = false

    // estado do abdominal
    private val gravity = floatArrayOf(0f, 0f, 9.8f)
    private var isUp = false

    // Volta da SummaryActivity: repassa o resultado para o menu e fecha
    private val summaryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            setResult(RESULT_OK, result.data)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        exercise = intent.getSerializableExtra(Extras.EXERCISE, Exercise::class.java)
            ?: Exercise.PUSH_UP
        goal = intent.getIntExtra(Extras.GOAL, 10)

        ring = findViewById(R.id.ring)
        chrono = findViewById(R.id.chrono)
        val tvExercise = findViewById<TextView>(R.id.tvExercise)
        val tvHint = findViewById<TextView>(R.id.tvHint)
        val btnManual = findViewById<Button>(R.id.btnManual)
        val btnFinish = findViewById<Button>(R.id.btnFinish)

        tvExercise.text = exercise.label
        tvHint.text = when (exercise) {
            Exercise.PUSH_UP ->
                "Deixe o celular no chão, tela para cima. Desça até o peito cobrir o topo da tela."
            Exercise.SIT_UP ->
                "Deite com o celular sobre o peito, tela para cima. Suba e desça o tronco."
        }
        ring.setProgress(reps, goal)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val type = when (exercise) {
            Exercise.PUSH_UP -> Sensor.TYPE_PROXIMITY
            Exercise.SIT_UP -> Sensor.TYPE_ACCELEROMETER
        }
        sensor = sensorManager.getDefaultSensor(type)
        if (sensor == null) {
            Snackbar.make(ring, "Sensor indisponível neste aparelho. Use o botão +1.", Snackbar.LENGTH_LONG).show()
        }

        chrono.base = SystemClock.elapsedRealtime()
        chrono.start()

        btnManual.setOnClickListener { addRep() }   // sem debounce, útil no emulador
        btnFinish.setOnClickListener {
            val duration = ((SystemClock.elapsedRealtime() - chrono.base) / 1000).toInt()
            val intent = Intent(this, SummaryActivity::class.java)
                .putExtra(Extras.EXERCISE, exercise)
                .putExtra(Extras.REPS, reps)
                .putExtra(Extras.GOAL, goal)
                .putExtra(Extras.DURATION, duration)
            summaryLauncher.launch(intent)
        }
    }

    // Registrar/liberar o sensor no ciclo de vida evita gastar bateria
    override fun onResume() {
        super.onResume()
        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> handleProximity(event)
            Sensor.TYPE_ACCELEROMETER -> handleTilt(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /** Flexão: conta na transição "longe -> perto" (peito desceu). */
    private fun handleProximity(event: SensorEvent) {
        val near = event.values[0] < event.sensor.maximumRange
        if (near && !wasNear) countRep()
        wasNear = near
    }

    /** Abdominal: ângulo entre o eixo Z do celular e a vertical; sobe (>40°) e desce (<20°) = 1 rep. */
    private fun handleTilt(event: SensorEvent) {
        for (i in 0..2) {
            gravity[i] = LOW_PASS * gravity[i] + (1 - LOW_PASS) * event.values[i]
        }
        val norm = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2])
        if (norm < 1f) return
        val angle = Math.toDegrees(acos((gravity[2] / norm).coerceIn(-1f, 1f).toDouble()))

        if (!isUp && angle > UP_ANGLE) {
            isUp = true
        } else if (isUp && angle < DOWN_ANGLE) {
            isUp = false
            countRep()
        }
    }

    private fun countRep() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRepTime < MIN_REP_INTERVAL_MS) return
        lastRepTime = now
        addRep()
    }

    private fun addRep() {
        reps++
        ring.setProgress(reps, goal)
        if (reps == goal) {
            Snackbar.make(ring, "Meta atingida! Toque em Finalizar.", Snackbar.LENGTH_LONG).show()
        }
    }
}
