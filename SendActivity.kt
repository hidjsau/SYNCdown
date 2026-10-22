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
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(this)?.toString()
        if (text.isNullOrEmpty()) {
            Toast.makeText(this, "Clipboard empty", Toast.LENGTH_SHORT).show()
        } else {
            Net.send(this, text)
            Toast.makeText(this, "Sent", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
