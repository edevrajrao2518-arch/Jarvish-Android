package com.jarvis.assistant
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.accessibilityservice.AccessibilityService as A
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** All commands run on the phone. Nothing is sent to any server by this app. */
object Brain {
    private val fin = Regex("\\b(pay|payment|transfer|send money|upi|bank|banking|account number|card|cvv|pin|otp|gpay|google pay|phonepe|paytm|bhim|netbanking|loan|wallet|paypal)\\b")
    private val blocked = listOf("bank", "paytm", "phonepe", "paisa", "bhim", "wallet", "upi", "paypal")

    fun run(c: Context, raw: String): String {
        val l = raw.lowercase(Locale.ROOT).replace(Regex("[?.!,]"), "").trim()
        if (l.isEmpty()) return "I’m listening, sir."
        if (fin.containsMatchIn(l)) return "I can’t help with payments, banking or money details. That keeps your finances private and safe."
        fun has(vararg k: String) = k.any { l.contains(it) }
        fun go(i: Intent) = c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        fun act(a: Int, ok: String): String { val s = JarvisService.inst ?: return "Please turn on Jarvis Control in Setup first."; return if (s.performGlobalAction(a)) ok else "Android couldn’t perform that action right now." }
        val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            Regex("^(?:open|launch|start|kholo|khol do) (.+)").find(l)?.let { return openApp(c, it.groupValues[1].trim()) }
            Regex("(?:call|dial) (\\+?[\\d ]{5,16})").find(l)?.let { go(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + it.groupValues[1].replace(" ", "")))); return "Dialer ready. Please press call to confirm." }
            Regex("(?:whatsapp|message|text) \\+?(\\d[\\d ]{7,14}) (.+)").find(l)?.let {
                go(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${it.groupValues[1].replace(" ", "")}?text=" + Uri.encode(it.groupValues[2]))))
                return "Message drafted. Please tap send in WhatsApp to confirm." }
            Regex("timer (?:for )?(\\d+) ?(sec|min|hour)").find(l)?.let {
                val n = it.groupValues[1].toInt(); val m = when (it.groupValues[2]) { "sec" -> 1; "min" -> 60; else -> 3600 }
                go(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, n * m).putExtra(AlarmClock.EXTRA_SKIP_UI, true)); return "Timer set." }
            if (has("brightness", "roshni")) {
                if (!Settings.System.canWrite(c)) { go(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + c.packageName))); return "Please allow system settings for me, then ask again." }
                val v = (Regex("(\\d+)").find(l)?.groupValues?.get(1)?.toInt() ?: if (has("up", "increase", "badha", "more")) 90 else 25).coerceIn(0, 100)
                Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS, v * 255 / 100); return "Brightness set to $v percent."
            }
        } catch (e: Exception) { return "Android didn’t let me do that just now. Please try again with Jarvis open on screen." }
        return when {
            has("help", "what can you do") -> "Try: open YouTube, volume up, brightness 40, timer for 5 minutes, call a number, message a number, screenshot, lock the screen, or say search for followed by your question."
            l.matches(Regex("(hi|hello|hey|namaste|good (morning|afternoon|evening))")) -> "Hello, sir. How can I help?"
            has("who are you", "your name") -> "I am Jarvis, your private voice assistant. Everything runs on your phone."
            has("thank") -> "Always a pleasure, sir."
            has("what time", "the time", "samay") -> "It is " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()) + "."
            has("what date", "today’s date", "todays date", "what day", "aaj ki date") -> "Today is " + DateFormat.getDateInstance(DateFormat.FULL).format(Date()) + "."
            has("volume up", "louder", "increase volume", "volume badh", "awaaz badh") -> { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); "Volume up." }
            has("volume down", "quieter", "decrease volume", "volume kam", "awaaz kam") -> { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); "Volume down." }
            has("mute") -> { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI); "Muted." }
            has("screenshot") -> act(A.GLOBAL_ACTION_TAKE_SCREENSHOT, "Screenshot taken.")
            has("lock") -> act(A.GLOBAL_ACTION_LOCK_SCREEN, "Locking the screen.")
            has("go home", "home screen") || l == "home" -> act(A.GLOBAL_ACTION_HOME, "Home.")
            has("go back") || l == "back" -> act(A.GLOBAL_ACTION_BACK, "Back.")
            has("recent", "switch app") -> act(A.GLOBAL_ACTION_RECENTS, "Here are your recent apps.")
            has("shut down", "shutdown", "restart", "power off", "reboot") -> act(A.GLOBAL_ACTION_POWER_DIALOG, "Opening the power menu. Please confirm there; Android doesn’t let apps restart the phone directly.")
            l.startsWith("search ") || l.startsWith("google ") || l.startsWith("look up ") -> {
                val query = raw.substringAfter(' ').trim()
                try { go(Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query)); "Opening a web search. Review the query in your browser." }
                catch (e: Exception) { "I couldn’t open a search app. Try again with your browser installed." }
            }
            else -> "I don’t know that command yet. Say help to hear available commands, or say search followed by a question to look it up."
        }
    }

    private fun openApp(c: Context, name: String): String {
        val pm = c.packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val hit = apps.firstOrNull { val n = it.loadLabel(pm).toString().lowercase(Locale.ROOT); n == name || n.contains(name) || (name.contains(n) && n.length > 3) }
            ?: return "I couldn’t find an app called $name."
        val pkg = hit.activityInfo.packageName
        if (blocked.any { pkg.lowercase(Locale.ROOT).contains(it) }) return "I don’t open banking or payment apps. Please open it yourself."
        val i = pm.getLaunchIntentForPackage(pkg) ?: return "I couldn’t open that app."
        c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return "Opening ${hit.loadLabel(pm)}."
    }
}
