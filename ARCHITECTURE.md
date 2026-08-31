# Project Structure

This file documents the current layout of the Daymark repository as an Android application built
with Kotlin and Gradle.

```markdown
daymark/
├── app/ ← Android application module
│ ├── build.gradle ← App-level Gradle configuration
│ ├── proguard-rules.pro ← ProGuard rules
│ └── src/
│ ├── androidTest/ ← Instrumentation tests
│ ├── main/
│ │ ├── java/com.asptechinc/daymark/ ← Kotlin source files
│ │ │ ├── ActivityAdapter.kt
│ │ │ ├── MainActivity.kt
│ │ │ ├── NewActivity.kt
│ │ │ ├── SettingsActivity.kt
│ │ │ ├── AppLockActivity.kt
│ │ │ └── ui/
│ │ │     ├── MainViewModel.kt
│ │ │     └── TagSelectionAdapter.kt
│ │ ├── res/ ← Android resources
│ │ │ ├── drawable/
│ │ │ ├── layout/
│ │ │ ├── menu/
│ │ │ ├── mipmap/
│ │ │ └── values/
│ │ └── AndroidManifest.xml
│ └── test/ ← Local unit tests
├── build.gradle ← Root Gradle build file
├── gradle/ ← Gradle wrapper files and version catalogue
├── gradle.properties ← Project-wide Gradle settings
├── gradlew ← Unix Gradle wrapper
├── gradlew.bat ← Windows Gradle wrapper
├── settings.gradle ← Gradle module settings
├── README.md ← Project overview and setup instructions
├── CONTRIBUTING.md ← Contribution guidelines
├── LICENSE ← Licence information
├── CHANGELOG.md ← Project changelog
└── tools/ ← Helper scripts
```

## Android app package layout

```markdown
app/src/main/java/com/asptechinc/daymark/
├── MainActivity.kt ← Main screen UI and app entry flow
├── NewActivity.kt ← Create/edit activity screen
├── SettingsActivity.kt ← Settings screen
├── ActivityAdapter.kt ← ListAdapter for activities
├── AppLockActivity.kt ← PIN entry/setup screen
└── ui/
    ├── MainViewModel.kt ← Business logic and state
    └── TagSelectionAdapter.kt ← Searchable tag selection

| Type            | File                     | Path                                  |
|-----------------|--------------------------|---------------------------------------|
| Activity        | MainActivity.kt          | app/src/main/java/com/asptechinc/daymark/ |
| Activity        | NewActivity.kt           | app/src/main/java/com/asptechinc/daymark/ |
| Activity        | SettingsActivity.kt      | app/src/main/java/com/asptechinc/daymark/ |
| Adapter         | ActivityAdapter.kt       | app/src/main/java/com/asptechinc/daymark/ |
| ViewModel       | MainViewModel.kt         | app/src/main/java/com/asptechinc/daymark/ui/ |
| Adapter         | TagSelectionAdapter.kt   | app/src/main/java/com/asptechinc/daymark/ui/ |
