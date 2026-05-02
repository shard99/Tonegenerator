# LF Tonegen

A precise, minimal Android application designed for debugging room acoustics, identifying room
nodes, and testing bass performance.

## How to Use

1. **Select Frequency**: Rotate the large central wheel to pick your target frequency.
2. **Set Overtones**: Use the settings to add harmonics if needed for node testing (feature currently hidden).
3. **Choose Channel**: Select Left, Right, or Both depending on your hardware setup.
4. **Play/Stop**: Use the prominent button at the bottom. Notice the smooth fade-in and
   instantaneous (but pop-free) stop.

## Features

- **High-Fidelity Synthesis**: Uses the `AudioTrack` API with `PCM_FLOAT` encoding for pure,
  low-distortion sine wave generation.
- **Logarithmic Frequency Control**: A custom circular wheel allows for intuitive frequency
  selection within a configurable range (default **20Hz to 400Hz**).
- **Persistent Settings**: Slot names, frequency range, and theme preferences are saved between sessions.
- **Measurement Slots**: Store up to 6 measured sound levels with custom labels.
- **Safe volume handling**: Tries to prevents sudden spikes when starting and stopping a tone.
- **Channel Selection**: Route audio to the **Left**, **Right** or **Both**.
- **Integrated Volume Monitor**: Real-time display of system media volume to avoid sudden hearing
  loss...

## Technical Details

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Concurrency**: Non-blocking, low-latency audio processing.
- **Target SDK**: 36 (Android 16)

## Installation

1. Clone this repository.
2. Open the project in **Android Studio**.
3. Build and run on an Android device (Physical device recommended since it's an audio app, but you
   do you).

## License

<a href="https://github.com/shard99/Tonegenerator">LF Tonegen</a> © 2026
by <a href="https://github.com/shard99">Andreas Østrem Nielsen</a> is licensed
under <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/">CC BY-NC-SA
4.0</a>
