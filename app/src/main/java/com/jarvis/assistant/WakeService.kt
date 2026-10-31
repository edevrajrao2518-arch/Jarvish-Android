package com.jarvis.assistant
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Always-on "Hey Jarvis" listener. Runs as a visible foreground service (notification shown) so Android allows the mic. */
class WakeService : Service(), TextToSpeech.OnInitListener {
    private var sr: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private val h = Handler(Looper.getMainLooper())
    private var running = false
    private var speaking = false
    private var awakeUntil = 0L
    private val wake = Regex("(jarvis|jarvish|jarwis|jervis|jarvice|जार्विस|जारविस|जर्विस)")

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate(); Bus.service = this
        tts = TextToSpeech(this, this)
    }

    override fun onInit(s: Int) {
        ttsReady = s == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts.language = Locale("en", "IN")
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { h.post { afterSpeak() } }
                @Deprecated("x") override fun onError(id: String?) { h.post { afterSpeak() } }
            })
        }
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val ch = "jarvis"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(ch, "Jarvis listening", NotificationManager.IMPORTANCE_LOW))
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, ch).setContentTitle("Jarvis is on").setContentText("Say “Hey Jarvis”")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentIntent(pi).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) else startForeground(1, n)
        if (!running) { running = true; Bus.post(0, "Systems online. Say “Hey Jarvis” and then your command."); startRec() }
        return START_STICKY
    }

    private fun startRec() {
        if (!running || speaking) return
        sr?.destroy(); sr = null
        if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            Bus.post(0, "On-device speech recognition isn’t available. Voice is off to keep audio private; you can still type commands.")
            Bus.updateState(0); stopSelf(); return
        }
        Bus.updateState(if (System.currentTimeMillis() < awakeUntil) 2 else 1)
        sr = SpeechRecognizer.createOnDeviceSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}
                override fun onError(e: Int) {
                    if (e == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) { Bus.post(0, "I need microphone permission."); stopSelf(); return }
                    if (e == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || e == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
                        Bus.post(0, "This phone’s on-device recognizer doesn’t support the selected language. You can still type commands.")
                        stopSelf(); return
                    }
                    h.postDelayed({ startRec() }, if (e == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || e == SpeechRecognizer.ERROR_CLIENT) 1500 else 300)
                }
                override fun onResults(r: Bundle?) { process(r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()) }
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true))
        }
    }

    private fun process(alts: List<String>) {
        for (a in alts) {
            val low = a.lowercase(Locale.ROOT); val m = wake.find(low) ?: continue
            val cmd = low.substring(m.range.last + 1).trim(' ', ',', '.')
            if (cmd.isEmpty()) { awakeUntil = System.currentTimeMillis() + 10000; speak("Yes, sir?") } else { awakeUntil = 0; handleText(cmd) }
            return
        }
        if (System.currentTimeMillis() < awakeUntil && alts.isNotEmpty()) { awakeUntil = 0; handleText(alts[0]); return }
        startRec()
    }

    fun handleText(t: String) { Bus.post(1, t); speak(try { Brain.run(this, t) } catch (e: Exception) { "Sorry, something went wrong." }) }

    private fun speak(t: String) {
        Bus.post(0, t); speaking = true; Bus.updateState(3)
        sr?.destroy(); sr = null; h.removeCallbacksAndMessages(null)
        if (!ttsReady) { h.postDelayed({ afterSpeak() }, 800); return }
        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "u")
    }

    private fun afterSpeak() { speaking = false; startRec() }

    override fun onDestroy() {
        running = false; h.removeCallbacksAndMessages(null); sr?.destroy()
        if (::tts.isInitialized) tts.shutdown()
        Bus.service = null; Bus.updateState(0); super.onDestroy()
    }
}

