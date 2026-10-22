package com.clipsync.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class CopyAccessibilityService : AccessibilityService() {
    private val h = Handler(Looper.getMainLooper())
    private val word = Regex("\\b(copy|cut)\\b")

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null || e.packageName?.toString() == packageName) return
        val sb = StringBuilder()
        e.text?.forEach { sb.append(it).append(' ') }
        e.contentDescription?.let { sb.append(it) }
        val t = sb.toString().trim().lowercase()
        val hit = when (e.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> t.length < 20 && word.containsMatchIn(t)
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ->
                e.className?.toString()?.contains("Toast") == true && t.contains("copied")
            else -> false
        }
        if (!hit) return
        h.removeCallbacksAndMessages(null)
        h.postDelayed({
            try {
                startActivity(
                    Intent(this, SendActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("auto", true)
                )
            } catch (ex: Exception) {
            }
        }, 400)
    }

    override fun onInterrupt() {}
}
