package com.amanengineering.kehadiran

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class KehadiranService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var callback: LocationCallback

    companion object {
        private const val CHANNEL_ID = "kehadiran_channel"
        private const val NOTIF_ID = 1001
        private const val API_URL = "https://aman-engineering.yunusyusof0603.workers.dev/api/kedatangan-auto"
        private const val INTERVAL_MS = 3L * 60L * 1000L // 3 minit
        private const val MIN_INTERVAL_MS = 90L * 1000L // 90 saat
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_INTERVAL_MS)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                sendPing(loc.latitude, loc.longitude)
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, mainLooper)
        } catch (e: SecurityException) {
            // kebenaran lokasi belum diberi — henti servis senyap
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        if (::fusedClient.isInitialized) {
            fusedClient.removeLocationUpdates(callback)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendPing(lat: Double, lng: Double) {
        val prefs = getSharedPreferences("kehadiran", Context.MODE_PRIVATE)
        val staffId = prefs.getString("staffId", null) ?: return

        Thread {
            try {
                val url = URL(API_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val body = JSONObject().apply {
                    put("staffId", staffId)
                    put("lat", lat)
                    put("lng", lng)
                }.toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                conn.inputStream.use { it.readBytes() }
                conn.disconnect()
            } catch (e: Exception) {
                // senyap — cuba lagi pada ping seterusnya
            }
        }.start()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kehadiran Automatik",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kehadiran dipantau automatik")
            .setContentText("Aman Engineering — jangan tutup app ini")
            .setSmallIcon(R.drawable.ic_stat_location)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
