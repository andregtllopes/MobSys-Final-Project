package com.mobsys.moodsense

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MoodEntry(
    val id: Long,
    val timestamp: Long,
    val moodLevel: Int,
    val energy: Int,
    val note: String,
    val lightLux: Float,
    val lightCategory: String,
    val motionCategory: String
) : Parcelable {

    companion object {
        val MOOD_LABELS = arrayOf("Awful", "Low", "Okay", "Good", "Great")
        val MOOD_EMOJI = arrayOf("😢", "🙁", "😐", "🙂", "😄")
    }
}
