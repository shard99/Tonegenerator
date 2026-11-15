# Development Guidelines for LF Tonegen

This document tracks architectural decisions and best practices to maintain code quality.

## Project Language Standard

- **Rule**: All code, comments, documentation, and development-related instructions must be in **English**.
- **Exception**: Actual localization resource files (e.g., `strings.xml` for other languages) are the only files
  permitted to contain non-English text.

## AI Automation Rules

- **Versioning**: When modifying `app/build.gradle.kts` or implementing new features, always increment
  `defaultConfig.versionCode` by 1 and increase the patch number of `versionName` (e.g., 1.1.1 -> 1.1.2).
- **Git Awareness**: Before incrementing, check if the version numbers have already been modified in uncommitted changes
  to prevent duplicate increments during the same development session.
- **Changelog**: The `CHANGELOG.md` file should only be touched when tagging a new release.
- **Code Style**: Always run `./gradlew ktlintFormat` before committing any Kotlin code changes to ensure adherence to
  the project's style guidelines.

## Companion Project

- **Source Code**: The source code for the remote generator hardware (companion) is available on a private GitHub-repo
  at [https://github.com/shard99/lf-tonegen-companion](https://github.com/shard99/lf-tonegen-companion).
- **BLE Logging**: The hardware companion supports sending real-time logs via a dedicated BLE characteristic (`7ba37b12-1f7c-47bc-9407-3bd442084c6e`). The Android app can subscribe to notifications from this characteristic to monitor the internal state of the generator.
- **Companion Connectivity**: The companion hardware must explicitly restart advertising upon disconnection to allow the Android app to reconnect. If it fails to do so, the app may get stuck in the "Connecting" (Yellow) state.

## Kotlin Best Practices

- **Rule**: Use anonymous names (`_`) for unused parameters in lambdas and unused exception names in `catch` blocks to
  improve code clarity.
- **Formatting**: The project uses **ktlint** for code style enforcement.
  - Use `./gradlew ktlintFormat` to automatically fix formatting issues.
  - Use `./gradlew ktlintCheck` to verify that the code conforms to the style.
  - Formatting is enforced during the `check` task.
  - **Local Automation**: Run `./gradlew addKtlintCheckGitPreCommitHook` to ensure your code is checked before every
    commit.

## Testing

- **Rule**: Changes to business logic must come with unit tests that pin the behaviour down.
- **Location**: Local JVM tests live in `app/src/test/java/...`. Instrumented tests, if ever needed, go in
  `app/src/androidTest`.
- **Commands**:
  - `./gradlew test` runs every variant. The debug variant also runs the Compose UI tests in `src/testDebug`,
    so it always has more tests than release.
  - `./gradlew testDebugUnitTest` runs only the debug variant; use it while iterating.
  - `./gradlew jacocoDebugUnitTestReport` writes an HTML/XML coverage report to
    `app/build/reports/jacoco/jacocoDebugUnitTestReport/`.
- **Coverage**: coverage is concentrated in the logic that matters (`data` and `viewmodel`). Most UI
  components and `BleManager`/`ToneGenerator` are not covered yet, so no coverage threshold is enforced — a
  global gate would only be noise at this level. Add one once the UI and audio layers have tests.
- **Structure**: ViewModels are tested against `RemoteGenerator` (implemented in production by `BleManager`) so
  `FakeRemoteGenerator` can record writes without any Bluetooth stack. `SettingsRepository` is tested against
  `FakePreferencesDataStore`, an in-memory `DataStore<Preferences>`.
- **Compose UI tests**: Stateless composables are tested with `createComposeRule()` under `RobolectricTestRunner`,
  so they run on the JVM without an emulator. `SettingsContent` is the entry point for this: it takes plain state
  and callbacks, so no ViewModel is needed. Pin the Robolectric SDK via `@Config(sdk = [...])` to match
  `targetSdk`, and keep the two in sync when `targetSdk` is raised.
- **Rule**: Compose UI tests belong in `app/src/testDebug/java/...`, not `app/src/test/java/...`. `ui-test-manifest`
  is a `debugImplementation` dependency, so a UI test in the shared `src/test` set fails on the release variant with
  `Unable to resolve activity for Intent ... androidx.activity.ComponentActivity`.
- **Rule**: Prefer semantic matchers over `testTag`. Sliders are identified by their unique
  `ProgressBarRangeInfo` range rather than a tag, so production code carries no test-only identifiers.
- **Rule**: Do not use `PreferenceDataStoreFactory` with a temp file in tests. Its second write fails on Windows
  because okio's `atomicMove` cannot rename over the existing file, and DataStore misreports this as duplicated
  instances for the same file. Use `FakePreferencesDataStore` instead.
- **Rule**: `snapshotFlow` collectors do not observe state changes in JVM tests unless apply notifications are
  dispatched. Flush with `Snapshot.sendApplyNotifications()` (see `FakeRemoteGenerator`).

## Continuous Integration (CI)

- **Rule**: All merges to the `main` branch are gated by a GitHub Actions workflow.
- **Checks**: The CI automatically runs `ktlintCheck`, the unit test suite, the JaCoCo coverage report and a full
  build on every Push and Pull Request to `main`. If formatting fails, a test fails, or the coverage task can no
  longer find the compiled classes, the merge is blocked.

## Clipboard Management

- **Rule**: Avoid `androidx.compose.ui.platform.ClipboardManager` (deprecated).
- **Replacement**: Use `androidx.compose.ui.platform.Clipboard` via `LocalClipboard.current`.
- **Usage**:
  - `Clipboard.setClipEntry` is a **suspend function**.
  - Use `ClipData.newPlainText(label, text).toClipEntry()` to create the clip.
  - Always wrap calls in a coroutine scope (e.g., `scope.launch { ... }`).

## Localization

- **Supported Languages**: English (default), Norwegian (Bokmål).
- **Implementation**: Uses `AppCompatDelegate.setApplicationLocales` for dynamic in-app switching.
- **Resource Management**: All UI text must be defined in `strings.xml`. Avoid hardcoded strings in Composable functions.

## ViewModels

- **Rule**: Keep ViewModels pure of UI-specific classes (like `Clipboard` or `Context`).
- **Pattern**: ViewModels should emit results via callbacks or State, and the UI layer should handle platform-specific
  actions like copying to clipboard.

## Frequency Range

- **Rule**: Frequency inputs in settings are restricted to integers between 10 and 30,000 Hz.
- **Implementation**: Handled in `SettingsScreen` with `KeyboardType.Number` and `coerceIn`.

## Channel Selection

- **Rule**: If "Allow dual-channel output" is disabled in settings, the user is restricted to the "Right" channel.
- **Implementation**: `GeneratorScreen` disables "Left" and "Both" buttons, and `AppViewModel` forces the selection to "Right" (index 2) when the setting is toggled off.
