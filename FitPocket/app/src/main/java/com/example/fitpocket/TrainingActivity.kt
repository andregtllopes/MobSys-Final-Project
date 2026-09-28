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
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Todos os exercícios usam o ACCELEROMETER, com duas técnicas diferentes:
 *  - Sit-up: ângulo de inclinação do tronco (handleTilt).
 *  - Push-up, Squat, Jumping Jack: pico de intensidade de movimento (handleMotionPeak).
 *
 * O push-up inicialmente usava o sensor de PROXIMITY (celular no chão, peito cobrindo a tela),
 * mas o alcance de detecção "perto" de vários aparelhos (Samsung incluso) é curto demais para
 * captar um peito a poucos centímetros de distância — só reagia a pressão/contato direto no
 * sensor, o que é inviável durante um push-up real. Trocamos para a mesma técnica de movimento
 * do squat, com o celular preso nas costas/peito em vez de apoiado no chão.
 */
class TrainingActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val MIN_REP_INTERVAL_MS = 600L  // debounce: ignora "repetições" muito rápidas
        private const val UP_ANGLE = 40.0              // graus: passou disso = subiu
        private const val DOWN_ANGLE = 20.0            // graus: voltou abaixo disso = desceu (conta 1)
        private const val LOW_PASS = 0.85f             // filtro para isolar a gravidade
        private const val TAG = "FitPocketSensor"

        // Push-up, squat e polichinelo contam por PICO de intensidade de movimento (desvio da
        // aceleração em relação à gravidade), não por ângulo. Valores iniciais estimados a
        // partir do padrão de movimento de cada exercício; a barra + valor de debug na tela
        // ajudam a recalibrar se necessário.
        private const val MOTION_EMA_ALPHA = 0.5f
        private const val PUSH_UP_RISE = 2.5f
        private const val PUSH_UP_FALL = 0.8f
        private const val SQUAT_RISE = 3.0f
        private const val SQUAT_FALL = 1.0f
        private const val JACK_RISE = 9.0f
        private const val JACK_FALL = 3.0f

        // Push-up e squat são movimentos lentos, controlados, com uma pausa natural no meio
        // (embaixo do agachamento / no fundo do push-up) — sem essa margem, essa pausa faz o
        // sinal cair momentaneamente abaixo do limiar de "repouso" e a repetição é contada
        // 2x (descida + subida como dois picos separados). Exigimos que o sinal fique baixo
        // por essa duração mínima antes de considerar que a repetição realmente terminou.
        // Polichinelo é um movimento rápido e contínuo, não precisa dessa margem.
        private const val BODYWEIGHT_MIN_REST_MS = 350L
        private const val JACK_MIN_REST_MS = 0L
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

    // estado do abdominal
    private val gravity = floatArrayOf(0f, 0f, 9.8f)
    private var isUp = false

    // estado de push-up/agachamento/polichinelo (detecção por pico de movimento)
    private var motionIntensityEma = 0f
    private var motionPeak = false
    private var motionRestSinceMs = 0L

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
                "Keep the phone snug against your chest or upper back (e.g. in a shirt pocket) so it moves with you."
            Exercise.SIT_UP ->
                "Lie down with the phone on your chest, screen up. Crunch up and down."
            Exercise.SQUAT ->
                "Hold the phone in your hand or front pocket. Squat down and stand back up."
            Exercise.JUMPING_JACK ->
                "Hold the phone in your hand. Jump your feet apart and back together."
        }
        ring.setProgress(reps, goal)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
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
        when (exercise) {
            Exercise.PUSH_UP -> handleMotionPeak(event, PUSH_UP_RISE, PUSH_UP_FALL, BODYWEIGHT_MIN_REST_MS)
            Exercise.SIT_UP -> handleTilt(event)
            Exercise.SQUAT -> handleMotionPeak(event, SQUAT_RISE, SQUAT_FALL, BODYWEIGHT_MIN_REST_MS)
            Exercise.JUMPING_JACK -> handleMotionPeak(event, JACK_RISE, JACK_FALL, JACK_MIN_REST_MS)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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

    /**
     * Push-up, agachamento e polichinelo: contam por PICO de intensidade de movimento, não por
     * ângulo. Usamos a mesma ideia de histerese do abdominal (handleTilt), só que aplicada à
     * magnitude da aceleração: passa de [riseThreshold] = "em movimento"; cai abaixo de
     * [fallThreshold] e fica assim por pelo menos [minRestMs] = "voltou ao repouso de verdade",
     * conta 1 repetição.
     *
     * O [minRestMs] existe porque push-up/agachamento têm uma pausa natural no meio do
     * movimento (embaixo do agachamento, no fundo do push-up): sem essa margem, o sinal cai
     * momentaneamente abaixo do limiar nessa pausa e a repetição é contada 2x (descida e subida
     * como dois picos separados). Polichinelo é rápido e contínuo, não precisa da margem (usa 0).
     */
    private fun handleMotionPeak(event: SensorEvent, riseThreshold: Float, fallThreshold: Float, minRestMs: Long) {
        val magnitude = sqrt(
            event.values[0] * event.values[0] +
                event.values[1] * event.values[1] +
                event.values[2] * event.values[2]
        )
        val deviation = abs(magnitude - SensorManager.GRAVITY_EARTH)
        motionIntensityEma = motionIntensityEma * (1 - MOTION_EMA_ALPHA) + deviation * MOTION_EMA_ALPHA

        val intensity = (motionIntensityEma / riseThreshold * 100f).coerceIn(0f, 100f)
        progressIntensity.progress = intensity.toInt()
        tvSensorDebug.text = "debug: %.2f (rise=%.1f fall=%.1f)".format(motionIntensityEma, riseThreshold, fallThreshold)
        Log.d(TAG, "motion ema=$motionIntensityEma rise=$riseThreshold fall=$fallThreshold peak=$motionPeak")

        val now = SystemClock.elapsedRealtime()
        if (!motionPeak) {
            if (motionIntensityEma > riseThreshold) {
                motionPeak = true
                motionRestSinceMs = 0L
            }
        } else {
            if (motionIntensityEma < fallThreshold) {
                if (motionRestSinceMs == 0L) motionRestSinceMs = now
                if (now - motionRestSinceMs >= minRestMs) {
                    motionPeak = false
                    countRep()
                }
            } else {
                motionRestSinceMs = 0L // ainda em movimento, cancela a confirmação de repouso
            }
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
