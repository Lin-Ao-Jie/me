package com.datanet.share.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.Looper

class WifiDirectClient(
    private val context: Context,
    private val onReady: (String) -> Unit,
    private val onState: (String) -> Unit
) {
    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val handler = Handler(Looper.getMainLooper())
    private var channel: WifiP2pManager.Channel? = null
    private var running = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> requestPeers()
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val info = if (Build.VERSION.SDK_INT >= 33)
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO, WifiP2pInfo::class.java)
                    else @Suppress("DEPRECATION") intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
                    if (info?.groupFormed == true) onReady("192.168.49.1")
                }
            }
        }
    }

    fun start() {
        if (running) return
        val mgr = manager ?: run { onState("Wi-Fi Direct unavailable"); return }
        running = true
        channel = mgr.initialize(context, context.mainLooper, null)
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") context.registerReceiver(receiver, filter)
        discoverLoop()
    }

    private fun discoverLoop() {
        if (!running) return
        val mgr = manager ?: return
        try {
            onState("Discovering sender...")
            mgr.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { requestPeers() }
                override fun onFailure(reason: Int) {
                    onState("P2P discovery failed: $reason")
                    handler.postDelayed({ discoverLoop() }, 3000)
                }
            })
        } catch (_: Exception) { handler.postDelayed({ discoverLoop() }, 3000) }
    }

    private fun requestPeers() {
        val mgr = manager ?: return
        try {
            mgr.requestPeers(channel) { list: WifiP2pDeviceList ->
                val peer = list.deviceList.firstOrNull() ?: return@requestPeers
                connect(peer)
            }
        } catch (_: Exception) {}
    }

    private fun connect(peer: WifiP2pDevice) {
        val mgr = manager ?: return
        val config = WifiP2pConfig().apply { deviceAddress = peer.deviceAddress }
        try {
            onState("Connecting to " + peer.deviceName + "...")
            mgr.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {}
                override fun onFailure(reason: Int) {
                    onState("P2P connect failed: " + reason)
                    handler.postDelayed({ discoverLoop() }, 2500)
                }
            })
        } catch (_: Exception) { handler.postDelayed({ discoverLoop() }, 2500) }
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        try { manager?.removeGroup(channel, null) } catch (_: Exception) {}
    }
}
