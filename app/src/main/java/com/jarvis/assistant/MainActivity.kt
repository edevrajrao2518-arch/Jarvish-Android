package com.jarvis.assistant
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*

private val BG = Color.parseColor("#050B14")
private val PANEL = Color.parseColor("#0A1626")
private val LINE = Color.parseColor("#12304D")
private val CY = Color.parseColor("#3EE6FF")
private val TX = Color.parseColor("#CFEFFF")
private val MUT = Color.parseColor("#6F93AA")

class MainActivity : Activity() {
    private lateinit var orb: OrbView
    private lateinit var stateTv: TextView
    private lateinit var list: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var toggle: Button
    private var shown = 0
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun box(fill: Int, stroke: Int, r: Int) = GradientDrawable().apply { setColor(fill); cornerRadius = dp(r).toFloat(); setStroke(dp(1), stroke) }
    private fun btn(t: String, fill: Int, txt: Int) = Button(this).apply { text = t; isAllCaps = false; setTextColor(txt); textSize = 14f; background = box(fill, CY, 22); minHeight = 0; minimumHeight = dp(44); stateListAnimator = null }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = BG; window.navigationBarColor = BG
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG); setPadding(dp(16), dp(24), dp(16), dp(12)) }
        root.addView(TextView(this).apply { text = "JARVIS"; textSize = 28f; letterSpacing = 0.35f; typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); setTextColor(CY); gravity = Gravity.CENTER })
        root.addView(TextView(this).apply { text = "Say “Hey Jarvis”"; textSize = 14f; setTextColor(MUT); gravity = Gravity.CENTER })
        orb = OrbView(this)
        root.addView(orb, LinearLayout.LayoutParams(dp(180), dp(180)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(12) })
        stateTv = TextView(this).apply { textSize = 13f; setTextColor(CY); gravity = Gravity.CENTER; letterSpacing = 0.1f }
        root.addView(stateTv)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll = ScrollView(this).apply { addView(list); isVerticalScrollBarEnabled = false }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(10); bottomMargin = dp(8) })
        val input = EditText(this).apply { hint = "Type a command…"; setHintTextColor(MUT); setTextColor(TX); textSize = 15f; setSingleLine(); imeOptions = EditorInfo.IME_ACTION_SEND; background = box(PANEL, LINE, 22); setPadding(dp(16), dp(10), dp(16), dp(10)) }
        val send = btn("Send", CY, BG)
        val go = { val t = input.text.toString().trim(); input.setText(""); if (t.isNotEmpty()) { val s = Bus.service; if (s != null) s.handleText(t) else { Bus.post(1, t); Bus.post(0, "Jarvis is off. Tap “Turn on Jarvis” first.") } } }
        input.setOnEditorActionListener { _, _, _ -> go(); true }
        send.setOnClickListener { go() }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        bar.addView(input, LinearLayout.LayoutParams(0, -2, 1f)); bar.addView(send, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        root.addView(bar)
        toggle = btn("Turn on Jarvis", PANEL, CY)
        toggle.setOnClickListener { if (Bus.state == 0) startJarvis() else stopService(Intent(this, WakeService::class.java)) }
        val setup = btn("Setup", PANEL, TX); setup.setOnClickListener { setupDialog() }
        val row = LinearLayout(this)
        row.addView(toggle, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) }); row.addView(setup, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        setContentView(root)
        if (Bus.log.isEmpty()) Bus.post(0, "Hello! For full control, open Setup and allow each item once.")
        startJarvis()
    }

    private fun refresh() {
        while (shown < Bus.log.size) { val (who, t) = Bus.log[shown]; bubble(who, t); shown++ }
        stateTv.text = when (Bus.state) { 0 -> "HANDS-FREE OFF"; 1 -> "LISTENING FOR “HEY JARVIS”"; 2 -> "LISTENING…"; else -> "SPEAKING" }
        orb.mode = Bus.state; orb.invalidate()
        toggle.text = if (Bus.state == 0) "Turn on Jarvis" else "Turn off Jarvis"
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun bubble(who: Int, t: String) {
        val me = who == 1
        val tv = TextView(this).apply { text = t; textSize = 15f; setTextColor(if (me) TX else CY); background = box(if (me) Color.parseColor("#10304A") else PANEL, if (me) Color.parseColor("#10304A") else LINE, 16); setPadding(dp(14), dp(9), dp(14), dp(9)); maxWidth = dp(270) }
        list.addView(tv, LinearLayout.LayoutParams(-2, -2).apply { gravity = if (me) Gravity.END else Gravity.START; topMargin = dp(6) })
    }

    private fun startJarvis() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS), 1); return }
        val i = Intent(this, WakeService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startJarvis()
        else Bus.post(0, "I need the microphone permission to hear “Hey Jarvis”.")
    }

    private fun setupDialog() {
        val pkg = Uri.parse("package:$packageName")
        val items = arrayOf("1. Allow restricted settings (App info → ⋮ menu)", "2. Turn on Jarvis Control (accessibility)", "3. Allow system settings (brightness)", "4. Battery: set to Unrestricted")
        AlertDialog.Builder(this).setTitle("One-time setup").setItems(items) { _, w ->
            startActivity(when (w) {
                0 -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
                1 -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                2 -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkg)
                else -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            })
        }.setNegativeButton("Close", null).show()
    }

    override fun onResume() { super.onResume(); Bus.onChange = { refresh() }; refresh() }
    override fun onPause() { Bus.onChange = null; super.onPause() }
}
