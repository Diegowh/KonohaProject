package com.example.konohaproject

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin

class ArcProgressDrawable(
    private val context: Context,
    private val strokeWidth: Float = 15f,
    private val arcAngle: Float = 270f,
    private val startAngle: Float = 135f

) : Drawable() {

    // Configuración de paints
    private val backgroundPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.button_secondary)
        style = Paint.Style.STROKE
        this.strokeWidth = this@ArcProgressDrawable.strokeWidth
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.button_primary)
        style = Paint.Style.STROKE
        this.strokeWidth = this@ArcProgressDrawable.strokeWidth
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val capPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.button_primary)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private lateinit var arcRect: RectF
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    override fun draw(canvas: Canvas) {
        setupDimensions()
        drawBackgroundArc(canvas)
        drawProgressArc(canvas)
        drawProgressCap(canvas)
    }

    private fun setupDimensions() {
        val bounds = bounds
        val size = bounds.width().coerceAtMost(bounds.height())
        val padding = strokeWidth * 1.5f

        arcRect = RectF(padding, padding, size - padding, size - padding)
        centerX = bounds.exactCenterX()
        centerY = bounds.exactCenterY()
        radius = (size / 2) - padding
    }

    private fun drawBackgroundArc(canvas: Canvas) {
        canvas.drawArc(arcRect, startAngle, arcAngle, false, backgroundPaint)
    }

    private fun drawProgressArc(canvas: Canvas) {

        // Esta comentado porque no se si voy a querer utilizar el gradiente
        // en el futuro, asi que aqui se queda por ahora jeje

//        val gradient = SweepGradient(
//            centerX, centerY,
//            intArrayOf(
//                ContextCompat.getColor(context, R.color.button_secondary),
//                ContextCompat.getColor(context, R.color.button_primary)
//            ),
//            null
//        )

//        val matrix = Matrix().apply {
//            postRotate(startAngle, centerX, centerY)
//        }
//        gradient.setLocalMatrix(matrix)
//        progressPaint.shader = gradient


        val progress = (level / 10000f) * arcAngle
        canvas.drawArc(arcRect, startAngle, progress, false, progressPaint)
    }

    private fun drawProgressCap(canvas: Canvas) {
        val progress = (level / 10000f) * arcAngle
        val angle = startAngle + progress


        val angleRad = Math.toRadians(angle.toDouble())
        val x = centerX + radius * cos(angleRad).toFloat()
        val y = centerY + radius * sin(angleRad).toFloat()


        canvas.drawCircle(x, y, strokeWidth * 1.2f, capPaint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun onLevelChange(level: Int): Boolean {
        invalidateSelf()
        return true
    }
}