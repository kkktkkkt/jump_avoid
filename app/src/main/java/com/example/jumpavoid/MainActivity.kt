package com.example.jumpavoid

import android.app.AlertDialog
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var detector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameView = GameView(this)
        setContentView(gameView)

        detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Treat as horizontal fling only when X velocity dominates
                if (abs(velocityX) > abs(velocityY) * 1.2f) {
                    if (velocityX < 0) gameView.moveLeft()
                    else               gameView.moveRight()
                }
                return true
            }
        })

        gameView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            true
        }

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
}
