package dev.shard9.tonegenerator.audio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID
import kotlin.math.pow

private const val TAG = "BleManager"
private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
private val FREQ_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
private val VOL_CHAR_UUID = UUID.fromString("8a7f14b6-7eb4-45fb-8120-6d45904f8e22")
private val PLAY_CHAR_UUID = UUID.fromString("c82b0f41-aef4-44bc-a0a3-c59124430e7a")
private val CHAN_CHAR_UUID = UUID.fromString("1c95d5e3-d03b-4c7d-9407-3bd442084c6e")
private val LOG_CHAR_UUID = UUID.fromString("7ba37b12-1f7c-47bc-9407-3bd442084c6e")
private val CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

@SuppressLint("MissingPermission")
class BleManager(
  private val context: Context,
) {
  enum class Status { DISCONNECTED, CONNECTING, CONNECTED, SYNCED, ERROR }

  var status by mutableStateOf(Status.DISCONNECTED)
    private set

  var onLogReceived: ((String) -> Unit)? = null

  private val bluetoothAdapter: BluetoothAdapter? by lazy {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    manager.adapter
  }

  private var bluetoothGatt: BluetoothGatt? = null
  private var freqCharacteristic: BluetoothGattCharacteristic? = null
  private var volCharacteristic: BluetoothGattCharacteristic? = null
  private var playCharacteristic: BluetoothGattCharacteristic? = null
  private var chanCharacteristic: BluetoothGattCharacteristic? = null
  private var logCharacteristic: BluetoothGattCharacteristic? = null

  var isConnected = false
    private set

  private var isScanning = false
  private val handler = Handler(Looper.getMainLooper())
  private val scanTimeoutRunnable =
    Runnable {
      if (isScanning) {
        Log.w(TAG, "Scan timed out after 10s")
        stopScanning()
        status = Status.ERROR
      }
    }

  fun startScanning() {
    if (bluetoothAdapter == null || !(bluetoothAdapter!!.isEnabled)) {
      Log.e(TAG, "Bluetooth not enabled or adapter null")
      status = Status.ERROR
      return
    }
    if (isScanning) {
      Log.d(TAG, "Scan already in progress, ignoring.")
      return
    }

    Log.d(TAG, "Starting BLE Scan...")
    status = Status.CONNECTING
    isScanning = true
    val scanner = bluetoothAdapter!!.bluetoothLeScanner

    val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
    val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

    try {
      scanner?.startScan(listOf(filter), settings, scanCallback)
      handler.postDelayed(scanTimeoutRunnable, 10000)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start scan: ${e.message}")
      status = Status.ERROR
      isScanning = false
    }
  }

  fun stopScanning() {
    handler.removeCallbacks(scanTimeoutRunnable)
    if (!isScanning) return
    Log.d(TAG, "Stopping BLE Scan...")
    try {
      val scanner = bluetoothAdapter!!.bluetoothLeScanner
      scanner?.stopScan(scanCallback)
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping scan: ${e.message}")
    }
    isScanning = false
  }

  private val scanCallback =
    object : ScanCallback() {
      override fun onScanResult(
        callbackType: Int,
        result: ScanResult,
      ) {
        val deviceAddress = result.device.address
        Log.d(TAG, "Found target device: $deviceAddress. Connecting...")
        stopScanning()
        connectToDevice(result.device)
      }

      override fun onBatchScanResults(results: MutableList<ScanResult>) {
        results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
      }

      override fun onScanFailed(errorCode: Int) {
        Log.e(TAG, "Scan failed with error: $errorCode")
        status = Status.ERROR
        isScanning = false
      }
    }

  private fun connectToDevice(device: BluetoothDevice) {
    // Using TRANSPORT_LE to ensure we don't accidentally try classic BT
    bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
  }

  private val gattCallback =
    object : BluetoothGattCallback() {
      override fun onConnectionStateChange(
        gatt: BluetoothGatt,
        status: Int,
        newState: Int,
      ) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
          Log.d(TAG, "Connected to GATT server.")
          this@BleManager.status = Status.CONNECTED
          isConnected = true
          gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
          Log.d(TAG, "Disconnected from GATT server.")
          this@BleManager.status = Status.DISCONNECTED
          isConnected = false
          freqCharacteristic = null
          volCharacteristic = null
          playCharacteristic = null
          chanCharacteristic = null
          logCharacteristic = null
          gatt.close()
          if (bluetoothGatt == gatt) bluetoothGatt = null
        }
      }

      override fun onServicesDiscovered(
        gatt: BluetoothGatt,
        status: Int,
      ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          val service = gatt.getService(SERVICE_UUID)
          if (service != null) {
            freqCharacteristic = service.getCharacteristic(FREQ_CHAR_UUID)
            volCharacteristic = service.getCharacteristic(VOL_CHAR_UUID)
            playCharacteristic = service.getCharacteristic(PLAY_CHAR_UUID)
            chanCharacteristic = service.getCharacteristic(CHAN_CHAR_UUID)
            logCharacteristic = service.getCharacteristic(LOG_CHAR_UUID)

            val logChar = logCharacteristic
            if (logChar != null) {
              Log.d(TAG, "Subscribing to log characteristic: ${logChar.uuid}")
              gatt.setCharacteristicNotification(logChar, true)
              val descriptor = logChar.getDescriptor(CLIENT_CONFIG_UUID)
              if (descriptor != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                  gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                  @Suppress("DEPRECATION")
                  descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                  @Suppress("DEPRECATION")
                  gatt.writeDescriptor(descriptor)
                }
              } else {
                Log.w(TAG, "CCCD Descriptor not found for log characteristic!")
                // Fallback to Synced if we can't subscribe but characteristics are there
                this@BleManager.status = Status.SYNCED
              }
            } else {
              Log.d(TAG, "Services discovered. Characteristics acquired (no logs characteristic found).")
              this@BleManager.status = Status.SYNCED
            }
          }
        }
      }

      override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
      ) {
        if (descriptor.characteristic.uuid == LOG_CHAR_UUID) {
          if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "Log subscription successful.")
          } else {
            Log.e(TAG, "Log subscription failed with status: $status")
          }
          // Regardless of log subscription success, we are now "SYNCED" and can send data
          this@BleManager.status = Status.SYNCED
        }
      }

      @Suppress("DEPRECATION")
      override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
      ) {
        if (characteristic.uuid == LOG_CHAR_UUID) {
          @Suppress("DEPRECATION")
          val logMsg = String(characteristic.value)
          Log.d(TAG, "Log received (deprecated): $logMsg")
          onLogReceived?.invoke(logMsg)
        }
      }

      override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
      ) {
        if (characteristic.uuid == LOG_CHAR_UUID) {
          val logMsg = String(value)
          Log.d(TAG, "Log received: $logMsg")
          onLogReceived?.invoke(logMsg)
        }
      }

      override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
      ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          Log.d(TAG, "Write success for: ${characteristic.uuid}")
        } else {
          Log.e(TAG, "Characteristic write failed: $status for ${characteristic.uuid}")
          this@BleManager.status = Status.ERROR
        }
      }
    }

  fun writeFrequency(freq: Float) {
    val char = freqCharacteristic ?: return
    val gatt = bluetoothGatt ?: return
    val data = freq.toString().toByteArray()
    write(gatt, char, data)
  }

  fun writeVolume(volumePercent: Int) {
    val char = volCharacteristic ?: return
    val gatt = bluetoothGatt ?: return
    // Map 0-100% logarithmically to 0-32767
    val ratio = (10.0.pow(volumePercent / 100.0) - 1.0) / 9.0
    val amp = (ratio * 32767).toInt()
    val data = amp.toString().toByteArray()
    write(gatt, char, data)
  }

  fun writePlayState(playing: Boolean) {
    val char = playCharacteristic ?: return
    val gatt = bluetoothGatt ?: return
    val data = (if (playing) "1" else "0").toByteArray()
    write(gatt, char, data)
  }

  fun writeChannel(index: Int) {
    val char = chanCharacteristic ?: return
    val gatt = bluetoothGatt ?: return
    val data = index.toString().toByteArray()
    write(gatt, char, data)
  }

  private fun write(
    gatt: BluetoothGatt,
    char: BluetoothGattCharacteristic,
    data: ByteArray,
  ) {
    Log.d(TAG, "Writing to ${char.uuid}: ${String(data)}")
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      gatt.writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
    } else {
      @Suppress("DEPRECATION")
      char.value = data
      @Suppress("DEPRECATION")
      gatt.writeCharacteristic(char)
    }
  }

  fun disconnect() {
    stopScanning()
    bluetoothGatt?.let { gatt ->
      Log.d(TAG, "Closing GATT connection...")
      try {
        gatt.disconnect()
        gatt.close()
      } catch (e: Exception) {
        Log.e(TAG, "Error during GATT close: ${e.message}")
      }
    }
    bluetoothGatt = null
    status = Status.DISCONNECTED
    isConnected = false
  }
}
