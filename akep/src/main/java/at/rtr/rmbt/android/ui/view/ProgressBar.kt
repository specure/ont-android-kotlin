/*
 *
 *  Licensed under the Apache License, Version 2.0 (the “License”);
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an “AS IS” BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R

class ProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {

    /**
     * Defines count of squares in graph
     */
    private var verticalCount = 2
    private var horizontalCount = DEFAULT_HORIZONTAL_SQUARE_COUNT

    private var enabledProgress: Boolean = true

    var squareSize: Float = 0f
        set(value) {
            field = value * 2f * resources.displayMetrics.density
        }

    var progress: Int = 0
        set(value) {
            val oldProgress = field
            field = value
            if (oldProgress != value) {
                updateProgress(progress)
                invalidate()
            }
        }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressBar)
        horizontalCount = typedArray.getInt(R.styleable.ProgressBar_horizontal_squares_count, DEFAULT_HORIZONTAL_SQUARE_COUNT)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setWillNotDraw(false)
        squareSize = 270f / 160
        typedArray.recycle()
    }

    private var disabledSquarePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_scale)
        style = Paint.Style.FILL_AND_STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        isAntiAlias = true
    }

    private var emptySquarePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_not_progressed)
        style = Paint.Style.FILL_AND_STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        isAntiAlias = true
    }

    private var progressPaint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    private var currentCanvas: Canvas? = null
    private var bitmap: Bitmap? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = SQUARE_MULTIPLIER * squareSize * (horizontalCount - 1) + squareSize
        val height = SQUARE_MULTIPLIER * squareSize * (verticalCount - 1) + squareSize
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width.toInt(), MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(height.toInt(), MeasureSpec.AT_MOST)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            createBitmap()
            updateProgress(progress)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    private fun createBitmap() {
        if (measuredHeight > 0 && measuredWidth > 0) {
            with(Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)) {
                bitmap = this
                val canvas = Canvas(this)
                currentCanvas = canvas
                if (enabledProgress) {
                    drawBackground(canvas)
                } else {
                    drawDisabledBackground(canvas)
                }
            }
        }
    }

    /**
     * Draw gray squares for the background
     */
    private fun drawBackground(canvas: Canvas) {

        var y = 0.0f
        for (i in 0 until verticalCount) {
            var x = 0.0f
            for (j in 0 until horizontalCount) {
                canvas.drawRect(x, y, x + squareSize, y + squareSize, emptySquarePaint)
                x += squareSize * SQUARE_MULTIPLIER
            }
            y += squareSize * SQUARE_MULTIPLIER
        }
    }

    /**
     * Draw gray squares for the background
     */
    private fun drawDisabledBackground(canvas: Canvas) {

        var y = 0.0f
        for (i in 0 until verticalCount) {
            var x = 0.0f
            for (j in 0 until horizontalCount) {
                canvas.drawRect(x, y, x + squareSize, y + squareSize, disabledSquarePaint)
                x += squareSize * SQUARE_MULTIPLIER
            }
            y += squareSize * SQUARE_MULTIPLIER
        }
    }

    /**
     * Calculate the size of filled graph of part and draw it
     */
    private fun updateProgress(percentage: Int) {
        currentCanvas?.apply {
            if (enabledProgress) {
                drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                drawBackground(this)
                drawRect(0f, 0f, (measuredWidth * percentage / 100).toFloat(), measuredHeight.toFloat(), progressPaint)
            } else {
                drawDisabledBackground(this)
            }
        }
    }

    fun setProgressEnabled(enabled: Boolean) {
        this.enabledProgress = enabled
        updateProgress(progress)
        invalidate()
    }

    companion object {
        private const val SQUARE_MULTIPLIER = 1.75f
        private const val DEFAULT_HORIZONTAL_SQUARE_COUNT = 16
    }
}