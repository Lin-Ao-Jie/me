package com.datanet.share.vivo

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class VivoCommShareProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val s = VivoCommShareDiagnostics.snapshot(this)
        val text = "Vivo CommShare compatibility probe\n\n" +
            "Model: " + s.model + "\n" +
            "Android: " + s.androidVersion + "\n" +
            "Bluetooth: " + s.bluetoothEnabled + "\n" +
            "Network: " + s.networkConnected + "\n" +
            "Capabilities: " + VivoCommShareProtocol.capabilitiesToString(s.capabilityMask) + "\n" +
            "Connection types: " + VivoCommShareProtocol.connectionTypesToString(s.connectionMask) + "\n\n" +
            "Protocol model: loaded\n" +
            "Remote pairing: not claimed until live transport is validated"
        setContentView(TextView(this).apply {
            setPadding(32, 32, 32, 32)
            textSize = 16f
            this.text = text
        })
    }
}
