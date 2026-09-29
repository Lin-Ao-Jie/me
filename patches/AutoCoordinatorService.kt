package com.datanet.share

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.datanet.share.common.Constants
import com.datanet.share.common.NetworkState
import com.datanet.share.common.TrafficMeter
import com.datanet.share.receiver.WifiDirectClient
import com.datanet.share.receiver.ReceiverVpnService
import com.datanet.share.receiver.ReceiverService
import com.datanet.share.sender.SenderService

/**
 * Low-power coordinator. Bluetooth PAN is the default transport. P2P is brought
 * up only when ordinary Wi-Fi is unavailable and Bluetooth appears saturated.
 * No wakelock and no continuous P2P discovery are used in the idle state.
 */
class AutoCoordinatorService : Service() {
    enum class Role { HOST, CLIENT }
    companion object {
        const val ACTION_START = "com.datanet.share.AUTO_START"
        const val ACTION_STOP = "com.datanet.share.AUTO_STOP"
        const val EXTRA_ROLE = "role"
        private const val PREFS = "adaptive"
        private const val PREF_ROLE = "role"
        private const val CHANNEL = "datanet_auto"
        private const val NOTIF_ID = 1100
        private const val SAMPLE_MS = 5000L
        private const val HIGH_MBPS = 1.2
        private const val LOW_MBPS = 0.5
        private const val LOW_HOLD_MS = 30_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var role = Role.HOST
    private var boostActive = false
    private var lowSince = 0L
    private var client: WifiDirectClient? = null
    private val traffic = TrafficMeter { it.contains("bt", true) || it.contains("pan", true) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopCoordinator(); return START_NOT_STICKY }
            ACTION_START -> {
                role = Role.entries.find { it.name.equals(intent.getStringExtra(EXTRA_ROLE), true) }
                    ?: loadRole()
                saveRole(role)
                startForegroundCompat("Adaptive ${role.name.lowercase()}", "Bluetooth-first; Wi-Fi boost on demand")
                schedule()
            }
        }
        return START_STICKY
    }

    private fun loadRole(): Role = try { Role.valueOf(getSharedPreferences(PREFS, 0).getString(PREF_ROLE, Role.HOST.name)!!) } catch (_: Exception) { Role.HOST }
    private fun saveRole(r: Role) { getSharedPreferences(PREFS, 0).edit().putString(PREF_ROLE, r.name).apply() }

    private fun schedule() { handler.removeCallbacksAndMessages(null); handler.post(loop) }
    private val loop = object : Runnable {
        override fun run() {
            try { tick() } finally { handler.postDelayed(this, SAMPLE_MS) }
        }
    }

    private fun tick() {
        if (NetworkState.hasValidatedWifi(this)) {
            lowSince = 0L
            if (boostActive) stopBoost()
            update("Normal Wi-Fi detected", "Sharing dormant")
            return
        }
        if (!NetworkState.hasBluetoothPan(this)) {
            update("Waiting for Bluetooth PAN", "Wi-Fi Direct off")
            return
        }
        val mbps = traffic.sampleMbps()
        if (!boostActive) {
            if (mbps != null && mbps >= HIGH_MBPS) startBoost("Bluetooth load ${"%.2f".format(mbps)} Mbps")
            else update("Bluetooth PAN", if (mbps == null) "Measuring" else "${"%.2f".format(mbps)} Mbps")
        } else {
            if (mbps != null && mbps <= LOW_MBPS) {
                if (lowSince == 0L) lowSince = System.currentTimeMillis()
                if (System.currentTimeMillis() - lowSince >= LOW_HOLD_MS) stopBoost()
            } else lowSince = 0L
        }
    }

    private fun startBoost(reason: String) {
        if (boostActive) return
        boostActive = true
        lowSince = 0L
        update("Starting Wi-Fi Direct boost", reason)
        if (role == Role.HOST) {
            startService(Intent(this, SenderService::class.java).apply { action = SenderService.ACTION_START })
        } else {
            client?.stop()
            client = WifiDirectClient(this,
                onReady = { host -> startReceiver(host) },
                onState = { msg -> update("P2P", msg) }).also { it.start() }
        }
    }

    private fun startReceiver(host: String) {
        val i = Intent(this, ReceiverService::class.java).apply {
            action = ReceiverService.ACTION_START
            putExtra(ReceiverService.EXTRA_SOCKS_HOST, host)
            putExtra(ReceiverService.EXTRA_SOCKS_PORT, Constants.SOCKS5_PORT)
        }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
    }

    private fun stopBoost() {
        boostActive = false
        lowSince = 0L
        client?.stop(); client = null
        if (role == Role.HOST) startService(Intent(this, SenderService::class.java).apply { action = SenderService.ACTION_STOP })
        else {
            startService(Intent(this, ReceiverService::class.java).apply { action = ReceiverService.ACTION_STOP })
            startService(Intent(this, ReceiverVpnService::class.java).apply { action = ReceiverVpnService.ACTION_STOP })
        }
        update("Bluetooth PAN", "Returned to low-power mode")
    }

    private fun stopCoordinator() { handler.removeCallbacksAndMessages(null); stopBoost(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }

    private fun startForegroundCompat(title: String, text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CHANNEL, "Adaptive sharing", NotificationManager.IMPORTANCE_LOW))
        val n = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle(title).setContentText(text).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) ServiceCompat.startForeground(this, NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        else startForeground(NOTIF_ID, n)
    }

    private fun update(title: String, text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle(title).setContentText(text).setOngoing(true).build()
        nm.notify(NOTIF_ID, n)
    }
}