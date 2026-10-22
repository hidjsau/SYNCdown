package com.clipsync.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.InputFilter
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var pin: EditText

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = getSharedPreferences("c", 0)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 64, 48, 48)

        val title = TextView(this)
        title.text = "ClipSync"
        title.textSize = 24f
        root.addView(title)

        pin = EditText(this)
        pin.hint = "4-digit PIN (same on all devices)"
        pin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        pin.filters = arrayOf(InputFilter.LengthFilter(4))
        pin.setText(prefs.getString("pin", ""))
        root.addView(pin)

        val bOverlay = Button(this)
        bOverlay.text = "1. Allow draw over apps"
        bOverlay.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        root.addView(bOverlay)

        val bNotif = Button(this)
        bNotif.text = "2. Allow notifications"
        bNotif.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            } else {
                Toast.makeText(this, "Already allowed", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(bNotif)

        val bStart = Button(this)
        bStart.text = "3. Save PIN & Start"
        bStart.setOnClickListener {
            val p = pin.text.toString()
            if (p.length != 4) {
                Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
            } else if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Allow draw over apps first", Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit().putString("pin", p).apply()
                stopService(Intent(this, SyncService::class.java))
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(Intent(this, SyncService::class.java))
                else startService(Intent(this, SyncService::class.java))
                Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(bStart)

        val bStop = Button(this)
        bStop.text = "Stop"
        bStop.setOnClickListener {
            stopService(Intent(this, SyncService::class.java))
        }
        root.addView(bStop)

        status = TextView(this)
        status.gravity = Gravity.START
        status.setPadding(0, 32, 0, 0)
        root.addView(status)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val o = Settings.canDrawOverlays(this)
        val n = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        status.text = "Draw over apps: ${if (o) "OK" else "Not allowed"}\nNotifications: ${if (n) "OK" else "Not allowed"}"
    }
}
