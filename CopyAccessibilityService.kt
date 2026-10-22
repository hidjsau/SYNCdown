package com.clipsync.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class CopyAccessibilityService : AccessibilityService() {
    private val word = Regex("\\b(copy|cut)\\b")
    private var last = 0L

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null) return
        val pkg = e.packageName?.toString() ?: ""
        if (pkg == packageName) return
        val sb = StringBuilder()
        e.text?.forEach { sb.append(it).append(' ') }
        e.contentDescription?.let { sb.append(it) }
        val t = sb.toString().trim().lowercase()
        val cls = e.className?.toString()?.lowercase() ?: ""
        val hit = when (e.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> t.length < 20 && word.containsMatchIn(t)
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ->
                cls.contains("toast") && t.contains("copied")
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ->
                pkg == "com.android.systemui" && (cls.contains("clip") || t.contains("clip"))
            else -> false
        }
        if (!hit) return
        val now = System.currentTimeMillis()
        if (now - last < 1500) return
        last = now
        try {
            startActivity(
                Intent(this, SendActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("auto", true)
            )
        } catch (ex: Exception) {
        }
    }

    override fun onInterrupt() {}
}
