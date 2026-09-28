package com.example.fitpocket.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val exercise: String,   // nome do enum Exercise (PUSH_UP / SIT_UP)
    val reps: Int,
    val goal: Int,
    val durationSec: Int,
    val effort: Float,      // 1..5, vindo da RatingBar
    val note: String
)
