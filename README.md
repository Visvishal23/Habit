# HabitFlow

A modern, offline-first Android habit tracker built with Kotlin, Jetpack Compose, Material 3, and Room.

No account, no login, no cloud sync, no analytics, no ads, no internet permission requested. Every feature — habit tracking, streaks, calendar heatmaps, journal with photos, reminders, statistics, and backup/restore — works fully offline, including in Airplane Mode.

## Features

- Home dashboard with today's progress, List View and Streak View
- Create/edit habits: icon, color, category, frequency (daily / specific weekdays / X times per week), numeric or simple goals
- Schedule-aware streak engine (current streak, best streak, completion rate, weekly/monthly consistency)
- Full calendar with a GitHub-style completion heatmap, tap any day to correct history
- Personal journal per habit: text + mood + photos (stored on-device only, via the Android Photo Picker)
- Local reminders via `AlarmManager` (survive reboot via a boot receiver)
- Statistics dashboard: Today / Week / Month / Year, a 7-day bar chart, and per-habit consistency
- Home-screen "Today's Habits" widget (Glance), togglable straight from the widget
- Light / Dark / System theme, with Material You dynamic color on Android 12+
- Local JSON backup & restore via Android's file picker (photos embedded as base64, so it's one portable file)

## Opening the project

1. Install **Android Studio** (Koala or newer recommended).
2. Choose **Open** and select this `HabitFlow/` folder.
3. Let Android Studio sync Gradle — it will download the Android Gradle Plugin, Kotlin, and the dependencies listed in `app/build.gradle.kts` automatically.
4. Run the `app` configuration on an emulator or physical device (minSdk 26 / Android 8.0+).

There's no `gradlew` wrapper checked in, since this project was generated without network access to fetch the wrapper binary — Android Studio's own bundled Gradle will handle the sync/build the first time you open it. If you'd rather use the command line, run `gradle wrapper` once inside the project to generate one, then use `./gradlew assembleDebug` as usual.

## Project structure

```
app/src/main/java/com/habitflow/app/
  data/local/          Room entities, DAOs, database, converters
  data/repository/      Repository layer (habits, journal, reminders, settings, backup)
  domain/               StreakCalculator - all streak/consistency math lives here
  ui/                    One package per screen (home, createhabit, habitdetail, calendar,
                          statistics, journal, settings, onboarding), plus shared components/ and theme/
  notifications/         AlarmManager scheduling + BroadcastReceivers for local reminders
  widget/                Glance home-screen widget
```

Data flows one way: Room → Repository (`Flow`) → ViewModel (`StateFlow`) → Compose UI. There's no dependency-injection framework — `ViewModelFactory` wires each screen's ViewModel from the repositories held on `HabitFlowApplication`, which keeps the whole data layer easy to trace in one pass.

## Verifying it's truly offline

The manifest never requests the `INTERNET` permission, so the app is incapable of making network calls at the OS level — you can confirm this yourself in Android's Settings → Apps → HabitFlow → Permissions, or by testing in Airplane Mode from first launch onward.

## Notes on scope

This is a complete, original implementation inspired by the feature set described in the brief — all branding, icons (a built-in emoji set), color palette, and code are original. A few areas were kept intentionally simple so the whole project stays easy to read and extend:

- Backup/restore uses a single JSON file (with photos embedded as base64) rather than a JSON+ZIP pair — same offline, no-cloud guarantee, one less moving part.
- The statistics bar chart is a small custom Canvas composable rather than a third-party charting library, to avoid an extra dependency.
- "Custom schedule" frequency is modeled the same as "X times per week" for now — a natural spot to extend if you want more elaborate rules later.
