package com.mobility.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.animation.AnimatorSet
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * A semi‑circular speedometer view.
 *
 * * The gauge spans from 135° to 45° (270° sweep).
 * * `setSpeed(kmh)` animates the needle with a spring‑like effect.
 * * `pulseRadar()` triggers a one‑second outer‑ring pulse that is called from the main tick
 *   (synchronised with GPS updates).
 */
class SpeedometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gaugePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(12).toFloat()
        color = Color.rgb(24, 33, 26) // dark‑emerald background
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2).toFloat()
        color = Color.WHITE
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(4).toFloat()
        color = Color.rgb(198, 255, 72) // lime accent
    }
    private val radarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2).toFloat()
        color = Color.rgb(198, 255, 72)
    }

    // Geometry
    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f
    private val gaugeRect = RectF()
    private val radarRect = RectF()

    // Speed handling
    private var currentSpeed = 0f // km/h, raw value
    private var needleAngle = 135f // start angle for 0 km/h
    private val maxSpeed = 180f // define max speed for full sweep

    // Radar animation state
    private var radarProgress = 0f // 0..1 expands from radius to radius*1.5
    private var radarAnimator: ValueAnimator? = null

    init {
        // ensure view gets enough padding for the outer radar ring
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = (Math.min(w, h) / 2f) - dp(16)
        centerX = w / 2f
        centerY = h / 2f
        gaugeRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw gauge background semi‑circle
        canvas.drawArc(gaugeRect, 135f, 270f, false, gaugePaint)
        // Draw tick marks every 10 km/h
        val tickCount = (maxSpeed / 10).toInt()
        for (i in 0..tickCount) {
            val angle = Math.toRadians((135 + i * (270f / tickCount)).toDouble())
            val inner = radius - dp(8)
            val outer = radius
            val sx = centerX + (inner * cos(angle)).toFloat()
            val sy = centerY + (inner * sin(angle)).toFloat()
            val ex = centerX + (outer * cos(angle)).toFloat()
            val ey = centerY + (outer * sin(angle)).toFloat()
            canvas.drawLine(sx, sy, ex, ey, tickPaint)
        }
        // Draw needle
        val needleRad = Math.toRadians(needleAngle.toDouble())
        val nx = centerX + (radius - dp(12)) * cos(needleRad).toFloat()
        val ny = centerY + (radius - dp(12)) * sin(needleRad).toFloat()
        canvas.drawLine(centerX, centerY, nx, ny, needlePaint)
        // Draw radar pulse if active
        if (radarProgress > 0f) {
            val r = radius + dp(10) * radarProgress
            radarRect.set(centerX - r, centerY - r, centerX + r, centerY + r)
            radarPaint.alpha = ((1 - radarProgress) * 255).toInt()
            canvas.drawArc(radarRect, 135f, 270f, false, radarPaint)
        }
    }

    /** Animate needle to the given speed (km/h). */
    fun setSpeed(kmh: Float) {
        val targetAngle = 135f + (kmh.coerceIn(0f, maxSpeed) / maxSpeed) * 270f
        val animator = ValueAnimator.ofFloat(needleAngle, targetAngle)
        animator.duration = 500L
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            needleAngle = animation.animatedValue as Float
            invalidate()
        }
        animator.start()
        currentSpeed = kmh
    }

    /** Trigger a one‑second radar pulse – call this once per GPS tick. */
    fun pulseRadar() {
        radarAnimator?.cancel()
        radarAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                radarProgress = anim.animatedValue as Float
                invalidate()
            }
        }
        radarAnimator?.start()
    }
}
