# Development Guidelines for LF Tonegen

This document tracks architectural decisions and best practices to maintain code quality.

## Project Language Standard
- **Rule**: All code, comments, documentation, and development-related instructions must be in **English**.
- **Exception**: Actual localization resource files (e.g., `strings.xml` for other languages) are the only files permitted to contain non-English text.

## AI Automation Rules
- **Versioning**: When modifying `app/build.gradle.kts` or implementing new features, always increment `defaultConfig.versionCode` by 1 and increase the patch number of `versionName` (e.g., 1.1.1 -> 1.1.2).
- **Git Awareness**: Before incrementing, check if the version numbers have already been modified in uncommitted changes to prevent duplicate increments during the same development session.

## Kotlin Best Practices
- **Rule**: Use anonymous names (`_`) for unused parameters in lambdas and unused exception names in `catch` blocks to improve code clarity.
- **Formatting**: The project uses **ktlint** for code style enforcement.
    - Use `./gradlew ktlintFormat` to automatically fix formatting issues.
    - Use `./gradlew ktlintCheck` to verify that the code conforms to the style.
    - Formatting is enforced during the `check` task.
    - **Local Automation**: Run `./gradlew addKtlintCheckGitPreCommitHook` to ensure your code is checked before every commit.

## Continuous Integration (CI)
- **Rule**: All merges to the `main` branch are gated by a GitHub Actions workflow.
- **Checks**: The CI automatically runs `ktlintCheck` and a full build on every Push and Pull Request to `main`. If formatting fails, the merge is blocked.

## Clipboard Management
- **Rule**: Avoid `androidx.compose.ui.platform.ClipboardManager` (deprecated).
- **Replacement**: Use `androidx.compose.ui.platform.Clipboard` via `LocalClipboard.current`.
- **Usage**:
    - `Clipboard.setClipEntry` is a **suspend function**.
    - Use `ClipData.newPlainText(label, text).toClipEntry()` to create the clip.
    - Always wrap calls in a coroutine scope (e.g., `scope.launch { ... }`).

## ViewModels
- **Rule**: Keep ViewModels pure of UI-specific classes (like `Clipboard` or `Context`).
- **Pattern**: ViewModels should emit results via callbacks or State, and the UI layer should handle platform-specific actions like copying to clipboard.

## Frequency Range
- **Rule**: Frequency inputs in settings are restricted to integers between 10 and 30,000 Hz.
- **Implementation**: Handled in `SettingsScreen` with `KeyboardType.Number` and `coerceIn`.
