package com.example.fitpocket

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** CustomView: animated progress ring drawn on a Canvas. Call setProgress(reps, goal) to update. */
class ProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val strokePx = 24f * density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        color = 0xFF2C2D31.toInt()
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = 0xFFC6FF00.toInt()
    }
    private val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 72f * density
        isFakeBoldText = true
    }
    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9AA0A6.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 20f * density
    }

    private val oval = RectF()
    private var reps = 0
    private var goal = 10
    private var fraction = 0f
    private var animator: ValueAnimator? = null

    fun setProgress(reps: Int, goal: Int) {
        this.reps = reps
        this.goal = goal.coerceAtLeast(1)
        val target = (reps.toFloat() / this.goal).coerceIn(0f, 1f)
        animator?.cancel()
        animator = ValueAnimator.ofFloat(fraction, target).apply {
            duration = 250
            addUpdateListener {
                fraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val half = strokePx / 2
        oval.set(
            half + paddingLeft,
            half + paddingTop,
            width - half - paddingRight,
            height - half - paddingBottom
        )
        // Gray track + progress arc starting at the top (-90°).
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)
        canvas.drawArc(oval, -90f, 360f * fraction, false, progressPaint)

        val cx = width / 2f
        val cy = height / 2f
        val baseline = cy - (countPaint.ascent() + countPaint.descent()) / 2
        canvas.drawText(reps.toString(), cx, baseline, countPaint)
        canvas.drawText("of $goal", cx, cy + 56f * density, goalPaint)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
