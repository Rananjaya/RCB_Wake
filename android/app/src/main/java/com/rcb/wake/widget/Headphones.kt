package com.rcb.wake.widget

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.rcb.wake.R
import com.rcb.wake.notification.Localized

/** Connected earphones for the My Day widget: a name and, for Bluetooth, the battery level. */
data class HeadphonesInfo(val name: String, val batteryPercent: Int?) {
    fun label(): String = if (batteryPercent != null) "$name · $batteryPercent%" else name
}

/**
 * Reads connected wired or Bluetooth earphones on the device. Nothing is stored or sent.
 * Bluetooth names and battery need the "Nearby devices" permission on Android 12+; without it
 * Bluetooth earphones are shown with a generic name and no battery.
 */
object Headphones {
    private val WIRED = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
    )
    private val BLUETOOTH = buildSet {
        add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
    }

    fun hasBluetoothPermission(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun current(context: Context): HeadphonesInfo? {
        val c = Localized.context(context)
        val outputs = context.getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        outputs.firstOrNull { it.type in BLUETOOTH }?.let { device ->
            val name = device.productName?.toString()?.takeIf { it.isNotBlank() }
                ?: c.getString(R.string.headphones_bluetooth)
            return HeadphonesInfo(name, bluetoothBattery(context, name))
        }
        if (outputs.any { it.type in WIRED }) return HeadphonesInfo(c.getString(R.string.headphones_wired), null)
        return null
    }

    /**
     * Battery reported by the earphones, matched by name among connected bonded devices.
     * Android has no public API for this, so the platform's BluetoothDevice#getBatteryLevel is
     * called reflectively; any failure simply hides the percentage.
     */
    @SuppressLint("MissingPermission") // Checked by hasBluetoothPermission.
    private fun bluetoothBattery(context: Context, name: String): Int? {
        if (!hasBluetoothPermission(context)) return null
        return runCatching {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return null
            val devices = adapter.bondedDevices.filter { isConnected(it) }
            val device = devices.firstOrNull { it.name == name || it.alias == name } ?: devices.singleOrNull() ?: return null
            val level = BluetoothDevice::class.java.getMethod("getBatteryLevel").invoke(device) as Int
            level.takeIf { it in 0..100 }
        }.getOrNull()
    }

    private fun isConnected(device: BluetoothDevice): Boolean =
        runCatching { BluetoothDevice::class.java.getMethod("isConnected").invoke(device) as Boolean }.getOrDefault(false)
}
