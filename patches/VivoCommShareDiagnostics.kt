package com.datanet.share.vivo

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build

object VivoCommShareDiagnostics {
    data class Snapshot(
        val model: String,
        val androidVersion: String,
        val bluetoothEnabled: Boolean,
        val networkConnected: Boolean,
        val capabilityMask: Int,
        val connectionMask: Int
    )

    fun snapshot(context: Context): Snapshot {
        val bt = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val connected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm?.activeNetwork != null
        } else {
            @Suppress("DEPRECATION")
            cm?.activeNetworkInfo?.isConnected == true
        }
        return Snapshot(
            model = Build.MODEL ?: "unknown",
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            bluetoothEnabled = bt?.adapter?.state == BluetoothAdapter.STATE_ON,
            networkConnected = connected,
            capabilityMask = VivoCommShareProtocol.CAP_DATA,
            connectionMask = VivoCommShareProtocol.CONN_BR or VivoCommShareProtocol.CONN_P2P
        )
    }
}
