package com.mobsys.moodsense

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.min

/**
 * Custom View: a touch-driven circular mood picker drawn on a Canvas.
 * Five wedges (one per mood level) are rendered around a ring, with the
 * matching emoji shown in the center. When [isEditable] is false the view
 * behaves as a read-only indicator (used on the Detail screen).
 */
class MoodDialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var isEditable: Boolean = true

    var selectedMood: Int = 3
        set(value) {
            field = value.coerceIn(1, 5)
            invalidate()
        }

    var onMoodSelected: ((Int) -> Unit)? = null

    private val moodColors = intArrayOf(
        Color.parseColor("#E53935"),
        Color.parseColor("#FB8C00"),
        Color.parseColor("#FDD835"),
        Color.parseColor("#7CB342"),
        Color.parseColor("#43A047")
    )

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.WHITE
    }
    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val bounds = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f - 16f
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius)

        val sweep = 360f / 5f
        for (i in 0 until 5) {
            segmentPaint.color = moodColors[i]
            segmentPaint.alpha = if (i + 1 == selectedMood) 255 else 90
            canvas.drawArc(bounds, -90f + i * sweep, sweep, true, segmentPaint)
        }
        canvas.drawCircle(cx, cy, radius, ringPaint)

        emojiPaint.textSize = radius * 0.9f
        val emoji = MoodEntry.MOOD_EMOJI[selectedMood - 1]
        val textY = cy - (emojiPaint.descent() + emojiPaint.ascent()) / 2
        canvas.drawText(emoji, cx, textY, emojiPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEditable) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val cx = width / 2f
                val cy = height / 2f
                val dx = event.x - cx
                val dy = event.y - cy
                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                if (angle < 0) angle += 360f
                val segment = (angle / (360f / 5f)).toInt().coerceIn(0, 4)
                val mood = segment + 1
                if (mood != selectedMood) {
                    selectedMood = mood
                    onMoodSelected?.invoke(mood)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
