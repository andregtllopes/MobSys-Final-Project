package com.mobsys.moodsense

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobsys.moodsense.databinding.ActivityNewEntryBinding
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Creates a new mood entry, or edits an existing one when launched with
 * [EXTRA_EDIT_ID] (data received from DetailActivity). Reads two sensors
 * live: TYPE_LIGHT and TYPE_ACCELEROMETER. Returns the saved entry to the
 * caller via [EXTRA_ENTRY] (data sent back through setResult).
 */
class NewEntryActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityNewEntryBinding
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var accelSensor: Sensor? = null

    private var currentLux = 0f
    private var currentLightCategory = "Unknown"
    private var motionEma = 0f
    private var currentMotionCategory = "Calm"

    private var editingId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        binding.moodDial.onMoodSelected = { mood ->
            binding.textMoodLabel.text = MoodEntry.MOOD_LABELS[mood - 1]
        }

        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "We'll remind you tomorrow", Toast.LENGTH_SHORT).show()
            }
        }

        editingId = intent.getLongExtra(EXTRA_EDIT_ID, -1L).takeIf { it != -1L }
        editingId?.let { id ->
            MoodRepository.getById(this, id)?.let { entry ->
                binding.toolbar.title = "Edit Entry"
                binding.moodDial.selectedMood = entry.moodLevel
                binding.textMoodLabel.text = MoodEntry.MOOD_LABELS[entry.moodLevel - 1]
                binding.ratingEnergy.rating = entry.energy.toFloat()
                binding.editNote.setText(entry.note)
            }
        }

        binding.buttonSave.setOnClickListener { saveEntry() }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            ?: run { binding.textLightValue.text = "Light sensor not available on this device" }
        accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            ?: run { binding.textMotionValue.text = "Motion sensor not available on this device" }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> {
                currentLux = event.values[0]
                currentLightCategory = when {
                    currentLux < 10 -> "Dark"
                    currentLux < 200 -> "Dim"
                    currentLux < 1000 -> "Bright"
                    else -> "Very Bright"
                }
                binding.progressLight.progress = currentLux.toInt().coerceIn(0, 100)
                binding.textLightValue.text = "%.0f lx · %s".format(currentLux, currentLightCategory)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val magnitude = sqrt(
                    event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]
                )
                val deviation = abs(magnitude - SensorManager.GRAVITY_EARTH)
                motionEma = motionEma * 0.8f + deviation * 0.2f
                currentMotionCategory = when {
                    motionEma < 0.3f -> "Calm"
                    motionEma < 1.5f -> "Active"
                    else -> "Very Active"
                }
                binding.progressMotion.progress = (motionEma * 20).toInt().coerceIn(0, 100)
                binding.textMotionValue.text = "%.2f m/s² · %s".format(motionEma, currentMotionCategory)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this use case.
    }

    private fun saveEntry() {
        val entry = MoodEntry(
            id = editingId ?: System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            moodLevel = binding.moodDial.selectedMood,
            energy = binding.ratingEnergy.rating.toInt().coerceAtLeast(1),
            note = binding.editNote.text.toString().trim(),
            lightLux = currentLux,
            lightCategory = currentLightCategory,
            motionCategory = currentMotionCategory
        )
        if (editingId != null) {
            MoodRepository.update(this, entry)
        } else {
            MoodRepository.add(this, entry)
        }
        Toast.makeText(this, "Entry saved", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK, Intent().putExtra(EXTRA_ENTRY, entry))
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_EDIT_ID = "extra_edit_id"
        const val EXTRA_ENTRY = "extra_entry"
    }
}
