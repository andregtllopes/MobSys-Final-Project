package com.mobsys.moodsense

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple persistence layer backed by SharedPreferences (entries serialized as a JSON array).
 */
object MoodRepository {

    private const val PREFS_NAME = "moodsense_prefs"
    private const val KEY_ENTRIES = "entries"

    fun getAll(context: Context): MutableList<MoodEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return mutableListOf()
        val array = JSONArray(raw)
        val result = mutableListOf<MoodEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                MoodEntry(
                    id = obj.getLong("id"),
                    timestamp = obj.getLong("timestamp"),
                    moodLevel = obj.getInt("moodLevel"),
                    energy = obj.getInt("energy"),
                    note = obj.getString("note"),
                    lightLux = obj.getDouble("lightLux").toFloat(),
                    lightCategory = obj.getString("lightCategory"),
                    motionCategory = obj.getString("motionCategory")
                )
            )
        }
        return result.sortedByDescending { it.timestamp }.toMutableList()
    }

    fun getById(context: Context, id: Long): MoodEntry? = getAll(context).find { it.id == id }

    fun add(context: Context, entry: MoodEntry) {
        val entries = getAll(context)
        entries.add(entry)
        save(context, entries)
    }

    fun update(context: Context, entry: MoodEntry) {
        val entries = getAll(context)
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) {
            entries[index] = entry
        } else {
            entries.add(entry)
        }
        save(context, entries)
    }

    fun delete(context: Context, id: Long) {
        val entries = getAll(context)
        entries.removeAll { it.id == id }
        save(context, entries)
    }

    private fun save(context: Context, entries: List<MoodEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("timestamp", entry.timestamp)
            obj.put("moodLevel", entry.moodLevel)
            obj.put("energy", entry.energy)
            obj.put("note", entry.note)
            obj.put("lightLux", entry.lightLux.toDouble())
            obj.put("lightCategory", entry.lightCategory)
            obj.put("motionCategory", entry.motionCategory)
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }
}
