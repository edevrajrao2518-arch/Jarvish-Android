package com.jarvis.assistant
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator

class OrbView(c: Context) : View(c) {
    var mode = 0
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    init {
        ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 8000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener { t = it.animatedValue as Float; invalidate() }; start()
        }
    }
    override fun onDraw(cv: Canvas) {
        val cx = width / 2f; val cy = height / 2f; val r = minOf(cx, cy)
        val col = when (mode) { 0 -> 0xFF4A5A66.toInt(); 3 -> 0xFFFFB347.toInt(); else -> 0xFF3EE6FF.toInt() }
        val speed = if (mode >= 2) 6.0 else 3.0
        val pulse = 1f + 0.07f * Math.sin(Math.toRadians(t * speed)).toFloat()
        p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.025f; p.color = col
        p.alpha = 110; cv.drawCircle(cx, cy, r * 0.96f, p)
        p.pathEffect = DashPathEffect(floatArrayOf(r * 0.14f, r * 0.09f), 0f); p.alpha = 170
        cv.save(); cv.rotate(t, cx, cy); cv.drawCircle(cx, cy, r * 0.80f, p); cv.restore(); p.pathEffect = null
        p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = r * 0.05f; p.alpha = 230
        val o = RectF(cx - r * 0.64f, cy - r * 0.64f, cx + r * 0.64f, cy + r * 0.64f)
        cv.save(); cv.rotate(-t * 1.5f, cx, cy); cv.drawArc(o, 0f, 80f, false, p); cv.drawArc(o, 180f, 80f, false, p); cv.restore()
        p.style = Paint.Style.FILL; p.alpha = 255
        p.shader = RadialGradient(cx, cy, r * 0.46f * pulse, intArrayOf(Color.WHITE, col, col and 0x00FFFFFF), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        cv.drawCircle(cx, cy, r * 0.46f * pulse, p)
    }
}
