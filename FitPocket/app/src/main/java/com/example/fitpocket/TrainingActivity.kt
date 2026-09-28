package com.example.fitpocket

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.Chronometer
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Sensor 1: PROXIMITY     -> push-up (chest approaching the top of the screen)
 * Sensor 2: ACCELEROMETER -> sit-up (torso tilt angle)
 */
class TrainingActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val MIN_REP_INTERVAL_MS = 600L  // debounce: ignora "repetições" muito rápidas
        private const val UP_ANGLE = 40.0              // graus: passou disso = subiu
        private const val DOWN_ANGLE = 20.0            // graus: voltou abaixo disso = desceu (conta 1)
        private const val LOW_PASS = 0.85f             // filtro para isolar a gravidade
        private const val TAG = "FitPocketSensor"
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var exercise: Exercise
    private lateinit var ring: ProgressRingView
    private lateinit var chrono: Chronometer
    private lateinit var progressIntensity: ProgressBar
    private lateinit var tvSensorDebug: TextView
    private var sensor: Sensor? = null

    private var goal = 10
    private var reps = 0
    private var lastRepTime = 0L

    // estado da flexão — calibrado dinamicamente, sem supor a unidade que o aparelho reporta
    // (alguns reportam distância em cm, outros só um valor binário 0/1)
    private var wasNear = false
    private var farBaseline = 0f
    private var baselineReady = false

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
        progressIntensity = findViewById(R.id.progressIntensity)
        tvSensorDebug = findViewById(R.id.tvSensorDebug)
        val tvExercise = findViewById<TextView>(R.id.tvExercise)
        val tvHint = findViewById<TextView>(R.id.tvHint)
        val btnManual = findViewById<Button>(R.id.btnManual)
        val btnFinish = findViewById<Button>(R.id.btnFinish)

        tvExercise.text = exercise.label
        tvHint.text = when (exercise) {
            Exercise.PUSH_UP ->
                "Place the phone on the floor, screen up. Lower your chest toward it."
            Exercise.SIT_UP ->
                "Lie down with the phone on your chest, screen up. Crunch up and down."
        }
        ring.setProgress(reps, goal)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val type = when (exercise) {
            Exercise.PUSH_UP -> Sensor.TYPE_PROXIMITY
            Exercise.SIT_UP -> Sensor.TYPE_ACCELEROMETER
        }
        sensor = sensorManager.getDefaultSensor(type)
        if (sensor == null) {
            Snackbar.make(ring, "Sensor unavailable on this device. Use the +1 button.", Snackbar.LENGTH_LONG).show()
        }
        Log.d(TAG, "exercise=$exercise sensor=$sensor maximumRange=${sensor?.maximumRange} resolution=${sensor?.resolution}")

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

    /**
     * Flexão: conta na transição "longe -> perto" (peito desceu).
     *
     * NÃO supomos a unidade do valor bruto: dependendo do aparelho, TYPE_PROXIMITY pode
     * reportar distância em cm (ex.: 0..5, 0..8) OU só um valor binário 0/1 — nesse segundo
     * caso, um limiar fixo tipo "< 3cm" trata os DOIS estados como "perto" e a detecção nunca
     * volta a "longe", travando a contagem (isso é o que provavelmente estava acontecendo).
     * Em vez de um número fixo, aprendemos ao vivo qual é o valor de "longe" neste aparelho e
     * comparamos de forma relativa a ele.
     */
    private fun handleProximity(event: SensorEvent) {
        val v = event.values[0]

        // Atualiza a referência de "longe" enquanto não estivermos perto (ou na primeira leitura).
        if (!baselineReady || (!wasNear && v > farBaseline)) {
            farBaseline = v
            baselineReady = true
        }
        // "Perto" = caiu abaixo de 60% do valor de "longe" observado neste aparelho (margem
        // generosa para aparelhos que não reportam um "perto" totalmente zerado).
        val threshold = (farBaseline * 0.6f).coerceAtLeast(0.05f)
        val near = v < threshold

        val intensity = if (farBaseline > 0f) {
            ((farBaseline - v) / farBaseline * 100f).coerceIn(0f, 100f)
        } else 0f
        progressIntensity.progress = intensity.toInt()
        tvSensorDebug.text = "debug: value=%.2f far=%.2f thr=%.2f".format(v, farBaseline, threshold)
        Log.d(TAG, "proximity value=$v far=$farBaseline threshold=$threshold near=$near wasNear=$wasNear")

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

        // Barra de intensidade: 0% deitado, 100% no topo do movimento (UP_ANGLE).
        val intensity = (angle / UP_ANGLE * 100.0).coerceIn(0.0, 100.0)
        progressIntensity.progress = intensity.toInt()
        tvSensorDebug.text = "debug: %.0f°".format(angle)

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
            Snackbar.make(ring, "Goal reached! Tap Finish.", Snackbar.LENGTH_LONG).show()
        }
    }
}
