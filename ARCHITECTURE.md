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
│ │ │ ├── DayCounterAdapter.kt
│ │ │ ├── DividerItemDecoration.kt
│ │ │ ├── KotlinExtensions.kt
│ │ │ ├── MainActivity.kt
│ │ │ ├── NewCounterActivity.kt
│ │ │ ├── RecyclerTouchListener.kt
│ │ │ └── SettingsActivity.kt
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
├── NewCounterActivity.kt ← Create/edit counter screen
├── SettingsActivity.kt ← Settings screen
├── DayCounterAdapter.kt ← RecyclerView adapter for activities
├── DividerItemDecoration.kt ← RecyclerView decorator
├── KotlinExtensions.kt ← Shared Kotlin extensions
└── RecyclerTouchListener.kt ← RecyclerView touch handling
```

| Type            | File                     | Path                                  |
|-----------------|--------------------------|---------------------------------------|
| Activity        | MainActivity.kt          | app/src/main/java/com/asptechinc/daymark/ |
| Activity        | NewCounterActivity.kt    | app/src/main/java/com/asptechinc/daymark/ |
| Activity        | SettingsActivity.kt      | app/src/main/java/com/asptechinc/daymark/ |
| Adapter         | DayCounterAdapter.kt     | app/src/main/java/com/asptechinc/daymark/ |
| Recycler helper | DividerItemDecoration.kt | app/src/main/java/com/asptechinc/daymark/ |
| Utilities       | KotlinExtensions.kt      | app/src/main/java/com/asptechinc/daymark/ |
| Touch helper    | RecyclerTouchListener.kt | app/src/main/java/com/asptechinc/daymark/ |
