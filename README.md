<a id="top"></a>

# Daymark

![Continuous Integration](https://github.com/ASPTechInc/Daymark/actions/workflows/ci.yml/badge.svg)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Screenshots](#screenshots)
- [Requirements](#requirements)
- [Setup and installation](#setting-up-and-running-the-kotlin-android-app)
- [Running the application](#running-the-application)
- [Testing the application](#testing-the-application)
- [Releasing the application](#releasing-the-application)
- [Application usage](#application-usage)
- [F-Droid Metadata](#f-droid-metadata)
- [Contributing](#contributing)
- [Licence](#licence)
- [Support](#support)

---
[⬇️ Go to bottom](#bottom)

## Overview

Daymark is a free open source (FOSS) Android application written in Kotlin that counts the number of
days since or until a given date for events. It is released under the [MIT licence](#licence).

It builds on the original
[Count The Days Android application](https://github.com/sreich/android-count-the-days)
created by [sreich](https://github.com/sreich) by preserving the foundation of the
original project while introducing modern enhancements like Material 3, widgets and advanced
notifications.

---

## Features

|   | Feature                   | Description                                                                         |
|---|:--------------------------|:------------------------------------------------------------------------------------|
| ✅ | **Activity tracking**     | Create day counters for activities, events, milestones and reminders.               |
| ✅ | **Date range support**    | Optional end dates to track durations in addition to single-day events.             |
| ✅ | **Relative time display** | View time as "3 weeks ago" or "in 2 months" based on your preferences.              |
| ✅ | **Home screen widgets**   | **1x1 Quick add** for fast entry and **4x4 list** to view all counters at a glance. |
| ✅ | **Rich notifications**    | Get notified on your device the moment an activity reaches its end date.            |
| ✅ | **Data sovereignty**      | Export your data to human-readable **CSV** or technical **JSON** formats.           |
| ✅ | **Flexible viewing**      | Switch between **List** and **Grid** layouts to suit your style.                    |
| ✅ | **Privacy lock**          | Protect your data with an optional **App PIN** (between 4 and 16 digits).           |
| ✅ | **Organisation**          | Use **Categories** and **Tags** to keep your activities organised.                  |
| ✅ | **Reorder & sort**        | Manually **drag-and-drop** activities or sort them alphabetically.                  |
| ✅ | **Quick item actions**    | Edit, duplicate, share, archive and delete via simple menus.                        |
| ✅ | **Modern UI**             | Full **Material 3** interface with support for **Dynamic Colour** and Dark Mode.    |
| ✅ | **Offline-first**         | No tracking, no ads and no internet connection required for core features.          |

### Future features

- **Translation support**: Community-driven localisations for non-English speakers.

---

## Screenshots

### Dark mode

|                       **TBC**                       |                        **TBC**                        |                       **TBC**                       |                       **TBC**                        |
|:---------------------------------------------------:|:-----------------------------------------------------:|:---------------------------------------------------:|:----------------------------------------------------:|
| <img src="assets/images/image-place-holder.png"  /> | <img src="assets/images/image-place-holder.png"    /> | <img src="assets/images/image-place-holder.png"  /> | <img src="assets/images/image-place-holder.png"   /> |

### Light mode

|                       **TBC**                       |                        **TBC**                        |                       **TBC**                       |                       **TBC**                        |
|:---------------------------------------------------:|:-----------------------------------------------------:|:---------------------------------------------------:|:----------------------------------------------------:|
| <img src="assets/images/image-place-holder.png"  /> | <img src="assets/images/image-place-holder.png"    /> | <img src="assets/images/image-place-holder.png"  /> | <img src="assets/images/image-place-holder.png"   /> |

---

## Requirements

- Kotlin
- Java 17+
- Room database (SQLite)

---

## Setting up and running the Kotlin Android app

### 1. Install Java (JDK)

Recommended: Install a standalone JDK (17+ recommended) or use the one bundled with Android Studio.

```bash
java -version
```

### 2. Install Android Studio

Download and install Android Studio from https://developer.android.com/studio. Ensure the Android
SDK, Platform Tools and Emulator are selected during setup.

### 3. Configure Android SDK paths (optional)

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

---

## Running the application

### Using Android Studio

1. Clone the repository: `git clone https://github.com/ASPTechInc/Daymark.git`
2. Open the project and wait for Gradle sync to complete.
3. Select a device and click **Run ▶**.

### Using the command line

```bash
./gradlew assembleDebug
./gradlew installDebug
adb shell monkey -p com.asptechinc.daymark 1
```

### Using VS Code launch.json

VS Code can be downloaded from https://code.visualstudio.com/download.

#### Install VS Code extensions

- [Kotlin (by JetBrains or the Kotlin Language extension)](https://marketplace.visualstudio.com/items?itemName=fwcd.kotlin)
- [Extension Pack for Java (Microsoft)](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

#### Launch app

1. Open VS Code command palette using `Ctrl` + `Shift` + `P`.

   Then, click **Debug: Select and Start Debugging** or click the dropdown menu next to the green ▶
   Run button (upper-left)

2. Pick any of the following launch configurations from the list:
    - **Run Android app (emulator)**

      > This calls the **Launch Android app (emulator)** task, which will start the emulator and run
      > the app.

    - **Run Android app (connected device)**

      > This calls the **Launch Android app (connected device)** task, which will run the app on a
      > connected device.

    - **Run Android app (automatically detect device)**

      > This calls the **Launch Android app (auto)** task, which will run the app on an
      > automatically detected device.

    - **Stop Android emulator**

      > This calls the **Stop Android emulator** task, which will stop the running emulator.

3. To stop the server, press `Ctrl` + `C` in the terminal or click the red square stop button in the
   debug panel.

---

## Testing the application

```bash
./gradlew :app:testDebugUnitTest     # Local unit tests
./gradlew connectedDebugAndroidTest  # Instrumented device tests

# Check for warnings (optional)
./gradlew :app:help --warning-mode all
```

---

## Releasing the application

This project uses GitHub Actions for automated releases. Pushing a tag matching `v*` to the `main`
branch will trigger a build and create a draft release with signed APKs.

---

## Application usage

### Home screen widgets

Daymark provides two widgets to keep your milestones visible without opening the app:

- **Quick add (1x1)**: Tapping this opens the app directly to the "New Activity" screen.
- **Activity list (4x4)**: Displays a scrollable list of your active counters with their relative
  time.

### Creating & Editing activities

- Click the **Floating action button (+)** to create a new activity.
- Enter a name, optional notes and choose a start date.
- Use the **Ellipsis (⋮)** on any activity card to access the **Edit** screen.

### Activity actions

- **Long Press**: activates reorder mode. Drag items to change their position in the list.
- **Menu Options**:
    - **Duplicate**: creates a copy of an existing activity.
    - **Share**: exports the details of an activity as plain text to other apps.
    - **Archive**: Hide completed activities from the main list.
    - **Reset**: Set the start date to "Now" and clear the end date.

### Search & Filtering

Use the top toolbar to find specific activities:

- **Search**: Search by activity name.
- **Filter**: Narrow down the list by category, month, year or status (archived or completed).
- **Sort**: Toggle between manual reordering and alphabetical sorting.

### Notifications

You can enable or disable notifications for activities with end dates in **Settings > General**. If
enabled, Daymark will alert you the moment an activity reaches its scheduled completion.

---

## Data storage & Backup

- **JSON backup**: A full technical backup of your database, perfect for restoring data on a new
  device.
- **CSV export**: A human-readable export that allows you to view your data in spreadsheet software
  like Google Sheets or LibreOffice.
- **Privacy**: All data remains on your device. Daymark does not use cloud storage or third-party
  analytics.

---

## F-Droid Metadata

This project includes [Fastlane](https://docs.fastlane.tools/getting-started/android/metadata/)
compatible metadata. This ensures that the app's listing, descriptions, and changelogs are
maintained directly within source control for FOSS repository compatibility.

Location: `fastlane/metadata/android/en-GB/`

---

## Resources

*Naming convention for resource files*

- **`activity_`**: Entire screen layouts.
- **`fragment_`**: Entire fragment screen
- **`item_`**: Individual list item layouts.
- **`view_`**: Reusable UI components.
- **`row_`**: One row in a list
- **`dialogue_`**: Custom dialogue layouts.

---

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on code
style, testing, and PR submission.

For useful notes, refer to [DEVELOPER_NOTES.md](docs/DEVELOPER_NOTES.md)

---

## Licence

The project is open-source under the [MIT Licence](LICENSE).

---

## Support

Developed by [**ASPTechnologies Incorporation**](https://github.com/ASPTechInc 'GitHub ASPTechInc').
If you find Daymark useful,
consider [Supporting the Developer](https://www.buymeacoffee.com/asptechinc). Daymark is and will
always be ad-free.

[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://buymeacoffee.com/asptechinc)

[🔝 Back to top](#top)

<a id="bottom"></a>
