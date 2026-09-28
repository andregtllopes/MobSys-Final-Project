package com.example.fitpocket

enum class Exercise(val label: String) {
    PUSH_UP("Push-up"),
    SIT_UP("Sit-up"),
    SQUAT("Squat"),
    JUMPING_JACK("Jumping Jack")
}

/** Intent extra keys. */
object Extras {
    const val EXERCISE = "exercise"
    const val GOAL = "goal"
    const val REPS = "reps"
    const val DURATION = "duration"
    const val SAVED = "saved"
}
