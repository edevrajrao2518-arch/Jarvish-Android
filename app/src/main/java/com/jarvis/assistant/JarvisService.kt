package com.jarvis.assistant
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/** Only performs Home/Back/Recents/Lock/Screenshot/Power-menu. Cannot read any screen content. */
class JarvisService : AccessibilityService() {
    companion object { var inst: JarvisService? = null }
    override fun onServiceConnected() { inst = this }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(i: Intent?): Boolean { inst = null; return super.onUnbind(i) }
}
