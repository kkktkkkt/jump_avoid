package com.example.jumpavoid

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sin

class StickFigure {

    var lane = 1  // 0=left  1=center  2=right
    private var tick = 0

    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 7f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    fun reset() { lane = 1; tick = 0 }
    fun moveLeft() { if (lane > 0) lane-- }
    fun moveRight() { if (lane < 2) lane++ }
    fun update() { tick++ }

    fun draw(canvas: Canvas, vpX: Float, vpY: Float, screenH: Float, roadHalfW: Float) {
        val d = GameView.FIGURE_DEPTH
        val cx = vpX + (lane - 1) * roadHalfW * d * (2f / 3f)
        val groundY = vpY + d * (screenH - vpY)

        // Scale figure height relative to screen
        val totalH = screenH * 0.11f
        val headR  = totalH * 0.13f
        val bodyH  = totalH * 0.28f
        val legH   = totalH * 0.30f
        val armH   = totalH * 0.20f
        val armSpread = totalH * 0.17f

        val feetY     = groundY
        val hipY      = feetY - legH
        val shoulderY = hipY - bodyH
        val headCY    = shoulderY - headR

        // Running swing via sine wave
        val swing     = sin(tick * 0.22).toFloat()
        val legSwing  = swing * legH * 0.55f
        val armSwing  = -swing * armSpread  // opposite to legs

        // Head
        canvas.drawCircle(cx, headCY, headR, paint)
        // Body
        canvas.drawLine(cx, shoulderY, cx, hipY, paint)
        // Arms (opposite phase to legs)
        canvas.drawLine(cx, shoulderY + bodyH * 0.15f, cx + armSwing,  shoulderY + armH, paint)
        canvas.drawLine(cx, shoulderY + bodyH * 0.15f, cx - armSwing,  shoulderY + armH, paint)
        // Legs
        canvas.drawLine(cx, hipY, cx + legSwing, feetY, paint)
        canvas.drawLine(cx, hipY, cx - legSwing, feetY, paint)
    }
}
