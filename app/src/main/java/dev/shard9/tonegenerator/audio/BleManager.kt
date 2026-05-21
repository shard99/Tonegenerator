package dev.shard9.tonegenerator.audio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

private const val TAG = "BleManager"
private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
private val FREQ_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
private val VOL_CHAR_UUID = UUID.fromString("8a7f14b6-7eb4-45fb-8120-6d45904f8e22")
private val PLAY_CHAR_UUID = UUID.fromString("c82b0f41-aef4-44bc-a0a3-c59124430e7a")

@SuppressLint("MissingPermission")
class BleManager(
  private val context: Context,
) {
  enum class Status { DISCONNECTED, CONNECTING, CONNECTED, SYNCED, ERROR }

  var status by mutableStateOf(Status.DISCONNECTED)
    private set

  private val bluetoothAdapter: BluetoothAdapter? by lazy {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    manager.adapter
  }

  private var bluetoothGatt: BluetoothGatt? = null
  private var freqCharacteristic: BluetoothGattCharacteristic? = null
  private var volCharacteristic: BluetoothGattCharacteristic? = null
  private var playCharacteristic: BluetoothGattCharacteristic? = null

  var isConnected = false
    private set

  fun startScanning() {
    if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) return

    Log.d(TAG, "Starting BLE Scan...")
    status = Status.CONNECTING
    val scanner = bluetoothAdapter!!.bluetoothLeScanner
    scanner?.startScan(scanCallback)
  }

  private val scanCallback =
    object : ScanCallback() {
      override fun onScanResult(
        callbackType: Int,
        result: ScanResult,
      ) {
        val device = result.device
        if (device.name == "LF Tonegen Companion") {
          Log.d(TAG, "Found companion device. Connecting...")
          bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
          connectToDevice(device)
        }
      }

      override fun onScanFailed(errorCode: Int) {
        Log.e(TAG, "Scan failed with error: $errorCode")
        status = Status.ERROR
      }
    }

  private fun connectToDevice(device: BluetoothDevice) {
    bluetoothGatt = device.connectGatt(context, false, gattCallback)
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
          gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
          Log.d(TAG, "Disconnected from GATT server.")
          this@BleManager.status = Status.DISCONNECTED
          freqCharacteristic = null
          volCharacteristic = null
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
            Log.d(TAG, "Services discovered and characteristics acquired.")
            this@BleManager.status = Status.SYNCED
          }
        }
      }

      override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
      ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          this@BleManager.status = Status.SYNCED
        } else {
          Log.e(TAG, "Characteristic write failed: $status")
          this@BleManager.status = Status.ERROR
        }
      }
    }

  fun writeFrequency(freq: Float) {
    val char = freqCharacteristic ?: return
    val gatt = bluetoothGatt ?: return
    val data = freq.toString().toByteArray()

    // Optimistically set to connected, wait for callback to set SYNCED
    if (status != Status.ERROR) status = Status.CONNECTED

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      gatt.writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
    } else {
      @Suppress("DEPRECATION")
      char.value = data
      @Suppress("DEPRECATION")
      gatt.writeCharacteristic(char)
    }
  }

  fun writeVolume(volumePercent: Int) {
    val char = volCharacteristic ?: return
    val gatt = bluetoothGatt ?: return
    // Map 0-100% to 0-32767
    val amp = (volumePercent * 327.67f).toInt()
    val data = amp.toString().toByteArray()

    if (status != Status.ERROR) status = Status.CONNECTED

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
    bluetoothGatt?.disconnect()
    bluetoothGatt?.close()
    bluetoothGatt = null
    status = Status.DISCONNECTED
  }
}
