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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/** Reads the accelerometer and counts reps for each exercise. */
class TrainingActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val MIN_REP_INTERVAL_MS = 600L  // ignore reps that are too close together
        private const val UP_ANGLE = 40.0              // degrees: past this = sit-up is up
        private const val DOWN_ANGLE = 20.0            // degrees: below this = sit-up is down
        private const val LOW_PASS = 0.85f             // low-pass filter to isolate gravity

        // Motion-peak thresholds per exercise, tuned from testing.
        private const val MOTION_EMA_ALPHA = 0.5f
        private const val PUSH_UP_RISE = 2.5f
        private const val PUSH_UP_FALL = 0.8f
        private const val SQUAT_RISE = 3.0f
        private const val SQUAT_FALL = 1.0f
        private const val JACK_RISE = 9.0f
        private const val JACK_FALL = 3.0f

        // Minimum rest time before a rep counts as finished. Push-ups and squats have a brief
        // pause mid-movement that would otherwise get counted as a second rep.
        private const val BODYWEIGHT_MIN_REST_MS = 350L
        private const val JACK_MIN_REST_MS = 0L
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var exercise: Exercise
    private lateinit var ring: ProgressRingView
    private lateinit var chrono: Chronometer
    private lateinit var progressIntensity: ProgressBar
    private var sensor: Sensor? = null

    private var goal = 10
    private var reps = 0
    private var lastRepTime = 0L

    // sit-up state
    private val gravity = floatArrayOf(0f, 0f, 9.8f)
    private var isUp = false

    // push-up/squat/jumping jack state (motion-peak detection)
    private var motionIntensityEma = 0f
    private var motionPeak = false
    private var motionRestSinceMs = 0L

    // Returns from SummaryActivity, forwards the result to the menu, and closes.
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
        val tvExercise = findViewById<TextView>(R.id.tvExercise)
        val tvHint = findViewById<TextView>(R.id.tvHint)
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
            Snackbar.make(ring, "Sensor unavailable on this device.", Snackbar.LENGTH_LONG).show()
        }

        chrono.base = SystemClock.elapsedRealtime()
        chrono.start()

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

    // Register/unregister with the activity lifecycle to save battery.
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

    /** Sit-up: angle between the phone's Z axis and vertical. Up past 40°, down below 20° = 1 rep. */
    private fun handleTilt(event: SensorEvent) {
        for (i in 0..2) {
            gravity[i] = LOW_PASS * gravity[i] + (1 - LOW_PASS) * event.values[i]
        }
        val norm = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2])
        if (norm < 1f) return
        val angle = Math.toDegrees(acos((gravity[2] / norm).coerceIn(-1f, 1f).toDouble()))

        // Intensity bar: 0% lying down, 100% at the top of the movement.
        val intensity = (angle / UP_ANGLE * 100.0).coerceIn(0.0, 100.0)
        progressIntensity.progress = intensity.toInt()

        if (!isUp && angle > UP_ANGLE) {
            isUp = true
        } else if (isUp && angle < DOWN_ANGLE) {
            isUp = false
            countRep()
        }
    }

    /**
     * Push-up, squat and jumping jack: counts by movement intensity instead of angle. Rises
     * above [riseThreshold] = movement started; drops below [fallThreshold] and stays there
     * for at least [minRestMs] = rep finished.
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
                motionRestSinceMs = 0L // still moving, cancel the rest timer
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
