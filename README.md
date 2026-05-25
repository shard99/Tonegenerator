# LF Tonegen

A precise, minimal Android application designed for debugging room acoustics, identifying room
nodes, and testing low-frequency performance.

## Core Features

- **Dual-Mode Generation**: Toggle between high-fidelity local generation (on-device) and remote hardware generation via BLE.
- **Hardware Companion Integration**: Seamlessly connect to the "LF Tonegen Companion" hardware for remote playback, useful for placing sources exactly where needed without physical cables.
- **Logarithmic Frequency Control**: An intuitive circular wheel for frequency selection across a wide range (10Hz to 30,000Hz).
- **Real-time Measurement**: Uses the Goertzel algorithm and a dynamic graph to analyze and visualize microphone levels at the target frequency.
- **Throttled BLE Control**: Intelligent rate-limiting (4Hz) for Frequency and Volume commands ensures responsiveness while maintaining Bluetooth stability.
- **Data Export**: Save measurements at up to 6 custom physical positions. Results are automatically formatted as CSV and copied to your clipboard.

## How to Use

1. **Choose Mode**: Toggle between **Phone** and **Remote** mode at the top of the screen.
2. **Connect (Remote only)**: When in Remote mode, the app automatically scans and syncs with the companion hardware.
3. **Select Frequency**: Rotate the central wheel to set your target frequency.
4. **Monitor Levels**: Watch the real-time "Mic Level" meter and the rolling measurement graph.
5. **Save Positions**: While playing, tap the star icon to store the current level to a specific room position.
6. **Play/Stop**: Use the prominent button at the bottom.
    * Stopping the tone automatically saves session data to history and copies a CSV string to your **clipboard**.

## Technical Implementation

- **Phone Mode**: Uses `AudioTrack` with `PCM_FLOAT` encoding for ultra-low distortion sine wave generation.
- **Remote Mode**: Communicates with hardware over Bluetooth Low Energy (BLE) using dedicated characteristics for Frequency, Volume, and Playback state.
- **Logarithmic Scaling**: The selection wheel uses logarithmic mapping to provide finer control over sub-bass frequencies where room nodes are most critical.
- **Pop-Free Playback**: Implements smooth fade-in and fade-out logic to prevent transducer pops.

## Installation

1. Clone this repository.
2. Open the project in **Android Studio**.
3. Build and run on an Android device (Physical device required for BLE and microphone analysis).

## Companion Project
The source code for the remote generator hardware is available at [https://github.com/shard99/lf-tonegen-companion](https://github.com/shard99/lf-tonegen-companion).

## License

<a href="https://github.com/shard99/Tonegenerator">LF Tonegen</a>
by <a href="https://github.com/shard99">Andreas Østrem Nielsen</a> is licensed
under <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/">CC BY-NC-SA
4.0</a>
