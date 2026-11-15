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

  /**
   * Sets the output volume from a 0-100 percentage. Implementations convert it to the companion's
   * amplitude range (see [volumePercentToAmplitude]) before transmitting.
   */
  fun writeVolume(volumePercent: Int)

  fun writeChannel(index: Int)

  fun writePlayState(playing: Boolean)
}
