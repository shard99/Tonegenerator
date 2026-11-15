package dev.shard9.tonegenerator.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot

/**
 * Recording stand-in for [BleManager], so ViewModel tests need no Bluetooth stack or device.
 */
class FakeRemoteGenerator : RemoteGenerator {
  override var status by mutableStateOf(BleManager.Status.DISCONNECTED)
    private set

  override var isConnected = false
    private set

  override var onLogReceived: ((String) -> Unit)? = null

  val frequencyWrites = mutableListOf<Float>()
  val volumeWrites = mutableListOf<Int>()
  val channelWrites = mutableListOf<Int>()
  val playStateWrites = mutableListOf<Boolean>()

  var scanCount = 0
    private set

  var disconnectCount = 0
    private set

  override fun startScanning() {
    scanCount++
    status = BleManager.Status.CONNECTING
    publishSnapshotChanges()
  }

  override fun disconnect() {
    disconnectCount++
    status = BleManager.Status.DISCONNECTED
    isConnected = false
    publishSnapshotChanges()
  }

  override fun writeFrequency(freq: Float) {
    frequencyWrites += freq
  }

  override fun writeVolume(volumePercent: Int) {
    volumeWrites += volumePercent
  }

  override fun writeChannel(index: Int) {
    channelWrites += index
  }

  override fun writePlayState(playing: Boolean) {
    playStateWrites += playing
  }

  /** Simulates a completed GATT handshake and characteristic discovery. */
  fun completeSync() {
    isConnected = true
    status = BleManager.Status.SYNCED
    publishSnapshotChanges()
  }

  fun clearWrites() {
    frequencyWrites.clear()
    volumeWrites.clear()
    channelWrites.clear()
    playStateWrites.clear()
  }

  /**
   * No Recomposer runs in JVM unit tests, so apply notifications are never dispatched on their
   * own; `snapshotFlow` collectors only observe state changes after this flush.
   */
  private fun publishSnapshotChanges() {
    Snapshot.sendApplyNotifications()
  }
}
