package at.rtr.rmbt.android.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import at.rtr.rmbt.android.R
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.GraphItemRecord
import at.specure.data.entity.TestResultGraphItemRecord
import timber.log.Timber
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class SpeedLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LineChart(context, attrs), ResultChart {

    private var pathStroke: Path = Path()
    private var paintStroke: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorAccent)
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var pathFill: Path = Path()
    private var paintFill: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var startTime: Long = -1

    private var chartPoints: ArrayList<PointF> = ArrayList()

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed && right - left > 0 && bottom - top > 0) {
            val lineGradientBitmap = ResourcesCompat.getDrawable(resources, R.drawable.bg_line_chart_gradient_path, null)
                ?.let { convertToBitmap(it, right - left, bottom - top) }
            paintStroke.shader = lineGradientBitmap?.let { BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
            val colors = intArrayOf(
                ContextCompat.getColor(context, R.color.speed_graph_gradient_fill_start),
                ContextCompat.getColor(context, R.color.speed_graph_gradient_fill_end)
            )
            paintFill.shader = LinearGradient(0f, 0f, 0f, getChartHeight(), colors, floatArrayOf(0.2f, 1f), Shader.TileMode.CLAMP)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (pathStroke.isEmpty && chartPoints.isNotEmpty()) {
            calculatePath()
        }

        canvas?.drawPath(pathFill, paintFill)
        canvas?.drawPath(pathStroke, paintStroke)
    }

    fun addGraphItems(graphItems: List<GraphItemRecord>?) {

        pathStroke.rewind()
        pathFill.rewind()
        if (graphItems != null && graphItems.isNotEmpty()) {

            chartPoints = ArrayList()

            if (graphItems[0].progress > 0) {
                chartPoints.add(PointF(0.0f, toLog(graphItems[0].value)))
            }
            for (index in graphItems.indices) {

                val x = graphItems[index].progress / 100.0f
                val y = toLog(graphItems[index].value)

                Timber.d("speedtest speed ${graphItems[index].value}")
                chartPoints.add(PointF(x, y))
            }
        }
        invalidate()
    }

    override fun addResultGraphItems(graphItems: List<TestResultGraphItemRecord>?, networkType: NetworkTypeCompat) {

        pathStroke.rewind()
        pathFill.rewind()

        graphItems?.let { items ->

            chartPoints = ArrayList()

            val maxValue = items.maxByOrNull { it.time }?.time
            if (maxValue != null) {

                if (((items[0].time / maxValue.toFloat()) * 100.0f) > 0) {
                    chartPoints.add(PointF(0.0f, toLog(graphItems[0].value * 8000 / graphItems[0].time)))
                }

                for (index in items.indices) {
                    val x = items[index].time / maxValue.toFloat()
                    val y = toLog(graphItems[index].value * 8000 / graphItems[index].time)
                    chartPoints.add(PointF(x, y))
                    Timber.d("itemsdisplaytest x $x y $y width ${getChartWidth()} height ${getChartHeight()}")
                }
            }
        }
        invalidate()
    }

    /**
     * This function is use for calculate path
     */

    private fun calculatePath() {
        var lX = 0f
        var lY = 0f
        pathStroke.moveTo(getChartWidth() * chartPoints[0].x, getChartHeight() - (getChartHeight() * chartPoints[0].y))
        for (index in 1 until chartPoints.size) {
            val currentPointX = getChartWidth() * chartPoints[index].x
            val currentPointY = getChartHeight() - (getChartHeight() * chartPoints[index].y)
            val previousPointX = getChartWidth() * chartPoints[index - 1].x
            val previousPointY = getChartHeight() - (getChartHeight() * chartPoints[index - 1].y)

            // Distance between currentPoint and previousPoint
            val firstDistance =
                sqrt((currentPointX - previousPointX).toDouble().pow(2.0) + (currentPointY - previousPointY).toDouble().pow(2.0)).toFloat()

            // Minimum is used to avoid going too much right
            val firstX = min(previousPointX + lX * firstDistance, (previousPointX + currentPointX) / 2)
            val firstY = previousPointY + lY * firstDistance

            val nextPointX = getChartWidth() * chartPoints[if (index + 1 < chartPoints.size) index + 1 else index].x
            val nextPointY = getChartHeight() - (getChartHeight() * chartPoints[if (index + 1 < chartPoints.size) index + 1 else index].y)

            // Distance between nextPoint and previousPoint (length of reference line)
            val secondDistance = sqrt((nextPointX - previousPointX).toDouble().pow(2.0) + (nextPointY - previousPointY).toDouble().pow(2.0)).toFloat()
            // (lX,lY) is the slope of the reference line
            lX = (nextPointX - previousPointX) / secondDistance * 0.3f
            lY = (nextPointY - previousPointY) / secondDistance * 0.3f

            // Maximum is used to avoid going too much left
            val secondX = max(currentPointX - lX * firstDistance, (previousPointX + currentPointX) / 2)
            val secondY = currentPointY - lY * firstDistance

            pathStroke.cubicTo(firstX, firstY, secondX, secondY, currentPointX, currentPointY)
        }

        pathFill.addPath(pathStroke)
        pathFill.lineTo(getChartWidth() * chartPoints[chartPoints.size - 1].x, getChartHeight())
        pathFill.lineTo(getChartWidth() * chartPoints[0].x, getChartHeight())
    }

    private fun convertToBitmap(drawable: Drawable, widthPixels: Int, heightPixels: Int): Bitmap? {
        val mutableBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mutableBitmap)
        drawable.setBounds(0, 0, widthPixels, heightPixels)
        drawable.draw(canvas)
        return mutableBitmap
    }

    fun reset() {

        chartPoints.clear()
        pathStroke.rewind()
        pathFill.rewind()
        startTime = -1
        invalidate()
    }

    /**
     * This function is used for convert download and upload speed into graph Y value
     */
    private fun toLog(value: Long): Float {
        return (if (value < 1e5) 0.0 else (2.0 + log10(value / 1e7)) / 4.0).toFloat()
    }

    override fun onDetachedFromWindow() {
        reset()
        super.onDetachedFromWindow()
    }

    companion object {

        private const val STROKE_WIDTH: Float = 3.0f
    }
}