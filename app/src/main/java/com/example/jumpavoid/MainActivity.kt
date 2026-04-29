package com.example.jumpavoid

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.MotionEvent

class MainActivity : Activity() {

    private lateinit var gameView: GameView

    private var lastLaneChangeX = 0f
    private val LANE_DP = 60f  // 1マス移動に必要なスライド距離(dp)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameView = GameView(this)
        setContentView(gameView)

        gameView.onGameOver = {
            if (!isFinishing) {
                AlertDialog.Builder(this)
                    .setTitle("GAME OVER")
                    .setMessage("スコア: ${gameView.score}")
                    .setPositiveButton("もう一度") { _, _ -> gameView.reset() }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val laneThreshold = LANE_DP * resources.displayMetrics.density
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastLaneChangeX = event.x
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastLaneChangeX
                when {
                    dx < -laneThreshold -> {
                        gameView.moveLeft()
                        lastLaneChangeX = event.x
                    }
                    dx > laneThreshold -> {
                        gameView.moveRight()
                        lastLaneChangeX = event.x
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
