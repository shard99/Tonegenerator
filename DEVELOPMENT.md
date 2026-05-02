# Development Guidelines for Tone Generator

This document tracks architectural decisions and best practices to maintain code quality.

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
