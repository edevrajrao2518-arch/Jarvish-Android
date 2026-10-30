package com.jarvis.assistant

import android.Manifest
import android.accessibilityservice.AccessibilityService as A
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var orb: View
    private var sr: SpeechRecognizer? = null
    private val hist = JSONArray()

    // Rule 11: anything money/bank related is refused on-device and never sent anywhere.
    // Local fail-closed gate before any request can reach the cloud assistant.
    // Covers common finance terms in English, Hindi/Hinglish and several widely used languages,
    // plus long numeric identifiers (account/card/phone-like numbers).
    private val fin = Regex("(pay|payment|transfer|send money|upi|bank|banking|account number|account|card|cvv|pin|otp|gpay|google pay|phonepe|paytm|bhim|netbanking|loan|wallet|balance|credit|debit|rupee|rupees|paisa|paise|bhugtan|khata|खाता|बैंक|भुगतान|पैसा|पैसे|कार्ड|खाता संख्या|banco|pago|pagos|transferencia|cuenta|tarjeta|dinero|banca|banque|paiement|virement|compte|carte|geld|zahlung|überweisung|konto|karte|banca|pagamento|trasferimento|conta|cartão|pix|банк|платёж|платеж|перевод|счёт|карта|حساب|بنك|دفع|تحويل|银行|付款|账户|银行卡|送金|銀行|口座|支払い|계좌|은행|결제|송금|iban|swift|ifsc|routing number|sort code|micr)", RegexOption.IGNORE_CASE)
    private val longNumericId = Regex("(?<!\\d)(?:\\d[\\s.\\-/]?){7,}\\d(?!\\d)")
    private val finPkg = listOf("bank", "paytm", "phonepe", "paisa", "bhim", "wallet", "pay")

    private val sys = "You are J.A.R.V.I.S., a witty, polite, helpful assistant. Reply in the SAME language the user used (English, Hindi, Hinglish, Spanish, any). " +
        "Replies are spoken aloud: max 3 short sentences, no markdown. Think step by step silently, give the best answer, and add one better suggestion when useful. " +
        "Never help with payments, transactions, bank or card details; refuse politely. First line must be LANG:<BCP-47 tag> e.g. LANG:hi-IN, then the reply on the next line."

    private fun vault() = EncryptedSharedPreferences.create(
        this, "vault", MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE) // blocks screenshots/recents preview of this app
        tts = TextToSpeech(this, this)
        val d = resources.displayMetrics.density
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setBackgroundColor(Color.parseColor("#050B14")); setPadding((16 * d).toInt(), (48 * d).toInt(), (16 * d).toInt(), (16 * d).toInt()) }
        root.addView(TextView(this).apply { text = "J A R V I S"; textSize = 22f; setTextColor(Color.parseColor("#3EE6FF")); gravity = Gravity.CENTER })
        orb = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.WHITE, Color.parseColor("#3EE6FF"), Color.parseColor("#1B9FC2"))).apply { shape = GradientDrawable.OVAL; gradientType = GradientDrawable.RADIAL_GRADIENT; gradientRadius = 80 * d; setStroke((3 * d).toInt(), Color.parseColor("#3EE6FF")) }
            setOnClickListener { listen() }
        }
        root.addView(orb, LinearLayout.LayoutParams((160 * d).toInt(), (160 * d).toInt()).apply { topMargin = (28 * d).toInt(); bottomMargin = (16 * d).toInt() })
        ObjectAnimator.ofPropertyValuesHolder(orb, PropertyValuesHolder.ofFloat("scaleX", 1f, 1.12f), PropertyValuesHolder.ofFloat("scaleY", 1f, 1.12f)).apply { duration = 1000; repeatCount = -1; repeatMode = ObjectAnimator.REVERSE; start() }
        status = TextView(this).apply { text = "Tap the orb and speak"; setTextColor(Color.parseColor("#3EE6FF")); gravity = Gravity.CENTER }
        root.addView(status)
        log = TextView(this).apply { setTextColor(Color.parseColor("#CFEFFF")); textSize = 15f }
        root.addView(ScrollView(this).apply { addView(log) }, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = (12 * d).toInt() })
        val input = EditText(this).apply { hint = "Or type a command"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); imeOptions = EditorInfo.IME_ACTION_SEND; setSingleLine() }
        input.setOnEditorActionListener { _, _, _ -> val t = input.text.toString().trim(); input.setText(""); if (t.isNotEmpty()) handle(t); true }
        root.addView(input)
        val row = LinearLayout(this)
        row.addView(Button(this).apply { text = "API key"; setOnClickListener { askKey() } })
        row.addView(Button(this).apply { text = "Enable control"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
        root.addView(row)
        setContentView(root)
        say("Systems online, sir.", "en-IN")
    }

    override fun onInit(s: Int) { tts.language = Locale.getDefault() }

    private fun askKey() {
        val e = EditText(this).apply { hint = "Paste your Anthropic API key" }
        AlertDialog.Builder(this).setTitle("API key (stored encrypted on this phone only)").setView(e)
            .setPositiveButton("Save") { _, _ -> vault().edit().putString("key", e.text.toString().trim()).apply() }.show()
    }

    private fun say(t: String, lang: String? = null) {
        runOnUiThread {
            log.append("\nJarvis: $t\n"); status.text = "Speaking"
            if (lang != null) { val l = Locale.forLanguageTag(lang); if (tts.isLanguageAvailable(l) >= TextToSpeech.LANG_AVAILABLE) tts.language = l }
            tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "j")
            status.text = "Tap the orb and speak"
        }
    }

    private fun listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1); return }
        tts.stop(); sr?.destroy(); sr = SpeechRecognizer.createSpeechRecognizer(this)
        sr!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { status.text = "Listening…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(e: Int) { status.text = "Didn’t catch that. Tap again." }
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
            override fun onResults(r: Bundle?) { r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { handle(it) } }
        })
        sr!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM))
    }

    private fun act(a: Int, ok: String) { val s = JarvisService.inst; if (s == null) say("Please tap Enable control and turn on Jarvis Control first.") else { s.performGlobalAction(a); say(ok) } }

    private fun handle(q: String) {
        log.append("\nYou: $q\n")
        val l = q.lowercase(Locale.ROOT)
        if (fin.containsMatchIn(q) || longNumericId.containsMatchIn(q)) { say("I can’t help with payments, banking or money details, sir. That keeps your finances private and safe."); return }
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        Regex("(?:open|launch|start) (.+)").find(l)?.let { m -> if (openApp(m.groupValues[1].trim())) return }
        Regex("(?:call|dial) ([\\d +]+)").find(l)?.let { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + it.groupValues[1].replace(" ", ""))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); say("Opening the dialer. Please press call to confirm."); return }
        Regex("(?:whatsapp|message|text) ([\\d+]{8,15}) (.+)").find(l)?.let {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${it.groupValues[1].replace("+", "")}?text=" + Uri.encode(it.groupValues[2]))))
            say("Message drafted. Tap send in WhatsApp to confirm."); return }
        Regex("timer (?:for )?(\\d+) (second|minute|hour)").find(l)?.let {
            val n = it.groupValues[1].toInt(); val s = n * mapOf("second" to 1, "minute" to 60, "hour" to 3600)[it.groupValues[2]]!!
            startActivity(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, s).putExtra(AlarmClock.EXTRA_SKIP_UI, true)); say("Timer set."); return }
        Regex("(?:search|google|look up) (.+)").find(l)?.let { startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", it.groupValues[1])); say("Searching."); return }
        when {
            "volume up" in l -> { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); say("Volume up.") }
            "volume down" in l -> { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); say("Volume down.") }
            "mute" in l -> { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI); say("Muted.") }
            "brightness" in l -> {
                if (!Settings.System.canWrite(this)) { startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))); say("Please allow system settings for me, then ask again."); return }
                val v = Regex("(\\d+)").find(l)?.groupValues?.get(1)?.toInt()?.coerceIn(0, 100) ?: if ("up" in l || "increase" in l) 90 else 20
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, v * 255 / 100); say("Brightness set to $v percent.") }
            "screenshot" in l -> act(A.GLOBAL_ACTION_TAKE_SCREENSHOT, "Screenshot taken.")
            "lock" in l -> act(A.GLOBAL_ACTION_LOCK_SCREEN, "Locking the screen.")
            "go home" in l || l == "home" -> act(A.GLOBAL_ACTION_HOME, "Home.")
            "go back" in l || l == "back" -> act(A.GLOBAL_ACTION_BACK, "Back.")
            "recent" in l || "switch app" in l -> act(A.GLOBAL_ACTION_RECENTS, "Here are your recent apps.")
            "shut down" in l || "shutdown" in l || "restart" in l || "power off" in l -> act(A.GLOBAL_ACTION_POWER_DIALOG, "Opening the power menu. Please confirm there; Android doesn’t let apps restart the phone directly.")
            else -> askAI(q)
        }
    }

    private fun openApp(name: String): Boolean {
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val hit = apps.firstOrNull { it.loadLabel(pm).toString().lowercase(Locale.ROOT).let { n -> n == name || n.contains(name) } } ?: return false
        val pkg = hit.activityInfo.packageName.lowercase(Locale.ROOT)
        if (finPkg.any { pkg.contains(it) }) { say("I don’t open banking or payment apps, sir. Please open it yourself."); return true }
        startActivity(pm.getLaunchIntentForPackage(hit.activityInfo.packageName)); say("Opening ${hit.loadLabel(pm)}."); return true
    }

    private fun askKey2(): String? = try { vault().getString("key", null) } catch (e: Exception) { null }

    private fun askAI(q: String) {
        val key = askKey2() ?: run { say("Please tap API key and add your key first."); return }
        status.text = "Thinking…"
        Thread {
            try {
                hist.put(JSONObject().put("role", "user").put("content", q))
                val body = JSONObject().put("model", "claude-sonnet-5-5").put("max_tokens", 400).put("system", sys)
                    .put("messages", JSONArray((0 until hist.length()).drop(maxOf(0, hist.length() - 8)).map { hist.get(it) }))
                val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
                c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 15000; c.readTimeout = 30000
                c.setRequestProperty("content-type", "application/json"); c.setRequestProperty("x-api-key", key); c.setRequestProperty("anthropic-version", "2023-06-01")
                c.outputStream.use { it.write(body.toString().toByteArray()) }
                val txt = JSONObject(c.inputStream.bufferedReader().readText()).getJSONArray("content").getJSONObject(0).getString("text")
                val lines = txt.lines(); val lang = lines.firstOrNull()?.takeIf { it.startsWith("LANG:") }?.removePrefix("LANG:")?.trim()
                val reply = (if (lang != null) lines.drop(1) else lines).joinToString("\n").trim()
                hist.put(JSONObject().put("role", "assistant").put("content", reply)); say(reply, lang)
            } catch (e: Exception) { hist.remove(hist.length() - 1); say("I couldn’t reach my AI core. Check internet and your API key.") }
        }.start()
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) { if (r.firstOrNull() == PackageManager.PERMISSION_GRANTED) listen() }
    override fun onDestroy() { sr?.destroy(); tts.shutdown(); super.onDestroy() }
}
