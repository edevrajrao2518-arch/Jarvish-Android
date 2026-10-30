package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private lateinit var status: TextView
    private lateinit var logView: TextView
    private lateinit var input: EditText
    private val log = StringBuilder()
    private val bg = Color.rgb(7, 14, 24)
    private val cyan = Color.rgb(91, 229, 224)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg; window.navigationBarColor = bg
        tts = TextToSpeech(this, this)
        buildUi()
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) SpeechRecognizer.createOnDeviceSpeechRecognizer(this) else null
        }
        addLog("Jarvis ready. Voice stays on-device when supported; typed commands are always available.")
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22, 20, 22, 18); setBackgroundColor(bg) }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        val icon = ImageView(this).apply { setImageResource(com.jarvis.assistant.R.drawable.ic_jarvis); contentDescription = "Jarvis logo" }
        body.addView(icon, LinearLayout.LayoutParams(118,118).apply { topMargin=20; bottomMargin=12 })
        body.addView(TextView(this).apply { text="JARVIS"; textSize=30f; letterSpacing=.22f; setTextColor(Color.WHITE); typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER })
        body.addView(TextView(this).apply { text="PERSONAL VOICE ASSISTANT"; textSize=11f; letterSpacing=.16f; setTextColor(cyan); gravity=Gravity.CENTER; setPadding(0,5,0,20) })
        val mic = Button(this).apply { text="🎙  TAP TO SPEAK"; setTextColor(bg); setBackgroundTintList(android.content.res.ColorStateList.valueOf(cyan)); setOnClickListener { listen() } }
        body.addView(mic, LinearLayout.LayoutParams(-1,58).apply { bottomMargin=10 })
        status = TextView(this).apply { text="Ready"; textSize=14f; setTextColor(cyan); gravity=Gravity.CENTER; setPadding(0,8,0,12) }
        body.addView(status)
        input = EditText(this).apply { hint="Type a command…"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); setSingleLine(true); imeOptions=EditorInfo.IME_ACTION_SEND; setPadding(14,10,14,10); setBackgroundColor(Color.rgb(25,35,48)); setOnEditorActionListener { _, action, _ -> if(action==EditorInfo.IME_ACTION_SEND){ submit(); true } else false } }
        body.addView(input, LinearLayout.LayoutParams(-1,54))
        val send = Button(this).apply { text="RUN COMMAND"; setTextColor(bg); setBackgroundTintList(android.content.res.ColorStateList.valueOf(cyan)); setOnClickListener { submit() } }
        body.addView(send, LinearLayout.LayoutParams(-1,50).apply { topMargin=8; bottomMargin=16 })
        body.addView(TextView(this).apply { text="SESSION LOG"; setTextColor(cyan); textSize=12f; typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.START; setPadding(0,8,0,5) })
        logView = TextView(this).apply { textSize=13f; setTextColor(Color.LTGRAY); setPadding(12,12,12,12); setBackgroundColor(Color.rgb(16,24,36)); minHeight=150 }
        body.addView(logView, LinearLayout.LayoutParams(-1,-2))
        body.addView(TextView(this).apply { text="PRIVACY: no API key • no internet permission • no saved chat"; setTextColor(Color.GRAY); textSize=11f; gravity=Gravity.CENTER; setPadding(0,20,0,8) })
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f)); setContentView(root)
    }

    private fun listen() {
        if (recognizer == null) { status.text="On-device speech recognition is unavailable. Type a command instead."; addLog(status.text.toString()); return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 7); return }
        status.text="Listening…"
        recognizer?.setRecognitionListener(object: RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text="Processing on device…" }
            override fun onError(error: Int) { status.text="Didn't catch that. Try again or type."; addLog(status.text.toString()) }
            override fun onResults(results: Bundle?) { val said=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); input.setText(said); runCommand(said) }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault()).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
        if (Build.VERSION.SDK_INT>=31) intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)
        recognizer?.startListening(intent)
    }
    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<out String>, grantResults:IntArray){ super.onRequestPermissionsResult(requestCode,permissions,grantResults); if(requestCode==7 && grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED) listen() else { status.text="Microphone permission not granted. You can type commands."; addLog(status.text.toString()) } }
    private fun submit(){ val text=input.text.toString().trim(); if(text.isNotEmpty()) runCommand(text) }
    private fun runCommand(raw:String) {
        val q=raw.trim(); if(q.isEmpty()) return
        addLog("You: $q"); input.setText("")
        val s=q.lowercase(Locale.ROOT)
        if (Regex("\\b(bank|banking|account number|card number|credit card|debit card|cvv|pin|password|otp|one time password|upi|ifsc|routing number|send money|transfer money|payment)\\b").containsMatchIn(s)) { reply("For your safety, Jarvis won't process or share banking, payment, or authentication details."); return }
        when {
            s in listOf("help","what can you do","commands") -> reply("Try: what time is it, open YouTube, open WhatsApp, call 123…, volume up, volume down, open display settings, search the web for …, or set a timer for 5 minutes.")
            s.contains("time") || s.contains("date") -> reply(SimpleDateFormat("EEEE, d MMMM yyyy, h:mm a",Locale.getDefault()).format(Date()))
            s.contains("volume up") || s.contains("increase volume") -> { (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI); reply("Volume increased.") }
            s.contains("volume down") || s.contains("decrease volume") -> { (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI); reply("Volume decreased.") }
            s.contains("display settings") || s.contains("brightness settings") -> { startActivity(Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)); reply("Opening display settings.") }
            s.startsWith("call ") -> { val number=q.substringAfter("call ").trim(); startActivity(Intent(Intent.ACTION_DIAL,android.net.Uri.parse("tel:"+android.net.Uri.encode(number)))); reply("Opening the dialer. Review and place the call there.") }
            s.contains("whatsapp") && (s.contains("message") || s.contains("text")) -> { val target=q.substringAfter("to ","").substringBefore(" saying ").trim(); val msg=q.substringAfter(" saying ","").trim(); if(msg.isBlank()) reply("Say: message [contact] saying [message].") else { val uri=if(target.isBlank()) android.net.Uri.parse("https://wa.me/?text="+android.net.Uri.encode(msg)) else android.net.Uri.parse("https://wa.me/"+android.net.Uri.encode(target)+"?text="+android.net.Uri.encode(msg)); startActivity(Intent(Intent.ACTION_VIEW,uri)); reply("Opening WhatsApp with a draft. Review it before sending.") } }
            s.startsWith("search the web for ") || s.startsWith("search for ") -> { val term=if(s.startsWith("search the web for ")) q.substring(19).trim() else q.substring(11).trim(); startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://www.google.com/search?q="+android.net.Uri.encode(term)))); reply("Opening your browser for that search.") }
            s.startsWith("set a timer for ") -> { val m=Regex("(\\d+)\\s*(second|minute|hour)").find(s); if(m==null) reply("Use: set a timer for 5 minutes.") else { val mult=when(m.groupValues[2]){"second"->1L;"hour"->3600L;else->60L}; val secs=m.groupValues[1].toLong()*mult; startActivity(Intent(android.provider.AlarmClock.ACTION_SET_TIMER).putExtra(android.provider.AlarmClock.EXTRA_LENGTH,secs.toInt()).putExtra(android.provider.AlarmClock.EXTRA_MESSAGE,"Jarvis timer").putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI,false)); reply("Opening the timer for ${m.value}.") } }
            s.startsWith("open ") -> openApp(q.substringAfter("open ").trim())
            else -> reply("I can handle simple phone shortcuts locally. Say help to see available commands.")
        }
    }
    private fun openApp(name:String) {
        val match=packageManager.getInstalledApplications(PackageManager.GET_META_DATA).firstOrNull { packageManager.getApplicationLabel(it).toString().equals(name,true) || packageManager.getApplicationLabel(it).toString().contains(name,true) }
        val launch=match?.let { packageManager.getLaunchIntentForPackage(it.packageName) }
        if(launch!=null){ startActivity(launch); reply("Opening ${packageManager.getApplicationLabel(match)}.") } else reply("I couldn't find an installed app named $name.")
    }
    private fun reply(text:String){ status.text=text; addLog("Jarvis: $text"); tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"jarvis") }
    private fun addLog(line:String){ val stamp=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date()); log.append("[$stamp] $line\n"); if(::logView.isInitialized) logView.text=log.toString() }
    override fun onInit(statusCode:Int){ if(statusCode==TextToSpeech.SUCCESS) tts?.language=Locale.getDefault() }
    override fun onDestroy(){ recognizer?.destroy(); tts?.shutdown(); super.onDestroy() }
}
