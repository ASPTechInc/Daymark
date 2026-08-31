# Contributing to Daymark

Thanks for your interest in contributing to **Daymark**!
Daymark is a free open source (FOSS) Android application and your contributions help make it better
for everyone. ❤️

---

## How to Contribute

1. **Fork the repository** to your own GitHub account.
2. **Clone your fork** to your local machine.
3. **Create a new branch** for your work:
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Make your changes**. Ensure you follow the project's code style and architecture.
5. **Run tests** to ensure no regressions were introduced (
   see [Testing](/README.md#testing-the-application)).
6. **Commit your changes** with clear and descriptive messages.
7. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```
8. **Create a pull request** from your fork's branch to the `main` branch of the original
   repository.

---

## Technical Stack

Daymark is built with modern Android development tools:

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Design System**: [Material 3](https://m3.material.io/)
- **Dependency Management
  **: [Gradle Version Catalogue](https://developer.android.com/build/migrate-to-catalogs) (
  libs.versions.toml)

---

## Code Style

To keep the codebase clean and consistent:

- **Kotlin Conventions**: Follow the
  official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- **Formatting**: We use **ktlint**. Please check and format your code before committing:
    - Check formatting: `./gradlew ktlintCheck`
    - Auto-format: `./gradlew ktlintFormat`
- **Focus**: Keep pull requests small and focused on a single feature or fix.
- **Documentation**: Use KDoc for public functions and classes where the purpose isn't immediately
  obvious.

---

## Testing

We value high-quality, tested code. Please ensure your changes are covered by tests.

### Unit Tests

Run local unit tests (JVM):

```bash
./gradlew :app:testDebugUnitTest
```

### Instrumented Tests

Run tests on a physical device or emulator:

```bash
./gradlew connectedDebugAndroidTest
```

---

## Reporting Issues

If you find a bug or have a feature request,
please [open an issue](https://github.com/ASPTechInc/Daymark/issues) and include:

- A clear title and description.
- Steps to reproduce (for bugs).
- Expected vs. actual behaviour.
- Screenshots or recordings if applicable.

---

## Pull Request Checklist

Before submitting your PR, please ensure:

- [ ] Your code compiles without errors.
- [ ] You have followed the [Code Style](/docs/DEVELOPER_NOTES.md#development-workflow) guidelines
  and run `ktlintFormat`.
- [ ] All new and existing tests pass.
- [ ] Your PR description clearly explains the changes.
- [ ] You have updated the `CHANGELOG.md` if necessary.

---

## Questions or Help

If you have questions or need help setting up the environment, please
use [GitHub Discussions](https://github.com/ASPTechInc/Daymark/discussions).
