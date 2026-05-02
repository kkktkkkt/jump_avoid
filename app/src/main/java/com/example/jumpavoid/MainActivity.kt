package com.example.jumpavoid

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var gameView: GameView

    private var lastLaneChangeX = 0f
    private val LANE_DP = 60f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameView = GameView(this)
        setContentView(gameView)

        gameView.onGameOver = {
            if (!isFinishing) {
                AlertDialog.Builder(this)
                    .setTitle("GAME OVER")
                    .setMessage("スコア: ${gameView.score}")
                    .setPositiveButton("もう一度") { _, _ ->
                        gameView.reset()
                        showLevelSelector()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        showLevelSelector()
    }

    private fun showLevelSelector() {
        val savedLevel = loadLevel()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(80, 48, 80, 32)
        }

        val label = TextView(this).apply {
            text = "スタートレベル: $savedLevel"
            textSize = 20f
            gravity = Gravity.CENTER
        }

        val seekBar = SeekBar(this).apply {
            max = 9
            progress = savedLevel - 1
            setPadding(0, 24, 0, 16)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                label.text = "スタートレベル: ${progress + 1}"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // センタリングしたスタートボタン（MATCH_PARENT で確実に表示）
        val startBtn = Button(this).apply {
            text = "スタート"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(label)
        layout.addView(seekBar)
        layout.addView(startBtn)

        val dialog = AlertDialog.Builder(this)
            .setTitle("レベル選択")
            .setView(layout)
            .setCancelable(false)
            .create()

        startBtn.setOnClickListener {
            val level = seekBar.progress + 1
            saveLevel(level)
            gameView.startAtLevel(level)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveLevel(level: Int) =
        getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .edit().putInt("level", level).apply()

    private fun loadLevel(): Int =
        getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .getInt("level", 1)

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
