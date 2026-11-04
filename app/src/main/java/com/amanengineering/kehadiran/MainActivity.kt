package com.amanengineering.kehadiran

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val foregroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            maybeRequestBackgroundLocation()
        }

    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Sila pergi Tetapan > Apps > Kehadiran Aman Engineering > Lokasi, pilih \"Benarkan sepanjang masa\" supaya kehadiran automatik berfungsi walau app ditutup.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(JsBridge(this), "AndroidKehadiran")
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://aman-engineering.yunusyusof0603.workers.dev/kedatangan")

        requestForegroundLocation()
    }

    private fun requestForegroundLocation() {
        val needed = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        foregroundLocationLauncher.launch(needed.toTypedArray())
    }

    private fun maybeRequestBackgroundLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) {
            val bgGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!bgGranted) {
                Toast.makeText(
                    this,
                    "Pilih \"Benarkan sepanjang masa\" pada skrin seterusnya supaya kehadiran automatik jalan walau app ditutup.",
                    Toast.LENGTH_LONG
                ).show()
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    inner class JsBridge(private val ctx: Context) {
        @JavascriptInterface
        fun saveStaffId(staffId: String) {
            if (staffId.isBlank()) return
            ctx.getSharedPreferences("kehadiran", Context.MODE_PRIVATE)
                .edit()
                .putString("staffId", staffId)
                .apply()
            val intent = Intent(ctx, KehadiranService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }
    }
}
