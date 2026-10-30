package ai.jarvish.mobile

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Local-first assistant shell. Actions are allowlisted and no arbitrary commands are executed. */
class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val bg = Color.rgb(7, 17, 27)
    private val panel = Color.rgb(11, 27, 41)
    private val cyan = Color.rgb(83, 230, 255)
    private val white = Color.rgb(233, 247, 255)
    private val muted = Color.rgb(138, 168, 184)
    private lateinit var log: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var status: TextView
    private lateinit var micButton: Button
    private lateinit var input: EditText
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var listening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        tts = TextToSpeech(this, this)
        buildUi()
        addMessage("JARVISH", "Systems online. I’m running in local mode. Your messages are not sent to an AI service.")
        addMessage("PRIVACY", "Voice recognition uses on-device speech recognition when your phone supports it. If it does not, you can type instead.")
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(16)); setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(label("JARVISH", 20, cyan, true), LinearLayout.LayoutParams(0, -2, 1f))
        status = label("LOCAL · READY", 11, muted, true)
        header.addView(status)
        root.addView(header)
        val line = View(this).apply { setBackgroundColor(Color.rgb(22, 56, 74)) }
        root.addView(line, LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(14); bottomMargin = dp(15) })
        root.addView(label("VOICE INTERFACE", 11, cyan, true))
        root.addView(label("At your service.", 28, white, false).apply { setPadding(0, dp(8), 0, dp(2)) })
        root.addView(label("Speak a request or type below. I’ll ask before opening another app or sharing a search.", 14, muted, false))
        micButton = Button(this).apply { text = "🎙   ACTIVATE VOICE"; isAllCaps = false; setTextColor(bg); setBackgroundColor(cyan); setOnClickListener { toggleListening() } }
        root.addView(micButton, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(16); bottomMargin = dp(18) })
        root.addView(label("COMMUNICATIONS", 11, cyan, true))
        scroll = ScrollView(this).apply { isFillViewport = false; background = rounded(panel, 16) }
        log = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(8), dp(14), dp(8)) }
        scroll.addView(log)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        input = EditText(this).apply {
            hint = "Talk to Jarvish…"; setHintTextColor(muted); setTextColor(white); setSingleLine(true); imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(12), dp(4), dp(12), dp(4)); background = rounded(panel, 10)
            setOnEditorActionListener { _, _, _ -> sendTyped(); true }
        }
        row.addView(input, LinearLayout.LayoutParams(0, dp(52), 1f).apply { rightMargin = dp(8) })
        val send = Button(this).apply { text = "SEND"; isAllCaps = false; setTextColor(cyan); setBackgroundColor(Color.rgb(18, 51, 73)); setOnClickListener { sendTyped() } }
        row.addView(send, LinearLayout.LayoutParams(dp(88), dp(52)))
        root.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        val quick = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf("Help", "Time", "Open app", "Search web").forEach { text ->
            val b = Button(this).apply { this.text = text; isAllCaps = false; textSize = 11f; setTextColor(muted); setBackgroundColor(bg); setOnClickListener { when(text) { "Help" -> handle("what can you do"); "Time" -> handle("what time is it"); "Open app" -> openAppDialog(); else -> searchDialog() } } }
            quick.addView(b, LinearLayout.LayoutParams(0, dp(45), 1f))
        }
        root.addView(quick)
        root.addView(label("LOCAL-FIRST · NO PAYMENT OR BANKING ACTIONS", 10, muted, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) })
        setContentView(root)
    }

    private fun sendTyped() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        input.setText("")
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken, 0)
        handle(text)
    }

    private fun handle(text: String) {
        addMessage("YOU", text)
        val q = text.lowercase(Locale.ROOT)
        val answer = when {
            looksFinancial(q) -> "For your privacy and safety, Jarvish cannot handle banking details, payments, transfers, or financial transactions. I will not send financial information to anyone."
            listOf("what time", "time is it", "current time").any(q::contains) -> "It is ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())}."
            listOf("what date", "today's date", "what day", "today").any(q::contains) -> "Today is ${DateFormat.getDateInstance(DateFormat.FULL).format(Date())}."
            listOf("what can you do", "help", "capabilities").any(q::contains) -> "I can tell the time and date, speak replies, adjust media volume, open an app you name, and prepare a web search after you approve it. I cannot make payments, execute arbitrary commands, or read other apps. More abilities need explicit Android permissions and a privacy review."
            listOf("volume up", "increase volume", "louder").any(q::contains) -> { adjustVolume(true); "Media volume increased." }
            listOf("volume down", "decrease volume", "quieter").any(q::contains) -> { adjustVolume(false); "Media volume decreased." }
            q.startsWith("open ") -> openApp(q.removePrefix("open ").trim())
            q.startsWith("search the web for ") -> askSearch(q.removePrefix("search the web for ").trim())
            q.startsWith("search for ") -> askSearch(q.removePrefix("search for ").trim())
            listOf("hello", "hi jarvish", "hey jarvish").any(q::contains) -> greeting(text)
            else -> "I’m in private local mode, so I don’t send your question to an online AI. I can help with device commands I support; for broader reasoning, an on-device AI model will need to be added."
        }
        addMessage("JARVISH", answer)
        speak(answer, text)
    }

    private fun greeting(text: String): String {
        val locale = Locale.getDefault().language
        return when (locale) {
            "hi" -> "नमस्ते। मैं तैयार हूँ।"
            "es" -> "Hola. Estoy listo para ayudarte."
            "fr" -> "Bonjour. Je suis prêt à vous aider."
            else -> "Hello. I’m ready to help."
        }
    }

    private fun looksFinancial(q: String): Boolean {
        val markers = listOf("bank", "banking", "upi", "credit card", "debit card", "card number", "cvv", "account number", "ifsc", "swift code", "routing number", "transfer money", "send money", "pay bill", "make payment", "transaction", "cryptocurrency", "wallet balance")
        val hasLongNumber = Regex("(?:\\d[ -]?){12,19}").containsMatchIn(q)
        return hasLongNumber || markers.any(q::contains)
    }

    private fun adjustVolume(up: Boolean) {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }

    private fun openAppDialog() {
        val edit = EditText(this).apply { hint = "App name, for example Camera"; setTextColor(white); setHintTextColor(muted) }
        AlertDialog.Builder(this).setTitle("Open an app").setMessage("Jarvish will only launch the app you name.").setView(edit)
            .setNegativeButton("Cancel", null).setPositiveButton("Open") { _, _ -> openApp(edit.text.toString()) }.show()
    }

    private fun openApp(name: String): String {
        if (name.isBlank()) return "Tell me which app to open."
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = packageManager.queryIntentActivities(launcher, 0).firstOrNull { info ->
            info.loadLabel(packageManager).toString().equals(name, true) || info.loadLabel(packageManager).toString().contains(name, true)
        }
        val intent = match?.activityInfo?.let { Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(it.packageName, it.name) }
        return if (intent != null) {
            try { startActivity(intent); "Opening ${match?.loadLabel(packageManager) ?: name}." }
            catch (_: ActivityNotFoundException) { "I couldn’t open that app." }
        } else "I couldn’t find an app named ‘$name’."
    }

    private fun searchDialog() {
        val edit = EditText(this).apply { hint = "Search terms"; setTextColor(white); setHintTextColor(muted) }
        AlertDialog.Builder(this).setTitle("Web search").setMessage("Your search will be sent to your selected browser/search provider. Continue only if you’re comfortable sharing these words.").setView(edit)
            .setNegativeButton("Cancel", null).setPositiveButton("Review & search") { _, _ -> askSearch(edit.text.toString()) }.show()
    }

    private fun askSearch(query: String): String {
        if (query.isBlank()) return "What would you like me to search for?"
        if (looksFinancial(query.lowercase(Locale.ROOT))) return "I won’t send financial or banking details to a search provider."
        AlertDialog.Builder(this).setTitle("Send search to web?").setMessage("This query will leave your phone: “$query”").setNegativeButton("Cancel", null).setPositiveButton("Search") { _, _ ->
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))) }
            catch (_: ActivityNotFoundException) { Toast.makeText(this, "No browser is available.", Toast.LENGTH_SHORT).show() }
        }.show()
        return "I’ve prepared the search. Confirm on screen to send it."
    }

    private fun toggleListening() {
        if (listening) { recognizer?.cancel(); stopListening(); return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 40); return
        }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            addMessage("PRIVACY", "This phone does not provide on-device speech recognition. Type your request instead; Jarvish will not switch to a network recognizer.")
            return
        }
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { status.text = "LISTENING" }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { stopListening() }
                    override fun onError(error: Int) { stopListening(); addMessage("VOICE", "I didn’t catch that. Please try again or type your request.") }
                    override fun onResults(results: Bundle?) {
                        val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        stopListening()
                        if (!phrase.isNullOrBlank()) handle(phrase)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
                startListening(intent)
            }
            listening = true
            micButton.text = "■   STOP LISTENING"
        } catch (_: Exception) {
            stopListening(); addMessage("VOICE", "On-device voice recognition could not start on this phone.")
        }
    }

    private fun stopListening() { listening = false; status.text = "LOCAL · READY"; micButton.text = "🎙   ACTIVATE VOICE" }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 40 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) toggleListening()
        else if (requestCode == 40) addMessage("PRIVACY", "Microphone permission was not granted. Typing still works.")
    }

    private fun speak(message: String, userText: String) {
        if (!ttsReady) return
        val language = languageFor(userText)
        val result = tts?.setLanguage(language)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) tts?.setLanguage(Locale.getDefault())
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvish-reply")
    }

    private fun languageFor(text: String): Locale = when {
        Regex("[\\u0900-\\u097F]").containsMatchIn(text) -> Locale("hi")
        Regex("[áéíóúñ¿¡]").containsMatchIn(text.lowercase()) -> Locale("es")
        Regex("[àâçéèêëîïôûùüÿœ]").containsMatchIn(text.lowercase()) -> Locale("fr")
        else -> Locale.getDefault()
    }

    private fun addMessage(who: String, message: String) {
        if (!::log.isInitialized) return
        val block = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(8), dp(4), dp(8)) }
        block.addView(label(who, 10, if (who == "YOU") Color.rgb(189, 158, 255) else cyan, true))
        block.addView(label(message, 14, white, false).apply { setPadding(0, dp(3), 0, 0) })
        log.addView(block)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(color); if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), Color.rgb(25, 60, 77)) }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onInit(statusCode: Int) { ttsReady = statusCode == TextToSpeech.SUCCESS }
    override fun onDestroy() {
        recognizer?.destroy()
        tts?.stop(); tts?.shutdown()
        super.onDestroy()
    }
}
