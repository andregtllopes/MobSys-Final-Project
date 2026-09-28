# FitPocket

A pocket workout tracker: push-ups, sit-ups, squats and jumping jacks, counted automatically by
the phone's accelerometer, with a Canvas-drawn progress ring, a gamified Progress screen (streak,
total reps, weekly bar chart), and workouts saved locally with Room.

This project is a fully set up Gradle project (`build.gradle.kts`, `settings.gradle.kts`, the
`Theme.FitPocket` theme, launcher icon, and KSP/Room already configured) and is built
automatically by GitHub Actions (`.github/workflows/android-build.yml`, job `build-fitpocket`).

## Opening it

Open this `FitPocket` folder directly in Android Studio (2024.x/2025.x or newer) and let Gradle
sync — minSdk 34, so an emulator or phone on Android 14+ is required to run it.

## How each exercise is detected

- **Sit-up**: angle between the phone's Z axis and vertical (phone on the chest).
- **Push-up, Squat, Jumping Jack**: accelerometer motion intensity (peaks above/below a
  threshold), with the phone held or kept snug against the body — see the in-app hint on each
  exercise's screen for exact placement.
