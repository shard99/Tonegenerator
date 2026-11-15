package dev.shard9.tonegenerator.audio

/**
 * Transport abstraction over the remote tone generator (BLE companion).
 *
 * [BleManager] is the production implementation; test sources provide an in-memory fake so
 * [dev.shard9.tonegenerator.viewmodel.AppViewModel] can be exercised on the JVM without
 * Bluetooth hardware or a device.
 */
interface RemoteGenerator {
  val status: BleManager.Status
  val isConnected: Boolean

  var onLogReceived: ((String) -> Unit)?

  fun startScanning()

  fun disconnect()

  fun writeFrequency(freq: Float)

  /** Sends a 0-100 volume percentage; the companion maps it through its own amplitude curve. */
  fun writeVolume(volumePercent: Int)

  fun writeChannel(index: Int)

  fun writePlayState(playing: Boolean)
}
