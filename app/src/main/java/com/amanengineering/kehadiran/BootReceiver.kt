package com.amanengineering.kehadiran

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("kehadiran", Context.MODE_PRIVATE)
        val staffId = prefs.getString("staffId", null) ?: return

        val svcIntent = Intent(context, KehadiranService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(svcIntent)
        } else {
            context.startService(svcIntent)
        }
    }
}
