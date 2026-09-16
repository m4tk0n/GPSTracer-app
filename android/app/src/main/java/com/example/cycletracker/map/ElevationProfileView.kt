package com.example.cycletracker.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * Jednoduchy vyskovy profil (vzdalenost na X, nadmorska vyska na Y).
 * Vlastni View misto externi knihovny grafu - min. zavislosti, rychlejsi build.
 */
class ElevationProfileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Seznam (kumulativni vzdalenost v km, nadmorska vyska v m). */
    private var points: List<Pair<Float, Float>> = emptyList()

    private val linePaint = Paint().apply {
        color = Color.parseColor("#2E7D32")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint().apply {
        color = Color.parseColor("#332E7D32")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val axisPaint = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        strokeWidth = 2f
    }
    private val labelPaint = Paint().apply {
        color = Color.parseColor("#475569")
        textSize = 26f
        isAntiAlias = true
    }

    fun setData(newPoints: List<Pair<Float, Float>>) {
        points = newPoints
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return

        val paddingLeft = 8f
        val paddingRight = 8f
        val paddingTop = 8f
        val paddingBottom = 24f
        val w = width - paddingLeft - paddingRight
        val h = height - paddingTop - paddingBottom
        if (w <= 0 || h <= 0) return

        val minEle = points.minOf { it.second }
        val maxEle = points.maxOf { it.second }.coerceAtLeast(minEle + 1f)
        val maxDist = points.maxOf { it.first }.coerceAtLeast(0.01f)

        fun xFor(d: Float) = paddingLeft + (d / maxDist) * w
        fun yFor(e: Float) = paddingTop + h - ((e - minEle) / (maxEle - minEle)) * h

        canvas.drawLine(paddingLeft, paddingTop + h, paddingLeft + w, paddingTop + h, axisPaint)

        val linePath = Path()
        val fillPath = Path()
        points.forEachIndexed { i, (d, e) ->
            val x = xFor(d)
            val y = yFor(e)
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, paddingTop + h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(xFor(points.last().first), paddingTop + h)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        // Popisky min/max vysky
        canvas.drawText("${maxEle.toInt()} m", paddingLeft, paddingTop + 22f, labelPaint)
        canvas.drawText("${minEle.toInt()} m", paddingLeft, paddingTop + h - 4f, labelPaint)
        canvas.drawText("${"%.1f".format(maxDist)} km", paddingLeft + w - 90f, paddingTop + h + 20f, labelPaint)
    }
}
