package com.mobsys.moodsense

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mobsys.moodsense.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Demonstrates data flowing to NewEntryActivity and back via the ActivityResult API.
    private val newEntryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val entry = extractEntry(result.data)
            refreshSummary()
            entry?.let {
                Snackbar.make(
                    binding.root,
                    "Saved: ${MoodEntry.MOOD_LABELS[it.moodLevel - 1]} mood",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.buttonNewEntry.setOnClickListener {
            newEntryLauncher.launch(Intent(this, NewEntryActivity::class.java))
        }
        binding.buttonHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSummary()
    }

    private fun refreshSummary() {
        val entries = MoodRepository.getAll(this)
        if (entries.isEmpty()) {
            binding.textLastMoodEmoji.text = "🌱"
            binding.textLastMoodLabel.text = "No entries yet"
            binding.textLastMoodDetail.text = "Tap \"New Entry\" to log your first mood"
        } else {
            val latest = entries.first()
            binding.textLastMoodEmoji.text = MoodEntry.MOOD_EMOJI[latest.moodLevel - 1]
            binding.textLastMoodLabel.text = MoodEntry.MOOD_LABELS[latest.moodLevel - 1]
            val sdf = SimpleDateFormat("EEE, d MMM • HH:mm", Locale.getDefault())
            binding.textLastMoodDetail.text =
                "${sdf.format(latest.timestamp)} · ${latest.lightCategory} · ${latest.motionCategory}"
        }
        binding.textEntryCount.text = "${entries.size} entries logged"
    }

    @Suppress("DEPRECATION")
    private fun extractEntry(data: Intent?): MoodEntry? {
        if (data == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra(NewEntryActivity.EXTRA_ENTRY, MoodEntry::class.java)
        } else {
            data.getParcelableExtra(NewEntryActivity.EXTRA_ENTRY)
        }
    }
}
