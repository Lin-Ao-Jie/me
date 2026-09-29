package com.datanet.share

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.Build
import android.util.DisplayMetrics

class AutoLaunchActivity : Activity() {
    private var pendingRole: AutoCoordinatorService.Role? = null

    private fun inferRole(): AutoCoordinatorService.Role {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        val smallestDp = minOf(dm.widthPixels / dm.density, dm.heightPixels / dm.density)
        return if (smallestDp >= 600f) AutoCoordinatorService.Role.CLIENT else AutoCoordinatorService.Role.HOST
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = inferRole()
        getSharedPreferences("adaptive", MODE_PRIVATE).edit()
            .putBoolean("enabled", true)
            .putString("role", role.name)
            .apply()
        if (role == AutoCoordinatorService.Role.CLIENT) {
            val prepare = VpnService.prepare(this)
            if (prepare != null) {
                pendingRole = role
                @Suppress("DEPRECATION") startActivityForResult(prepare, 7001)
                return
            }
        }
        startAuto(role)
    }

    @Deprecated("Legacy activity result is used for Android 11 compatibility.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7001 && resultCode == RESULT_OK) pendingRole?.let { startAuto(it) }
        else if (requestCode == 7001) finish()
    }

    private fun startAuto(role: AutoCoordinatorService.Role) {
        val i = Intent(this, AutoCoordinatorService::class.java).apply {
            action = AutoCoordinatorService.ACTION_START
            putExtra(AutoCoordinatorService.EXTRA_ROLE, role.name)
        }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        finish()
    }
}
