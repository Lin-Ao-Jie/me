package com.datanet.share

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED && intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val prefs = context.getSharedPreferences("adaptive", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return
        val role = prefs.getString("role", AutoCoordinatorService.Role.HOST.name) ?: AutoCoordinatorService.Role.HOST.name
        val i = Intent(context, AutoCoordinatorService::class.java).apply {
            action = AutoCoordinatorService.ACTION_START
            putExtra(AutoCoordinatorService.EXTRA_ROLE, role)
        }
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
    }
}
