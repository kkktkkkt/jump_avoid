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
    @Volatile private var isWaitingForStart = true

    private var speed = BASE_SPEED
    private var speedOffset = 0f
    private var frameCount = 0

    // Level-up display
    private var currentLevel = 1
    private var levelUpFramesLeft = 0
    private var levelUpText = ""
    private val LEVEL_UP_FRAMES = 120

    private var screenW = 0f
    private var screenH = 0f
    private var vpX = 0f
    private var vpY = 0f
    private var roadHalfW = 0f

    var onGameOver: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var gameThread: GameThread? = null

    // Sound
    private val stepSound = StepSound()

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
    private val levelUpPaint = Paint().apply {
        textSize = 86f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    init {
        holder.addCallback(this)
        stickFigure.onStep = { stepSound.play() }
    }

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
        stepSound.release()
    }

    // --- Public controls (called from UI thread) ---

    fun moveLeft()  { if (!isGameOver && !isWaitingForStart) stickFigure.moveLeft() }
    fun moveRight() { if (!isGameOver && !isWaitingForStart) stickFigure.moveRight() }

    fun reset() {
        isGameOver = false
        resetRequested = true
    }

    fun startAtLevel(level: Int) {
        frameCount = 0
        score = 0
        speedOffset = (level - 1) * 0.010f
        speed = BASE_SPEED + speedOffset
        currentLevel = level          // prevent immediate level-up fire
        levelUpFramesLeft = 0
        isWaitingForStart = false
    }

    // --- Game logic (called from game thread) ---

    private fun handleReset() {
        obstacles.clear()
        score = 0
        speed = BASE_SPEED
        speedOffset = 0f
        frameCount = 0
        currentLevel = 1
        levelUpFramesLeft = 0
        stickFigure.reset()
        resetRequested = false
        isWaitingForStart = true
    }

    private fun update() {
        if (screenW == 0f) return
        if (resetRequested) { handleReset(); return }
        if (isWaitingForStart || isGameOver) return

        frameCount++
        score = frameCount / 10
        speed = BASE_SPEED + speedOffset + frameCount * 0.000010f

        // Level-up detection
        val newLevel = score / 50 + 1
        if (newLevel > currentLevel) {
            currentLevel = newLevel
            levelUpFramesLeft = LEVEL_UP_FRAMES
            levelUpText = "LEVEL UP!  LV $currentLevel"
        }
        if (levelUpFramesLeft > 0) levelUpFramesLeft--

        val spawnEvery = max(18, 85 - frameCount / 40 - (currentLevel - 1) * 5)
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
        canvas.drawRect(0f, 0f, screenW, screenH, bgPaint)
        if (screenW == 0f) return

        // Road fill (trapezoid)
        val path = Path()
        path.moveTo(vpX, vpY)
        path.lineTo(vpX - roadHalfW, screenH)
        path.lineTo(vpX + roadHalfW, screenH)
        path.close()
        canvas.drawPath(path, roadPaint)

        // Scrolling horizontal grid lines
        val gridStep = 1f / 10f
        val scrollFrac = (frameCount * speed * 3.5f) % gridStep
        var d = gridStep + scrollFrac
        while (d <= 1f) {
            val y  = vpY + d * (screenH - vpY)
            val hw = roadHalfW * d
            canvas.drawLine(vpX - hw, y, vpX + hw, y, gridPaint)
            d += gridStep
        }

        // Lane dividers
        for (xFactor in listOf(-1f, -1f / 3f, 1f / 3f, 1f)) {
            canvas.drawLine(vpX, vpY, vpX + xFactor * roadHalfW, screenH, lanePaint)
        }

        // Obstacles (back-to-front)
        for (o in obstacles.sortedBy { it.depth }) {
            val ox = o.screenX(vpX, roadHalfW)
            val oy = o.screenY(vpY, screenH)
            val hw = o.halfW(roadHalfW)
            val oh = hw * 3f
            canvas.drawRect(ox - hw, oy - oh, ox + hw, oy, obsPaint)
            canvas.drawRect(ox - hw, oy - oh, ox + hw, oy, obsRimPaint)
        }

        // Character
        stickFigure.draw(canvas, vpX, vpY, screenH, roadHalfW)

        // HUD: score + level
        canvas.drawText("SCORE  $score   LV $currentLevel", 36f, 72f, scorePaint)

        // Level-up notification (fading)
        if (levelUpFramesLeft > 0) {
            val alpha = (levelUpFramesLeft * 255 / LEVEL_UP_FRAMES).coerceIn(0, 255)
            val textY = screenH * 0.42f
            // Shadow pass
            levelUpPaint.color = Color.argb(alpha / 2, 0, 0, 0)
            canvas.drawText(levelUpText, vpX + 3f, textY + 3f, levelUpPaint)
            // Gold text
            levelUpPaint.color = Color.argb(alpha, 255, 215, 0)
            canvas.drawText(levelUpText, vpX, textY, levelUpPaint)
        }
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
