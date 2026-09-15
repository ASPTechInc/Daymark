## Table of Contents

- [Overview](#overview)
- [Project structure](#project-structure)
- [Data storage notes](#data-storage-notes)
- [Development workflow](#development-workflow)
    - [Code formatting](#code-formatting)
    - [Code analysis](#code-analysis)
    - [Gradle tools](#gradle-tools)
- [Updating app version notes](#updating-app-version-notes)
- [Releasing the application notes](#releasing-the-application-notes)
- [Resizing app logo for Android compatibility](#resizing-app-logo-for-android-compatibility)
    - [Creating the adaptive icon structure](#create-adaptive-icon-structure)
    - [Using GIMP to resize the logo](#using-gimp-to-resize-the-logo)
- [Test GitHub workflows locally](#testing-github-workflows-locally)
- [Stop tracking files in Git](#stop-tracking-files-in-git)

---

## Overview

This section entails information that the developer found useful while creating the application. It
complements what is documented in the [README](/README.md).

---

## Project structure

See [ARCHITECTURE.md](/ARCHITECTURE.md) for a visual breakdown of the project.

---

## Data storage notes

Data is stored using both SharedPreferences and Room database.

Room is a robust SQLite abstraction provided by Android that is used for the application's primary,
structured data entities.

SharedPreferences is a key-value storage used for application
settings and user preferences like storing and retrieving theme mode index and storing app lock

---

## Development workflow

### Code formatting

Reformat code:

- Windows/Linux: `Ctrl + Alt + L`
- macOS: `⌥ + ⌘ + L`

### Android Studio settings

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

### Code analysis

#### Using Android Studio (IDE)

This is the most user-friendly way to see real-time results and navigate to issues:

1. Go to the top menu and select Analyze > Inspect Code....

2. Select the scope (e.g., Whole project or Module 'app').

3. Click OK. Android Studio will run its internal inspections and display a list of warnings,
   errors and suggestions in the "Problems" or "Inspection Results" tool window.

#### Using Gradle (Command Line)

This is useful for a comprehensive check that includes Android's "Lint" tool:

```bash
./gradlew lintDebug
```

Once finished, it will generate an HTML report (usually at
app/build/reports/lint-results-debug.html) which you can open in a browser to see detailed analysis.

### Gradle tools

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

## Updating app version notes

To update the app to a higher version:

1. Insert a new entry in the [CHANGELOG.md](/CHANGELOG.md) for the new app version.
2. In [app's build.gradle](/app/build.gradle) file, increment `versionCode` to the next number
   and update `versionName` with the new version of the app.
3. Add a text file to the `changelogs` directory for the Fastlane metadata in
   [fastlane/metadata/android/en-GB/changelogs](/fastlane/metadata/android/en-GB/changelogs). The
   name of the text file should be a number higher than the current number of the existing text
   file.

---

## Releasing the application notes

A keystore is used to store the signing key required for Android app releases.

### Create a keystore file

> Replace `keystore.jks` and `key-alias` with your desired values.

```bash
keytool -genkeypair \
  -v \
  -keystore keystore.jks \
  -alias key-alias \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### Encode the keystore file

The decoding of the file occurs in the GitHub repository workflow.

> Replace `keystore.jks` and `keystore.base64` with your file names.

```bash
base64 -i keystore.jks | tr -d '\n' > keystore.base64
```

### Update the example keystore file in the project

Rename the example keystore file [keystore.properties.example](/app/keystore.properties.example)
to `keystore.properties`. Then, update the values of the variables - `storeFile`, `storePassword`,
`keyAlias` and `keyPassword` with the values used above.

### Create environment secrets in GitHub repository

The following secrets should be created. Their values should match the values used above.

```txt
KEY_ALIAS
KEY_PASSWORD
KEYSTORE_CONTENT
KEYSTORE_FILE_NAME
KEYSTORE_PASSWORD
```

### Create app release

Push a tag matching the pattern `v*` to the `main` branch. The
[release.yml](/.github/workflows/release.yml) GitHub workflow will then automatically build the
release APK and bundle, sign them using the provided keystore and upload the artefacts.

Example tag for a release:

```bash
git checkout main
git tag v1.0.0
git push origin v1.0.0
```

#### Rebuild an existing tag

> Replace v1.0 with the actual tag version. Ensure that all commits has been pushed to GitHub
> before running the command below so that the tag would include the latest changes

```bash
git tag -f v1.0 && git push origin v1.0 --force
```

---

## Resizing app logo for Android compatibility

### Create adaptive icon structure

The **`ic_launcher.xml` and `ic_launcher_round.xml` are the adaptive-icon definitions**, while the
actual white background and padded logo live separately in your project.

A typical structure is:

```text
app/
└── src/
    └── main/
        └── res/
            │
            ├── drawable/
            │   └── ic_launcher_foreground.png   ← or a XML file
            │
            ├── mipmap-anydpi-v26/
            │   ├── ic_launcher.xml
            │   └── ic_launcher_round.xml
            │
            └── mipmap-.../
            │    └── legacy PNG icons like mipmap-mdpi/ic_launcher.png etc
            │
            ├── values/
            │   └── colours.xml    ← includes a colour resource for ic_launcher_background
```

#### Step 1. Create `ic_launcher.xml`

Create:

```text
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
```

Put this inside:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

---

#### Step 2. Create `ic_launcher_round.xml`

Create:

```text
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
```

Put the **same thing** inside it as the previous step:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

The distinction is that Android may use `ic_launcher` for the normal icon and `ic_launcher_round`
where a launcher specifically requests the round variant.

---

#### Step 3. Create a white background for the app icon

Create:

```text
app/src/main/res/values/colours.xml
```

Place this inside of it:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#FFFFFF</color>
</resources>
```

So:

```text
@color/ic_launcher_background
          │
          ▼
    #FFFFFF
```

This gives the adaptive icon a **white background**.

---

#### Step 4. The foreground i.e. the logo with transparent padding around it

Create:

```text
app/src/main/res/drawable/ic_launcher_foreground.png
```

For example:

```text
res/
└── drawable/
    └── ic_launcher_foreground.png
```

That PNG should contain **your logo with transparent space around it**.

Refer to [Using GIMP to resize the logo](#using-gimp-to-resize-the-logo) to properly size your logo
within the 512 × 512 PNG.

For example, imagine a PNG that is 512 × 512.

Don't make the logo occupy all 512 × 512:

```text
┌─────────────────────┐
│█████████████████████│
│█████████████████████│
│█████████████████████│
│█████████████████████│
└─────────────────────┘
```

Instead, make the actual logo smaller to about 350 x 350 size:

```text
┌─────────────────────┐
│                     │
│                     │
│       ███████       │
│      █████████      │
│       ███████       │
│                     │
│                     │
└─────────────────────┘
```

The transparent area is intentional.

Then:

```xml

<foreground android:drawable="@drawable/ic_launcher_foreground" />
```

means:

> Take this image and put it on top of the white background.

---

#### Step 5. What about the existing `mipmap-*` PNGs?

This is the part that can be confusing.

You may currently have:

```text
mipmap-mdpi/ic_launcher.png
mipmap-hdpi/ic_launcher.png
mipmap-xhdpi/ic_launcher.png
mipmap-xxhdpi/ic_launcher.png
mipmap-xxxhdpi/ic_launcher.png
```

Those are **legacy launcher icons**.

The adaptive icon:

```text
mipmap-anydpi-v26/ic_launcher.xml
```

is used on **Android 8.0/API 26 and newer**.

The `-v26` is significant:

```text
mipmap-anydpi-v26
              ↑
         Android 8+
```

For older Android versions, the PNGs are used.

So if you want your application to look correct on **older Android versions too**, you should
update the legacy PNGs as well.

But you don't necessarily need to manually create five different logos.

You can generate the appropriate density PNGs from your source artwork.

---

#### Step 6. Important note: Adaptive icons have a "safe zone"

Android adaptive icons aren't simply:

```text
┌───────────────┐
│               │
│     LOGO      │
│               │
└───────────────┘
```

The launcher can apply different masks:

```text
       square
    ┌───────────┐
    │           │
    │   LOGO    │
    │           │
    └───────────┘

       circle
       ╭───────╮
      /         \
     |   LOGO    |
      \         /
       ╰───────╯

    rounded square
    ╭───────────╮
    │           │
    │   LOGO    │
    │           │
    ╰───────────╯
```

The launcher controls the final mask.

Therefore, your logo needs sufficient padding so that it remains visually comfortable under
different masks.

---

### Using GIMP to resize the logo

A 512×512 size logo or of any other size can be resized with GIMP while keeping the canvas at
512×512 while making the actual logo smaller by adding transparent padding around it. It can be done
using GIMP.

#### Expected result

Start with:

```text
512 × 512 PNG
┌──────────────────────────────┐
│                              │
│      ██████████████████      │
│      ██████████████████      │
│      ██████████████████      │
│                              │
└──────────────────────────────┘
```

Make it something like:

```text
512 × 512 PNG
┌──────────────────────────────┐
│                              │
│                              │
│          ████████            │
│          ████████            │
│          ████████            │
│                              │
│                              │
└──────────────────────────────┘
```

The area around the logo remains **transparent**.

Then Android puts that foreground over the white background.

#### Step 1. Open your logo in GIMP

In GIMP:

**File → Open**

Select your 512×512 PNG.

First check:

**Image → Image Properties**

You should see:

```text
Width: 512 px
Height: 512 px
```

---

#### Step 2. Make sure the image has transparency

Look at the Layers panel.

Right-click your logo layer.

If you see:

**Add Alpha Channel**

click it.

If you instead see:

**Remove Alpha Channel**

then the layer already has transparency.

---

#### Step 3. Duplicate the original layer

Before changing anything, duplicate the layer to have a backup if anything goes wrong:

**Layer → Duplicate Layer**

You'll now have something like:

```text
Layers
────────────────────
Logo copy       ← work on this
Logo            ← original backup
────────────────────
```

You can hide the original layer by clicking its eye icon.

---

#### Step 4. Scale the logo itself using Layer

Select the logo layer and use:

**Layer → Scale Layer**

You'll get a dialogue.

If your current logo layer is 512×512, you can change it to something smaller.
350 px x 350 px is the recommended resize option.

For example:

```text
Width: 350 px
Height: 350 px
```

Make sure the **chain/link icon is locked**, so the aspect ratio stays correct.

Then click:

**Scale**

Now your logo layer is 350×350.

---

#### Step 5. Centre the smaller logo

Use:

**Layer → Layer to Image Size**

This makes the layer's canvas 512×512 again while preserving the smaller logo.

Then use:

**Alignment Tool**

or manually position the logo in the centre.

An easier method in recent GIMP versions is:

**Tools → Transform Tools → Align**

Select the logo layer and align it:

```text
Horizontal: Center
Vertical: Center
```

You want:

```text
512 × 512 canvas

┌──────────────────────────────┐
│                              │
│                              │
│           ██████             │
│          ████████            │
│           ██████             │
│                              │
│                              │
└──────────────────────────────┘
```

---

#### Step 6. Alternatively, use GIMP's Scale Tool

Select the **Scale Tool**:

**Tools → Transform Tools → Scale**

Then click on your logo.

You can interactively shrink it.

For example, start with:

```text
512 × 512
```

and experiment with:

```text
400 × 400
350 × 350
300 × 300
```

until the visual size looks right.

Remember: **the actual logo doesn't necessarily need to be a particular pixel size**. What matters
is how large it appears once Android applies its launcher mask.

---

#### Step 7. Important: Don't make the white background part of the PNG

This is particularly important for the adaptive icon setup.

Your final foreground PNG should look like:

```text
ic_launcher_foreground.png

512 × 512

┌──────────────────────────────┐
│                              │
│                              │
│           YOUR               │
│           LOGO               │
│                              │
│                              │
└──────────────────────────────┘

       transparent
```

**Not:**

```text
┌──────────────────────────────┐
│            WHITE             │
│                              │
│           LOGO               │
│                              │
│            WHITE             │
└──────────────────────────────┘
```

The white comes from:

```xml

<background android:drawable="@color/ic_launcher_background" />
```

---

#### Step 8. Export it from GIMP

Once you're happy with the size:

**File → Export As**

Name it:

```text
ic_launcher_foreground.png
```

Put it in:

```text
app/src/main/res/drawable/
```

So you have:

```text
app/
└── src/
    └── main/
        └── res/
            └── drawable/
                └── ic_launcher_foreground.png
```

When exporting, make sure you're keeping the **alpha/transparency**.

If GIMP shows an option related to saving transparency, don't remove it.

---

#### Step 9. What size should the logo be?

For a 512×512 foreground, start around:

##### Option A — moderately small

```text
Logo: ~350 × 350
Canvas: 512 × 512
```

##### Option B — noticeably smaller

```text
Logo: ~300 × 300
Canvas: 512 × 512
```

##### Option C — quite small

```text
Logo: ~250 × 250
Canvas: 512 × 512
```

---

### Using Android Studio's image asset studio

1. Right-click on the `res` folder.

2. Select New > Image Asset.

3. For Path, select your high-quality source image (the one in drawable or on your computer).

4. Under the Foreground Layer tab, use the Scaling slider.

   ◦ Slide it to the left (e.g., to 60% or 70%) to make the logo look smaller.
   ◦ Android Studio will show you a "Safe Zone" circle to make sure your logo doesn't get cut off.

5. Click Next and then Finish.

---

## Testing GitHub workflows locally

[**`act`**](https://github.com/nektos/act) is used to test workflows locally without pushing to 
GitHub or using any actions runner credits. It reads your `.github/workflows/` files and runs them 
locally inside Docker containers. It provides a full emulation of GitHub Actions runner 
environments right on your computer.

### Installation
- **macOS (Homebrew)**
  ```bash
  brew install act
  ```
- **Linux**
  ```bash
  curl -s https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash
  ```
- **Windows (Chocolatey / Scoop)**
  ```bash
  choco install act
  # or
  scoop install act
  ```
### Add to path
  ```bash
  echo 'export PATH="$HOME/bin:$PATH"' >> ~/.bashrc
  source ~/.bashrc
  ```

### Create hidden files

Create these files in the project root directory.

.env
```bash
# Contents of .env
ANDROID_HOME=/<path-to-directory>/Android/Sdk
```

.secrets
```bash
# Contents of .secrets
GITHUB_TOKEN=<github-personal-access-token>
```

.vars
```bash
# Contents of .vars
ANDROID_HOME=/<path-to-directory>/Android/Sdk
```

### Add your user to the docker group

  ```bash
  # Add your user to the docker group
  sudo usermod -aG docker $USER
  
  # Install 'newgrp' command
  sudo apt install util-linux-extra
  
  # Apply the group changes
  newgrp docker
  
  # Verify the fix
  docker ps
  ````

> If changes do not take effect, log out and log back in

### Common commands

>Alternatively, the `act` command can be executed as `~/bin/act`

1. **List all available actions/jobs in your workspace:**
   ```bash
   act -l
   ```
2. **Run the entire workflow (simulating a `push` event):**
   ```bash
   act
   ```
   
3. **Run a specific job only (e.g., the `build` job from your `ci.yml`):**

   ```bash
   # Run 'build' job from 'ci.yml' workflow
   act -j build -W .github/workflows/ci.yml
   # OR (Recommended for Android builds)
   # Map your host SDK to a neutral internal path to avoid permission/path issues
   act --env-file .env --container-options "-v /<path-to-directory>/Android/Sdk:/opt/android-sdk" --env ANDROID_HOME=/opt/android-sdk -j build -W .github/workflows/ci.yml
   
   # Run 'test' job from 'ci.yml' workflow
   act -j test -W .github/workflows/ci.yml
   # OR
   act -s GITHUB_TOKEN=your_token_here -j test -W .github/workflows/ci.yml
   # OR (Recommended for Android tests)
   act --env-file .env --container-options "-v /<path-to-directory>/Android/Sdk:/opt/android-sdk" --env ANDROID_HOME=/opt/android-sdk -j test -W .github/workflows/ci.yml
      
   # Run 'lint' job from 'ci.yml' workflow
   act -j lint -W .github/workflows/ci.yml
   
   # Run 'build' job from 'release.yml' workflow
   act -j build -W .github/workflows/release.yml
   ```
   
4. **Dry run (to see what steps would run without executing them):**
   ```bash
   act -n
   ```

> [!NOTE]
> Given that `act` uses Docker containers to run workflows, you will need to have **Docker** 
> (or an alternative like Podman) installed and running on your machine.

---

## Stop tracking files in Git

The command, `git rm -r --cached <name-of-file-or-directory>` will remove the specified file or 
folder from Git's index (the staging area) without deleting the actual files from your computer.

>The commands must be executed in the project root directory

```bash
# e.g. This command will stop tracking the build/ folder 
# since it is already being tracked

git rm -r --cached build/
# OR, if there are other build/ folders (like app/build/) that are also being tracked, run:
# git rm -r --cached **/build/

# Commit the change
git commit -m "Stop tracking build/ directory"
```
