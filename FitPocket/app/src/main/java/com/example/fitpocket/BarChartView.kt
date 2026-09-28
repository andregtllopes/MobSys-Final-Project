package com.example.fitpocket

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

data class BarChartEntry(val label: String, val value: Int, val highlighted: Boolean = false)

/**
 * CustomView: gráfico de barras desenhado com Canvas para a tela de Progresso
 * (repetições feitas em cada um dos últimos 7 dias). Chame setData() para atualizar,
 * as barras crescem com uma animação simples.
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density

    private var entries: List<BarChartEntry> = emptyList()
    private var revealFraction = 0f
    private var animator: ValueAnimator? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2C2D31.toInt() }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC6FF00.toInt() }
    private val todayStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = 0xFFFFFFFF.toInt()
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9AA0A6.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 11f * density
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 12f * density
        isFakeBoldText = true
    }

    private val barRect = RectF()
    private val stubRect = RectF()

    fun setData(newEntries: List<BarChartEntry>) {
        entries = newEntries
        animator?.cancel()
        revealFraction = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 650
            addUpdateListener {
                revealFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val minHeight = (140f * density).toInt()
        val height = MeasureSpec.getSize(heightMeasureSpec).coerceAtLeast(minHeight)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (entries.isEmpty()) return

        val valueRowHeight = 18f * density
        val labelRowHeight = 18f * density
        val chartTop = valueRowHeight
        val chartBottom = height - labelRowHeight
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
        val maxValue = entries.maxOf { it.value }.coerceAtLeast(1)

        val slot = width.toFloat() / entries.size
        val barWidth = (slot * 0.42f).coerceAtMost(36f * density)
        val stubHeight = 5f * density
        val radius = barWidth / 2f

        entries.forEachIndexed { i, entry ->
            val cx = slot * i + slot / 2f

            if (entry.value > 0) {
                val fraction = (entry.value.toFloat() / maxValue) * revealFraction
                val barHeight = (chartHeight * fraction).coerceAtLeast(stubHeight)
                val top = chartBottom - barHeight
                barRect.set(cx - barWidth / 2f, top, cx + barWidth / 2f, chartBottom)
                canvas.drawRoundRect(barRect, radius, radius, barPaint)
                if (entry.highlighted) {
                    canvas.drawRoundRect(barRect, radius, radius, todayStrokePaint)
                }
                canvas.drawText(entry.value.toString(), cx, top - 6f * density, valuePaint)
            } else {
                // Dia sem treino: um "stub" cinza baixinho, só para o dia continuar visível no eixo.
                stubRect.set(cx - barWidth / 2f, chartBottom - stubHeight, cx + barWidth / 2f, chartBottom)
                canvas.drawRoundRect(stubRect, radius, radius, trackPaint)
                if (entry.highlighted) {
                    canvas.drawRoundRect(stubRect, radius, radius, todayStrokePaint)
                }
            }

            canvas.drawText(entry.label, cx, height.toFloat() - 3f * density, labelPaint)
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
