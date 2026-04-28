package com.example.jumpavoid

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.max

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val stickFigure = StickFigure()
    private val obstacles = mutableListOf<Obstacle>()

    var score = 0
        private set

    @Volatile private var isGameOver = false
    @Volatile private var resetRequested = false
    private var speed = BASE_SPEED
    private var frameCount = 0

    private var screenW = 0f
    private var screenH = 0f
    private var vpX = 0f      // vanishing point X (screen center)
    private var vpY = 0f      // vanishing point Y (upper area)
    private var roadHalfW = 0f

    var onGameOver: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var gameThread: GameThread? = null

    // --- Paints ---
    private val bgPaint = Paint().apply { color = Color.rgb(18, 22, 38) }
    private val roadPaint = Paint().apply {
        color = Color.rgb(38, 50, 72)
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint().apply {
        color = Color.rgb(60, 82, 115)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val lanePaint = Paint().apply {
        color = Color.rgb(85, 112, 155)
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val obsPaint = Paint().apply {
        color = Color.rgb(210, 55, 55)
        style = Paint.Style.FILL
    }
    private val obsRimPaint = Paint().apply {
        color = Color.rgb(255, 125, 100)
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 54f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    init { holder.addCallback(this) }

    // --- Surface lifecycle ---

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameThread = GameThread().also { it.running = true; it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        screenW = w.toFloat()
        screenH = h.toFloat()
        vpX = screenW / 2f
        vpY = screenH * 0.24f
        roadHalfW = screenW * 0.42f
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameThread?.let { it.running = false; it.join() }
        gameThread = null
    }

    // --- Public controls (called from UI thread) ---

    fun moveLeft()  { if (!isGameOver) stickFigure.moveLeft() }
    fun moveRight() { if (!isGameOver) stickFigure.moveRight() }

    fun reset() {
        isGameOver = false
        resetRequested = true
    }

    // --- Game logic (called from game thread) ---

    private fun handleReset() {
        obstacles.clear()
        score = 0
        speed = BASE_SPEED
        frameCount = 0
        stickFigure.reset()
        resetRequested = false
    }

    private fun update() {
        if (screenW == 0f) return
        if (resetRequested) { handleReset(); return }
        if (isGameOver) return

        frameCount++
        score = frameCount / 10
        speed = BASE_SPEED + frameCount * 0.000004f

        val spawnEvery = max(35, 90 - frameCount / 80)
        if (frameCount % spawnEvery == 0) {
            obstacles.add(Obstacle((0..2).random()))
        }

        val iter = obstacles.iterator()
        while (iter.hasNext()) {
            val o = iter.next()
            o.update(speed)
            if (o.isPassed) iter.remove()
        }

        stickFigure.update()

        for (o in obstacles) {
            if (o.lane == stickFigure.lane &&
                o.depth >= FIGURE_DEPTH - 0.05f &&
                o.depth <= FIGURE_DEPTH + 0.08f
            ) {
                isGameOver = true
                mainHandler.post { onGameOver?.invoke() }
                return
            }
        }
    }

    private fun render(canvas: Canvas) {
        // Background
        canvas.drawRect(0f, 0f, screenW, screenH, bgPaint)
        if (screenW == 0f) return

        // Road fill (trapezoid converging at vanishing point)
        val path = Path()
        path.moveTo(vpX, vpY)
        path.lineTo(vpX - roadHalfW, screenH)
        path.lineTo(vpX + roadHalfW, screenH)
        path.close()
        canvas.drawPath(path, roadPaint)

        // Scrolling horizontal grid lines (creates movement illusion)
        val gridStep = 1f / 10f
        val scrollFrac = (frameCount * speed * 3.5f) % gridStep
        var d = gridStep + scrollFrac
        while (d <= 1f) {
            val y  = vpY + d * (screenH - vpY)
            val hw = roadHalfW * d
            canvas.drawLine(vpX - hw, y, vpX + hw, y, gridPaint)
            d += gridStep
        }

        // Lane dividers (4 lines radiating from vanishing point)
        for (xFactor in listOf(-1f, -1f / 3f, 1f / 3f, 1f)) {
            canvas.drawLine(vpX, vpY, vpX + xFactor * roadHalfW, screenH, lanePaint)
        }

        // Obstacles (drawn back-to-front so nearer ones appear on top)
        for (o in obstacles.sortedBy { it.depth }) {
            val ox = o.screenX(vpX, roadHalfW)
            val oy = o.screenY(vpY, screenH)
            val hw = o.halfW(roadHalfW)
            val oh = hw * 3f
            canvas.drawRect(ox - hw, oy - oh, ox + hw, oy, obsPaint)
            canvas.drawRect(ox - hw, oy - oh, ox + hw, oy, obsRimPaint)
        }

        // Stick figure
        stickFigure.draw(canvas, vpX, vpY, screenH, roadHalfW)

        // HUD
        canvas.drawText("SCORE  $score", 36f, 72f, scorePaint)
    }

    // --- Game loop thread ---

    private inner class GameThread : Thread() {
        var running = false

        override fun run() {
            while (running) {
                val c = holder.lockCanvas() ?: continue
                try {
                    update()
                    render(c)
                } finally {
                    holder.unlockCanvasAndPost(c)
                }
                sleep(16)
            }
        }
    }

    companion object {
        private const val BASE_SPEED = 0.008f
        const val FIGURE_DEPTH = 0.78f
    }
}
