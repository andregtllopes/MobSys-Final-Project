# MoodSense — MobSys 2026 Final Project

A small Android (Kotlin) mood-journal app: you pick a mood on a custom dial,
rate your energy, write a note, and the app automatically captures the
ambient **light level** and your **motion level** from two real device
sensors at the moment you log the entry. All entries are saved on-device and
browsable in a history list.

## How to open and run

1. Open the `MoodSense` folder (this folder) with **Android Studio 2024.x/2025.x** ("Open" → select this folder, not a sub-folder).
2. Let Gradle sync (needs an internet connection the first time, to download Gradle 8.7 / AGP 8.5.2 / the Kotlin & AndroidX libraries). If Android Studio reports the Gradle wrapper jar is missing, accept its offer to regenerate it (or `File > Sync Project with Gradle Files`) — this project intentionally ships without the binary `gradle-wrapper.jar` so it stays plain text.
3. Run the `app` configuration on an emulator (API 26+) or a physical phone.
   - The emulator does simulate the accelerometer, but usually **not** the light sensor — the app detects that and shows "Light sensor not available on this device" instead of crashing. On a real phone both sensors work.
4. This project was **not compiled in this environment** (no Android SDK / emulator available here) — please build it once in Android Studio to confirm, and take the screenshots required for the submission PDF from that run.

## Where this maps to the assignment (`Final_projects_2026.pdf`)

| Requirement | Where |
|---|---|
| Kotlin, Android Studio, API 34 target (min 26, runs through 16/API 36) | `app/build.gradle.kts` |
| ≥ 3 Activities | `MainActivity`, `NewEntryActivity`, `HistoryActivity`, `DetailActivity` (4 total) |
| Data passed to next Activity **and back** (Intents) | Main → NewEntry → back (`EXTRA_ENTRY` via `setResult`); History → Detail → back (`EXTRA_ENTRY_ID` down, `EXTRA_DELETED` / edit result up) |
| CustomView | `MoodDialView.kt` — Canvas-drawn, touch-driven circular mood picker, reused (read-only) on the Detail screen |
| RecyclerView | `HistoryActivity` + `HistoryAdapter` + `item_mood_entry.xml` |
| Two sensors, read & used | `NewEntryActivity`: `Sensor.TYPE_LIGHT` (ambient lux → Dark/Dim/Bright) and `Sensor.TYPE_ACCELEROMETER` (motion magnitude → Calm/Active/Very Active) |
| UI widget diversity | Toolbar, MaterialButton, RatingBar, ProgressBar (x2), SwitchMaterial, EditText, CardView, FloatingActionButton, RecyclerView |
| Data storage | `MoodRepository.kt` — entries persisted as JSON in `SharedPreferences` |

## Project structure

```
MoodSense/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mobsys/moodsense/
│       │   ├── MainActivity.kt
│       │   ├── NewEntryActivity.kt
│       │   ├── HistoryActivity.kt
│       │   ├── DetailActivity.kt
│       │   ├── HistoryAdapter.kt
│       │   ├── MoodDialView.kt      (CustomView)
│       │   ├── MoodEntry.kt         (Parcelable data model)
│       │   └── MoodRepository.kt    (SharedPreferences storage)
│       └── res/ (layouts, colors, theme, launcher icon)
├── build.gradle.kts
└── settings.gradle.kts
```

## Building without Android Studio (cloud build)

This repo includes `.github/workflows/android-build.yml`, a GitHub Actions
workflow that compiles a debug APK entirely on GitHub's servers — no local
Android SDK/Studio install needed. Steps:

1. Create a free account at github.com if you don't have one.
2. Create a new **empty** repository (no README/.gitignore templates).
3. Get this `MoodSense` folder's contents into that repository. Easiest way:
   install **GitHub Desktop** (a small, lightweight app, not Android Studio),
   sign in, clone the empty repo to a local folder, copy everything from this
   `MoodSense` folder into it (including the hidden `.github` folder), then
   use the "Commit" and "Push origin" buttons in GitHub Desktop.
   (Alternative with zero installs: on the repo's GitHub page, use
   "uploading an existing file" and drag the `app`, `gradle`, `.github`
   folders and the root files in from Windows Explorer.)
4. On GitHub, open the repo's **Actions** tab — the "Android Build" workflow
   starts automatically after the push and takes a few minutes.
5. Once it finishes (green check), open the run, scroll to **Artifacts**,
   and download `MoodSense-debug-apk` (a zip containing `app-debug.apk`).
6. Get that `.apk` onto the Samsung phone (e.g. upload it to Google Drive
   from the PC, then open Drive on the phone and download it) and tap it to
   install — Android will warn it's from an "unknown developer" since it's
   an unsigned debug build; that's expected for testing your own app, allow
   the install when prompted.

## Preparing the submission (per slide 4 of the PDF)

The PDF asks for a **separate 1–4 page PDF** (title, participants + Matrikelnummer,
what the app does, screenshots with short captions, notes) plus the project
files as a `.rar`/zip, sent by email with an accompanying no-attachment email.
This repo gives you the working app; you still need to:

1. Run the app in Android Studio and take 3–5 screenshots (dashboard, mood
   dial in use, history list, detail screen).
2. Fill in your name + Matrikelnummer and paste the screenshots into a short
   PDF (I can draft that write-up text for you if you'd like — just say so
   and give me the names/Matrikelnummern to include).
3. `File > Export > Export to Zip File...` in Android Studio (or zip this
   folder after deleting `app/build/` and `.gradle/` if present) and re-pack
   as `.rar` if a `.rar` is strictly required.
