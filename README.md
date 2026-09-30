# School Planner

A joyful Android school planner: Today / Tomorrow schedule, homework and exam planner, week grid,
parent-protected timetable setup, three selectable skins (Galactic Droid, Master Builder, Alien Scribe),
haptics + synthesised sound effects, exam / morning reminders and JSON backup & restore.

* Kotlin, Jetpack Compose (Material 3), Room, DataStore, WorkManager, AlarmManager, SoundPool
* `minSdk 24` (Android 7.0 – runs on the Galaxy S8 and newer), `targetSdk 34`

## Getting the APK (no Android Studio needed)

1. Push this repo to GitHub. The **Build APK** workflow (`.github/workflows/build-apk.yml`) runs on every push.
2. Open the repo on GitHub → **Actions** → the latest **Build APK** run → **Artifacts** → download
   `school-planner-debug-apk` and unzip it to get `app-debug.apk`.
3. Copy the APK to the phone and open it. Android will ask you to allow "Install unknown apps" for the app you
   opened it from (Files / Chrome). Accept, then install.

You can also start a build manually: **Actions → Build APK → Run workflow**.

The debug APK is signed with the standard Android debug key, which is fine for personal use.
To update the app later, install the newer APK over the old one (same debug key = data is kept)
*as long as it is built by GitHub's runner or the same machine*; if the key ever changes, use
**Parent area → Backup & reset** first.

## Building locally

```
./gradlew assembleDebug        # output: app/build/outputs/apk/debug/app-debug.apk
```

Needs JDK 17 and the Android SDK (platform 34).

## First run

1. Tap the lock icon → create a parent PIN.
2. In the parent area add **Subjects**, the **Bell schedule**, then fill the **Timetable**
   (or tap *Load an example timetable* to try the app immediately).
3. Kids can add homework and exams from the **Planner** tab without a PIN.

## Notes

* Animations (starfield, confetti, mascot, ink rings) are drawn with Compose Canvas, and the sound effects are
  synthesised at first launch, so there are no Lottie or audio assets to bundle.
* Attached homework photos are stored inside the app and are not part of the JSON backup.
* Exact exam alarms use `AlarmManager.setExactAndAllowWhileIdle`; on Android 12+ they fall back to
  inexact alarms if the exact-alarm permission is not granted.
