# Daymark

## Table of Contents

- [Overview](#overview)
- [Features](#features)
    - [Future features](#future-features)
- [Screenshots](#screenshots)
- [Requirements](#requirements)
- [Setup and Installation](#setting-up-and-running-the-kotlin-android-app)
    - [Install Java](#1-install-java-jdk)
    - [Install Android Studio](#2-install-android-studio)
    - [Configure Android SDK](#3-configure-android-sdk-paths)
- [Running the Application](#running-the-application)
- [Testing the Application](#testing-the-application)
- [Project Structure](#project-structure)
- [Development Workflow](#development-workflow)
    - [Code Formatting](#code-formatting)
    - [Android Studio Settings](#android-studio-settings)
- [Gradle Tools](#code-quality-gradle-tools)
    - [Ktlint](#ktlint)
- [Application Usage](#application-usage)
    - [Creating a Day Counter](#creating-a-day-counter)
    - [Editing Items](#editing-items)
    - [Item Actions](#item-actions)
    - [Search, Filter and Sort](#search-filter-and-sort)
    - [Future features](#future-features)
- [Data Storage](#data-storage)
- [Contributing](#contributing)
- [Licence](#licence)
- [Support](#support)
- [Connect with me](#connect-with-me)

## Overview

Daymark is a free open source (FOSS) Android application written in Kotlin that counts the number of
days since
or until a given date. It is [MIT licence](#licence).

It builds on the
original [Count The Days Android application](https://github.com/sreich/android-count-the-days)
created by [sreich](https://github.com/sreich) as a fork that preserves the foundation of the
original project while introducing enhancements.

---

## Features

See the [Application usage](#application-usage) section for a detailed list of features.

|   |                                                                                                                                       |
|---|:-------------------------------------------------------------------------------------------------------------------------------------:|
| ✅ |                             **Create day activities** for activities, events, milestones, and reminders.                              |
| ✅ |                             **Optional end dates** to track date ranges in addition to single-day events.                             |
| ✅ |              **Relative time display** showing elapsed or remaining time (for example, *3 weeks ago* or *in 2 months*).               |
| ✅ |               **Exact start and end dates** displayed using a human-friendly format (for example, *1st August, 2026*).                |
| ✅ |                                          **Edit existing activities** with a single app tap.                                          |
|   | **Quick item actions** via the item menu or long press: <br>✅ Edit <br>✅ Duplicate <br>✅ Reset <br>✅ Delete <br>✅ Archive <br>✅ Share |
| ✅ |                                                **Search** activities by activity name.                                                |
| ✅ |                                           **Filter** activities by category or start month.                                           |
| ✅ |                                         **Sort** activities alphabetically by activity name.                                          |
| ✅ |                                                **Categories** to organise activities.                                                 |
| ✅ |                             **Persistent storage**, automatically saving activities between app launches.                             |
| ✅ |                                      **Material 3 interface** with support for dynamic theming.                                       |
| ✅ |                             **Sample activities** on first launch to demonstrate the app's functionality.                             |
| ✅ |                              **RecyclerView-based list** for smooth scrolling and efficient performance.                              |

### Future features

These can be added without separate list manipulation logic:

- Active/completed activities
- Date ranges
- Categories
- Archived items
- Favourites
- Tags

- Search items
- Rearrange items
- Add end countdate
- Sort items by title, date inputted, manual
- View things in terms of days, months, weeks,
- Change theme, dark theme,material

Core counting improvements (high value)

- Count between two dates
- Show total days, weeks, months, and years between start and end.

Inclusive / exclusive toggle

- Let users choose whether to include the start/end day.

Multiple activities

- Track several events at once (e.g. “Since quitting”, “Until deadline”).

Pause / resume counter

- Useful for temporary events.

Timezone-safe counting

- Avoid day shifts when users travel or DST changes.

Display & UX improvements

- Progress indicator
- Percentage bar or circular progress when an end date exists.

- Human-readable summaries
  e.g. “3 months, 12 days (104 days total)”

- Calendar view...Visualise the range on a mini calendar.

- Colour-coded activities...Different colours for different events.

- Reorder / pin activities....Keep important ones at the top.

Notifications & reminders

- Milestone notifications
  e.g. day 7, 30, 100, 365.

- Custom reminders
  “Notify me every 10 days” or “1 week before end date”.

- End-date alert...Notify when the counter finishes.

Data & persistence

- Backup & restore (local or cloud)
  JSON export/import (great for FOSS).

- CSV / text export...for tracking progress.

- Offline-first design
- No account required.

Widgets & quick access (Android-friendly)

- Home screen widget
- Live day count without opening the app.
- Quick-add counter shortcut

Accessibility & polish

- Dark / AMOLED mode
- Screen reader–friendly labels

Power-user / nice-to-have features

- Tags / categories like health, work, personal, faith, etc.

- Privacy lock...App PIN / biometric.
- No tracking / no ads (clearly stated)
- Translation support (crowdsourced)
- Count between two dates
- Multiple activities
- Progress indicator
- Milestone notifications
- Backup/export

---

## Screenshots

|                       **TBC**                       |                        **TBC**                        |                       **TBC**                       |                       **TBC**                        |
|:---------------------------------------------------:|:-----------------------------------------------------:|:---------------------------------------------------:|:----------------------------------------------------:|
| <img src="assets/images/image-place-holder.png"  /> | <img src="assets/images/image-place-holder.png"    /> | <img src="assets/images/image-place-holder.png"  /> | <img src="assets/images/image-place-holder.png"   /> |

|                       **TBC**                       |                        **TBC**                        |                       **TBC**                       |                       **TBC**                        |
|:---------------------------------------------------:|:-----------------------------------------------------:|:---------------------------------------------------:|:----------------------------------------------------:|
| <img src="assets/images/image-place-holder.png"  /> | <img src="assets/images/image-place-holder.png"    /> | <img src="assets/images/image-place-holder.png"  /> | <img src="assets/images/image-place-holder.png"   /> |

|                       **TBC**                       |                        **TBC**                        |                       **TBC**                       |                       **TBC**                        |
|:---------------------------------------------------:|:-----------------------------------------------------:|:---------------------------------------------------:|:----------------------------------------------------:|
| <img src="assets/images/image-place-holder.png"  /> | <img src="assets/images/image-place-holder.png"    /> | <img src="assets/images/image-place-holder.png"  /> | <img src="assets/images/image-place-holder.png"   /> |

---

## Requirements

- Kotlin
- Java 17+
- Shared Preferences

---

## Setting up and running the Kotlin Android app

### 1. Install Java (JDK)

Android development requires a Java Development Kit.

Recommended:

- Install the JDK bundled with Android Studio or
- Install a standalone JDK (17+ recommended)

Verify:

```bash
java -version
```

### 2. Install Android Studio

Download and install Android Studio:

https://developer.android.com/studio

During setup, ensure these are installed:

- Android SDK
- Android SDK Platform Tools
- Android Emulator
- Android SDK Build Tools

Open the project in Android Studio and allow Gradle to sync.

### 3. Configure Android SDK paths

Add the Android SDK environment variables.

Example for Linux/macOS:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator
```

Verify:

```bash
adb --version
```

## Running the application

#### Using Android Studio:

1. Clone the project repository

  ```bash
    git clone https://github.com/Sherida101/Daymark.git
  ``` 

> **Push an existing repository from the command line
>
>
> or git remote set-url origin https://github.com/Sherida101/Daymark.git
>
> git branch -M main
>
> git push -u origin main

> **Create a new repository on the command line
>
> echo "# Daymark" >> README.md
>
>
> git init
>
> git add README.md
>
> git commit -m "first commit"
>
> git branch -M main
>
> git remote add origin https://github.com/Sherida101/Daymark.git
>
> git push -u origin main

2. Open the project.
3. Wait for Gradle sync to complete.
4. Select an emulator or connected Android device.
5. Click Run ▶.

#### Using the terminal:

```bash
./gradlew assembleDebug

./gradlew installDebug
adb shell monkey -p com.asptechinc.daymark 1
```

#### Using VSCode `launch.json`

##### Install VS Code extensions

- [Kotlin (by JetBrains or the Kotlin Language extension)](https://marketplace.visualstudio.com/items?itemName=fwcd.kotlin)
- [Extension Pack for Java (Microsoft)](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

##### Launch app

1. Open VS Code command palette using `Ctrl` + `Shift` + `P`.

   Then, click **Debug: Select and Start Debugging** or click the dropdown menu next to the green ▶
   Run button (upper-left)

2. Pick any of the following launch configurations from the list:

    - **Run Android app (emulator)**

      > This calls the **Launch Android app (emulator)** task, which will start the emulator and run
      the app.

    - **Run Android app (connected device)**

      > This calls the **Launch Android app (connected device)** task, which will run the app on a
      connected device.

    - **Run Android app (automatically detect device)**
      > This calls the **Launch Android app (auto)** task, which will run the app on an
      automatically detected device.

    - **Stop Android emulator**

      > This calls the **Stop Android emulator** task, which will stop the running emulator.

3. To stop the server, press `Ctrl` + `C` in the terminal or click the red square stop button in the
   debug panel.

---

## Testing the application

``` bash
./gradlew :app:testDebugUnitTest
```

---

## Project Structure

See [architecture.md](architecture.md) for a detailed overview of the project structure.

---

## Development workflow

### Code formatting

Reformat code:

- Windows/Linux: `Ctrl + Alt + L`
- macOS: `⌥ + ⌘ + L`

### Android Studio Settings

#### Enable automatic imports

Go to: `Settings` → `Editor` → `General` → `Auto Import`

Enable:

- Optimise imports on the fly
- Add unambiguous imports automatically

#### Format code on save

Go to: `Settings` → `Tools` → `Actions on Save`

Enable:

- Reformat code
- Optimise imports

### Code quality gradle tools

This project uses **Ktlint** - Kotlin formatting.

#### Ktlint

The plugin is added using the Gradle plugin system.

Define the plugin and version in: `gradle/libs.versions.toml`

Example:

```TOML
[versions]
ktlint = "14.2.0"

[plugins]
kotlin-ktlint = {
    id = "org.jlleitschuh.gradle.ktlint",
    version.ref = "ktlint"
}
```

Reference it in the root: `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.ktlint) apply false
}
```

Apply it in: `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.ktlint)
}
```

Run:

```bash
./gradlew ktlintCheck
```

Automatically fix formatting:

```bash
./gradlew ktlintFormat
```

---

## Application Usage

### Creating a Day Counter

### Editing Items

- Tap item → Edit the activity

### Item Actions

- Long press item → Show quick actions:
    - Delete
    - Reset
    - Duplicate
    - Other item actions

- Ellipsis (⋮) on item → Opens the same item action menu

### Search, Filter and Sort

Search, filtering, and sorting are handled through a single list update flow.

---

## Data storage

Data is stored using Shared Preferences, which is a simple key-value storage mechanism provided by
Android.

---

## Testing

Tests are located in the [test](./app/src/test) directory

---

## Contributing

Contributions are welcome! Please read the [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how
to contribute to this project.

If you find any issues or have suggestions, feel free to open an issue.


---

## Licence

The project is open-sourced under the [MIT Licence](LICENSE). You can use, modify and distribute
this project as long as you include the original licence.


---

## Support

![developer](https://img.shields.io/badge/Developed%20By%20%3A-ASPTechnologies%20Incorporations-blue) | [!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://buymeacoffee.com/asptechinc)

If you like this project, please consider supporting the developer.
There are no advertisements nor in-app purchases.

Your support will help keep the project free and updated. Thank you!

Star ⭐ the repository if you like what you see 😉.

## Connect with Me

[<img align="center" alt="Sherida101 | GitHub" src="https://img.shields.io/badge/GitHub-Repository-181717?logo=github" />](https://github.com/Sherida101)
&ensp;GitHub: [@Sherida101](https://github.com/Sherida101 'GitHub Sherida101')