# LF Tonegen

A precise, minimal Android application designed for debugging room acoustics, identifying room
nodes, and testing low-frequency performance.

## How to Use

1. **Select Frequency**: Rotate the large central wheel to pick your target frequency.
2. **Measure Levels**: Observe the real-time "Mic Level" meter.
3. **Save Positions**: Click the history icon to save the current level to a specific position (
   e.g., "Position 1").
4. **Choose Channel**: Select Left, Right, or Both depending on your hardware setup.
5. **Play/Stop**: Use the prominent button at the bottom.
    * Stopping the tone (or opening the menu) automatically saves your session data to the **Results
      ** history and copies it to your **clipboard** in a semicolon-separated format.

## Features

- **High-Fidelity Synthesis**: Uses the `AudioTrack` API with `PCM_FLOAT` encoding for pure,
  low-distortion sine wave generation.
- **Logarithmic Frequency Control**: A custom circular wheel allows for intuitive frequency
  selection within a configurable range (default **20Hz to 400Hz**, up to 30,000Hz).
- **Real-time Frequency Analysis**: Uses the Goertzel algorithm for sound level measurement of the
  generated frequency.
- **Configurable Positions**: Define up to 6 custom measurement positions (physical locations in
  your room).
- **Persistent Settings**: Position names, frequency range, and theme preferences are saved between
  sessions.
- **Safe Volume Handling**: Prevents sudden spikes with smooth fade-in and pop-free stop logic.

## Installation

1. Clone this repository.
2. Open the project in **Android Studio**.
3. Build and run on an Android device (Physical device required for microphone input and accurate
   audio playback).

## License

<a href="https://github.com/shard99/Tonegenerator">LF Tonegen</a>
by <a href="https://github.com/shard99">Andreas Østrem Nielsen</a> is licensed
under <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/">CC BY-NC-SA
4.0</a>
