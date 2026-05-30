# Changelog

All notable changes to this project will be documented in this file.

## [2.1.0] - 2026-05-30

This release introduces significant upgrades to connectivity, internationalization, real-time measurements, pop-free audio generation, and a redesigned UI for a more seamless acoustic debugging experience.

### Added
- **BLE Remote Control & Synchronization**: Full remote control of companion hardware via Bluetooth Low Energy (BLE) with real-time playback state synchronization (`PLAY_CHAR_UUID`).
- **Throttled BLE Control**: Intelligent rate-limiting (4Hz) for Frequency, Volume, and Channel updates using conflated snapshot flows to ensure hardware stability and prevent command queue congestion.
- **Pop-Free Channel Switching**: Integrated smooth channel transition and 20ms fade-in/fade-out logic for both local and remote modes, eliminating audio pops during playback changes.
- **Microphone Level Measurements in Remote Mode**: Extended Goertzel algorithm microphone analysis to Remote mode, enabling unified level tracking across both generation modes.
- **Dynamic Measurement Graph Previews**: Enhanced rolling graph with right-aligned Y-axis labels and preview support for visual testing in Android Studio.
- **Companion Log Console**: A toggleable, real-time remote log terminal that subscribes to BLE log notifications from the companion hardware.
- **Multilingual Localization**: Added full English and Norwegian (Bokmål) language support, featuring an intuitive in-app language selector and dynamic locale switching.
- **Custom Position Tracking & Export**: Support for tracking mic levels at up to 6 custom physical room positions, automatically formatting results as CSV and copying to the clipboard on playback stop.

### Changed
- **Redesigned Controls**: Relocated the volume control from a horizontal slider to an intuitive vertical layout for better usability.
- **Enlarged Graph View**: Resized the rolling measurement graph-view to match the dimension of the remote console terminal for a balanced UI layout.
- **Optimized Volume Dragging**: Smooth volume slider adjustments using cached/remembered slider state to prevent UI stuttering during active dragging.
- **Polished Layouts**: Refined overall padding, spacing, and component sizes across all screens.

### Fixed
- **BLE Reliability**: Improved reconnection flow reliability and added an active scan timeout to prevent battery drain.
- **Language Selector Fixes**: Resolved locale persistence and UI state synchronization issues on the settings screen.
- **Dependency & Security Patches**: Fixed multiple high-severity vulnerability alerts via strict dependency constraints in `build.gradle.kts` (e.g. io.netty, BouncyCastle, Protobuf).
- **GitHub Actions Security**: Configured proper workflow permissions to satisfy secure CI/CD requirements.

### Tooling & Build
- Upgraded **Android Gradle Plugin (AGP)** to v9.2.1.
- Updated project to use Kotlin compiler features compatible with newer target SDKs.
