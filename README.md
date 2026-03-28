# Tone Generator (Tonegen)

A precise, minimal Android application designed for debugging room acoustics, identifying room
nodes, and testing bass performance.

## How to Use

1. **Select Frequency**: Rotate the large central wheel to pick your target frequency.
2. **Set Overtones**: Use the slider to add harmonics if needed for node testing.
3. **Choose Channel**: Select Left, Right, or Both depending on your hardware setup.
4. **Play/Stop**: Use the prominent button at the bottom. Notice the smooth fade-in and
   instantaneous (but pop-free) stop.

## Features

- **High-Fidelity Synthesis**: Uses the `AudioTrack` API with `PCM_FLOAT` encoding for pure,
  low-distortion sine wave generation.
- **Logarithmic Frequency Control**: A custom circular wheel allows for intuitive frequency
  selection from **20Hz to 500Hz**. The logarithmic scaling provides single-Hz precision at the low
  end where room modes are most critical.
- **Harmonic Overtones**: Add up to 5 overtones (harmonics) to the fundamental frequency (2x, 4x,
  8x, 16x, and 32x). Overtones are automatically capped at 20kHz.
- **Safe volume handling**: Tries to prevents sudden spikes when starting and stopping a tone.
- **Channel Selection**: Route audio to the **Left**, **Right** or **Both**.
- **Integrated Volume Monitor**: Real-time display of system media volume to avoid sudden hearing
  loss...

## Technical Details

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Concurrency**: Non-blocking, low-latency audio processing.
- **Target SDK**: 35 (Android 15)

## Installation

1. Clone this repository.
2. Open the project in **Android Studio**.
3. Build and run on an Android device (Physical device recommended since it's an audio app, but you
   do you).

## License

<a href="https://github.com/shard99/Tonegenerator">Tone Generator</a> © 2026
by <a href="https://github.com/shard99">Andreas Østrem Nielsen</a> is licensed
under <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/">CC BY-NC-SA
4.0</a>
