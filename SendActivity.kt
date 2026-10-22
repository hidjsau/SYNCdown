package com.clipsync.app

import android.app.Activity
import android.content.ClipboardManager
import android.widget.Toast

class SendActivity : Activity() {
    private var done = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || done) return
        done = true
        val auto = intent.getBooleanExtra("auto", false)
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(this)?.toString()
        if (text.isNullOrEmpty()) {
            if (!auto) Toast.makeText(this, "Clipboard empty", Toast.LENGTH_SHORT).show()
        } else if (Net.send(this, text)) {
            Toast.makeText(this, "Sent", Toast.LENGTH_SHORT).show()
        } else if (!auto) {
            Toast.makeText(this, "Pair a device first", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
