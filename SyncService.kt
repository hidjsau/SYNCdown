package com.clipsync.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.UUID
import kotlin.concurrent.thread

object Net {
    const val PORT = 48555
    val myId: String = UUID.randomUUID().toString().substring(0, 8)

    fun pin(ctx: Context): String =
        ctx.getSharedPreferences("c", 0).getString("pin", "") ?: ""

    fun send(ctx: Context, text: String) {
        val pin = pin(ctx)
        if (pin.length != 4) return
        val t = if (text.length > 15000) text.substring(0, 15000) else text
        val data = Crypto.enc(pin, "$myId|$t")
        thread {
            try {
                val s = DatagramSocket()
                s.broadcast = true
                val targets = HashSet<InetAddress>()
                targets.add(InetAddress.getByName("255.255.255.255"))
                val ifs = NetworkInterface.getNetworkInterfaces()
                while (ifs != null && ifs.hasMoreElements()) {
                    val n = ifs.nextElement()
                    if (!n.isUp || n.isLoopback) continue
                    for (a in n.interfaceAddresses) a.broadcast?.let { targets.add(it) }
                }
                for (a in targets) {
                    try {
                        s.send(DatagramPacket(data, data.size, a, PORT))
                    } catch (e: Exception) {
                    }
                }
                s.close()
            } catch (e: Exception) {
            }
        }
    }
}

class SyncService : Service() {
    private var running = false
    private var sock: DatagramSocket? = null
    private var lock: WifiManager.MulticastLock? = null
    private var bubble: TextView? = null
    private val main = Handler(Looper.getMainLooper())

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel("sync", "ClipSync", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val n = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "sync")
        else Notification.Builder(this)
        n.setContentTitle("ClipSync running")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, n.build())
        }
        if (!running) {
            running = true
            val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            lock = wm.createMulticastLock("clipsync").also {
                it.setReferenceCounted(false)
                it.acquire()
            }
            listen()
        }
        showBubble()
        return START_STICKY
    }

    private fun listen() {
        thread {
            try {
                val s = DatagramSocket(null)
                s.reuseAddress = true
                s.bind(InetSocketAddress(Net.PORT))
                sock = s
                val buf = ByteArray(65535)
                while (running) {
                    val p = DatagramPacket(buf, buf.size)
                    s.receive(p)
                    handle(p.data.copyOf(p.length))
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun handle(data: ByteArray) {
        val pin = Net.pin(this)
        if (pin.length != 4) return
        val plain = Crypto.dec(pin, data) ?: return
        val i = plain.indexOf('|')
        if (i < 0) return
        if (plain.substring(0, i) == Net.myId) return
        val text = plain.substring(i + 1)
        main.post {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("sync", text))
            Toast.makeText(this, "Synced", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBubble() {
        if (bubble != null || !Settings.canDrawOverlays(this)) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val size = (56 * resources.displayMetrics.density).toInt()
        val v = TextView(this)
        v.text = "Sync"
        v.setTextColor(Color.WHITE)
        v.textSize = 12f
        v.gravity = Gravity.CENTER
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(0xCC1976D2.toInt())
        v.background = bg
        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = 0
        lp.y = 300
        var sx = 0
        var sy = 0
        var tx = 0f
        var ty = 0f
        var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY; moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt()
                    val dy = (e.rawY - ty).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true
                    if (moved) {
                        lp.x = sx + dx
                        lp.y = sy + dy
                        wm.updateViewLayout(v, lp)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        startActivity(
                            Intent(this, SendActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
            true
        }
        wm.addView(v, lp)
        bubble = v
    }

    override fun onDestroy() {
        running = false
        sock?.close()
        lock?.release()
        bubble?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (e: Exception) {
            }
        }
        bubble = null
        super.onDestroy()
    }
}
