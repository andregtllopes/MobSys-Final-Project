package com.example.fitpocket.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(workout: Workout)

    @Query("SELECT * FROM workouts ORDER BY timestamp DESC")
    suspend fun getAll(): List<Workout>
}
