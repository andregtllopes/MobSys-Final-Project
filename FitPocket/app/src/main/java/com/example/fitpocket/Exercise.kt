package com.example.fitpocket

/** Enum já é Serializable, então pode ir direto no putExtra (Tutorial 3, Ex. 3). */
enum class Exercise(val label: String) {
    PUSH_UP("Flexão"),
    SIT_UP("Abdominal")
}

/** Chaves dos extras da Intent, centralizadas para evitar erro de digitação. */
object Extras {
    const val EXERCISE = "exercise"
    const val GOAL = "goal"
    const val REPS = "reps"
    const val DURATION = "duration"
    const val SAVED = "saved"
}
