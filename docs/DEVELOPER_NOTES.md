## Table of Contents

- [Overview](#overview)
- [Project structure](#project-structure)
- [Development workflow](#development-workflow)
- [Releasing the application notes](#releasing-the-application-notes)

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

Push a tag matching the pattern `v*` to the `main` branch. The GitHub workflow will then
automatically build the release APK and bundle, sign them using the provided keystore
and upload the artefacts.

Example tag for a release:

```bash
git checkout main
git tag v1.0.0
git push origin v1.0.0
```

---
