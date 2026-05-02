package com.example.jumpavoid

import android.graphics.*
import kotlin.math.sin

class StickFigure {

    var lane = 1  // 0=left  1=center  2=right
    private var tick = 0

    var onStep: (() -> Unit)? = null

    // 全パーツ クリーム色、輪郭のみ茶色
    private val creamFill = Paint().apply {
        color = Color.rgb(255, 245, 225)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val brownOutline = Paint().apply {
        color = Color.rgb(80, 55, 40)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    fun reset() { lane = 1; tick = 0 }
    fun moveLeft()  { if (lane > 0) lane-- }
    fun moveRight() { if (lane < 2) lane++ }

    fun update() {
        tick++
        if (tick % 8 == 0) onStep?.invoke()
    }

    fun draw(canvas: Canvas, vpX: Float, vpY: Float, screenH: Float, roadHalfW: Float) {
        val d = GameView.FIGURE_DEPTH
        val cx = vpX + (lane - 1) * roadHalfW * d * (2f / 3f)
        val groundY = vpY + d * (screenH - vpY)

        // --- プロポーション ---
        val totalH     = screenH * 0.14f
        val headR      = totalH * 0.27f         // 大きな後頭部
        val earR       = headR  * 0.24f
        val bodyHalfW  = headR  * 0.78f
        val bodyH      = totalH * 0.19f
        val legW       = totalH * 0.10f         // 脚の幅
        val legH       = totalH * 0.25f
        val legSpreadX = bodyHalfW * 0.48f
        val legCorner  = legW * 0.45f
        val armW       = totalH * 0.12f         // 腕の長さ
        val armH       = totalH * 0.09f         // 腕の厚み（正方形に近い短冊）
        val armCorner  = armH * 0.45f

        val feetY     = groundY
        val hipY      = feetY - legH
        val shoulderY = hipY - bodyH
        val bodyCY    = (shoulderY + hipY) / 2f  // ボディ中央Y
        val headCY    = shoulderY - headR * 0.50f
        val earOffX   = headR * 0.50f
        val earTopY   = headCY - headR * 0.75f

        // --- アニメーション ---
        val swing      = sin(tick * 0.22).toFloat()
        val leftLift   = swing.coerceAtLeast(0f)   * legH * 0.50f
        val rightLift  = (-swing).coerceAtLeast(0f) * legH * 0.50f
        val leftArmDy  = (-swing).coerceAtLeast(0f) * armH * 1.5f   // 逆位相・上下
        val rightArmDy = swing.coerceAtLeast(0f)    * armH * 1.5f

        // 1. 耳（頭の後ろ両側）
        canvas.drawCircle(cx - earOffX, earTopY, earR, creamFill)
        canvas.drawCircle(cx - earOffX, earTopY, earR, brownOutline)
        canvas.drawCircle(cx + earOffX, earTopY, earR, creamFill)
        canvas.drawCircle(cx + earOffX, earTopY, earR, brownOutline)

        // 2. ボディ（楕円）
        val bodyRect = RectF(cx - bodyHalfW, shoulderY, cx + bodyHalfW, hipY)
        canvas.drawOval(bodyRect, creamFill)
        canvas.drawOval(bodyRect, brownOutline)

        // 3. 腕（ボディ左右に密着した短冊・ボディ中央高さ）
        val leftArmRect = RectF(
            cx - bodyHalfW - armW, bodyCY - armH / 2 + leftArmDy,
            cx - bodyHalfW,        bodyCY + armH / 2 + leftArmDy
        )
        canvas.drawRoundRect(leftArmRect, armCorner, armCorner, creamFill)
        canvas.drawRoundRect(leftArmRect, armCorner, armCorner, brownOutline)

        val rightArmRect = RectF(
            cx + bodyHalfW,        bodyCY - armH / 2 + rightArmDy,
            cx + bodyHalfW + armW, bodyCY + armH / 2 + rightArmDy
        )
        canvas.drawRoundRect(rightArmRect, armCorner, armCorner, creamFill)
        canvas.drawRoundRect(rightArmRect, armCorner, armCorner, brownOutline)

        // 4. 脚（短い角丸矩形・交互膝上げ）
        val leftLegRect = RectF(
            cx - legSpreadX - legW / 2, hipY - leftLift,
            cx - legSpreadX + legW / 2, feetY - leftLift
        )
        canvas.drawRoundRect(leftLegRect, legCorner, legCorner, creamFill)
        canvas.drawRoundRect(leftLegRect, legCorner, legCorner, brownOutline)

        val rightLegRect = RectF(
            cx + legSpreadX - legW / 2, hipY - rightLift,
            cx + legSpreadX + legW / 2, feetY - rightLift
        )
        canvas.drawRoundRect(rightLegRect, legCorner, legCorner, creamFill)
        canvas.drawRoundRect(rightLegRect, legCorner, legCorner, brownOutline)

        // 5. 頭（後頭部・大きな丸・顔なし）
        canvas.drawCircle(cx, headCY, headR, creamFill)
        canvas.drawCircle(cx, headCY, headR, brownOutline)
    }
}
