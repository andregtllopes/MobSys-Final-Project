package com.mobsys.moodsense

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobsys.moodsense.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter

    // Data comes back from DetailActivity (edits/deletes) and NewEntryActivity (new entries).
    private val detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadEntries()
    }
    private val newEntryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadEntries()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = HistoryAdapter(mutableListOf()) { entry ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra(DetailActivity.EXTRA_ENTRY_ID, entry.id)
            detailLauncher.launch(intent)
        }
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter

        binding.fabAddEntry.setOnClickListener {
            newEntryLauncher.launch(Intent(this, NewEntryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadEntries()
    }

    private fun loadEntries() {
        val entries = MoodRepository.getAll(this)
        adapter.updateData(entries)
        binding.textEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        binding.toolbar.subtitle = "${entries.size} entries"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
