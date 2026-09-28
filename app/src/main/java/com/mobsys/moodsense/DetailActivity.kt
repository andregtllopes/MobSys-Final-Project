package com.mobsys.moodsense

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mobsys.moodsense.databinding.ActivityDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Receives an entry id from HistoryActivity ([EXTRA_ENTRY_ID]) and sends
 * back whether the entry was deleted ([EXTRA_DELETED]) — the "round trip"
 * data transfer between activities.
 */
class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private var entryId: Long = -1L

    private val editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            loadEntry()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.moodDial.isEditable = false

        entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L)
        if (entryId == -1L) {
            finish()
            return
        }
        loadEntry()

        binding.buttonEdit.setOnClickListener {
            val intent = Intent(this, NewEntryActivity::class.java)
            intent.putExtra(NewEntryActivity.EXTRA_EDIT_ID, entryId)
            editLauncher.launch(intent)
        }
        binding.buttonDelete.setOnClickListener { confirmDelete() }
    }

    private fun loadEntry() {
        val entry = MoodRepository.getById(this, entryId)
        if (entry == null) {
            finish()
            return
        }
        binding.moodDial.selectedMood = entry.moodLevel
        binding.textMoodLabel.text = MoodEntry.MOOD_LABELS[entry.moodLevel - 1]
        binding.ratingEnergy.rating = entry.energy.toFloat()
        binding.textNote.text = entry.note.ifBlank { "(no note)" }
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy • HH:mm", Locale.getDefault())
        binding.textTimestamp.text = sdf.format(entry.timestamp)
        binding.textLightDetail.text = "Light: %.0f lx (%s)".format(entry.lightLux, entry.lightCategory)
        binding.textMotionDetail.text = "Motion: ${entry.motionCategory}"
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete entry?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                MoodRepository.delete(this, entryId)
                setResult(RESULT_OK, Intent().putExtra(EXTRA_DELETED, true))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
        const val EXTRA_DELETED = "extra_deleted"
    }
}
