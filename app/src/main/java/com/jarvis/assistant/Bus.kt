package com.jarvis.assistant

/** In-memory only. Nothing is saved to disk; the chat disappears when the app is closed. */
object Bus {
    val log = ArrayList<Pair<Int, String>>()   // 0 = Jarvis, 1 = you
    var state = 0                              // 0 off, 1 waiting for wake word, 2 listening, 3 speaking
    var service: WakeService? = null
    var onChange: (() -> Unit)? = null
    fun post(who: Int, text: String) { log.add(who to text); onChange?.invoke() }
    fun setState(s: Int) { state = s; onChange?.invoke() }
}
