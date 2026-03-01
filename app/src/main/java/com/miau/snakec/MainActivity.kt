package com.miau.snakec

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.miau.snakec.events.TouchEvent
import com.miau.snakec.events.TouchProcessor
import com.miau.snakec.logic.Direction
import com.miau.snakec.ui.GameView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var gameView: GameView
    private lateinit var scoreText: TextView

    private lateinit var mainMenu: View
    private lateinit var gameOverMenu: View

    private var loopJob: Job? = null

    // swipe tracking
    private var downX = 0f
    private var downY = 0f
    private var activeId: Int? = null
    private val swipeThreshold = 40f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = FrameLayout(this)
        
        // Ensure content is within status/nav bars
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        gameView = GameView(this)

        scoreText = TextView(this).apply {
            textSize = 24f
            // Darker Turquoise
            setTextColor(0xFF008080.toInt()) 
            // Shadow effect for text
            setShadowLayer(4f, 2f, 2f, 0x44000000.toInt())
            text = "Score: 0"
            // Padding inside the "window"
            setPadding(32, 16, 32, 16)
            // Apply the background drawable
            setBackgroundResource(R.drawable.score_background)
        }

        mainMenu = buildMainMenu(
            onStart = {
                hideMainMenu()
                startGameFresh()
            }
        )

        gameOverMenu = buildGameOverMenu(
            onRestart = {
                hideGameOver()
                restartGame()
            },
            onMainMenu = {
                hideGameOver()
                showMainMenu()
            }
        )

        // base layer: game view
        root.addView(gameView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // score overlay with margin
        val scoreParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { 
            gravity = Gravity.TOP or Gravity.END
            topMargin = 32
            rightMargin = 32
        }
        root.addView(scoreText, scoreParams)

        // overlays
        val overlayParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        root.addView(mainMenu, overlayParams)
        root.addView(gameOverMenu, overlayParams)

        setContentView(root)

        showMainMenu()
        hideGameOver()

        // listen for game over
        gameView.onGameOver = {
            stopLoop()
            vibrateOnDeath()
            showGameOver()
        }

        // swipe input
        gameView.setOnTouchListener(
            TouchProcessor { ev ->
                when (ev) {
                    is TouchEvent.Down -> {
                        activeId = ev.id
                        downX = ev.x
                        downY = ev.y
                    }
                    is TouchEvent.Move -> {
                        if (ev.id != activeId) return@TouchProcessor
                        // ignore swipes if we’re not playing (menu visible)
                        if (mainMenu.visibility == View.VISIBLE || gameOverMenu.visibility == View.VISIBLE) {
                            return@TouchProcessor
                        }

                        val dx = ev.x - downX
                        val dy = ev.y - downY
                        if (abs(dx) < swipeThreshold && abs(dy) < swipeThreshold) return@TouchProcessor

                        val dir = if (abs(dx) > abs(dy)) {
                            if (dx > 0) Direction.RIGHT else Direction.LEFT
                        } else {
                            if (dy > 0) Direction.DOWN else Direction.UP
                        }

                        gameView.engine.setDirection(dir)
                        downX = ev.x
                        downY = ev.y
                    }
                    is TouchEvent.Up -> {
                        if (ev.id == activeId) activeId = null
                        gameView.performClick()
                    }
                }
            }
        )
    }

    private fun vibrateOnDeath() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    override fun onPause() {
        super.onPause()
        stopLoop()
    }

    private fun startGameFresh() {
        gameView.resetGame()
        updateScore()
        startLoop()
    }

    private fun restartGame() {
        gameView.resetGame()
        updateScore()
        startLoop()
    }

    private fun startLoop() {
        if (loopJob != null) return
        loopJob = lifecycleScope.launch {
            while (isActive) {
                gameView.engine.step()
                updateScore()
                gameView.invalidate()
                delay(180L)
            }
        }
    }

    private fun updateScore() {
        scoreText.text = "Score: ${gameView.engine.state.score}"
    }

    private fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    // ---------- UI helpers ----------

    private fun showMainMenu() {
        stopLoop()
        mainMenu.visibility = View.VISIBLE
        gameView.resetGame()
        updateScore()
    }

    private fun hideMainMenu() {
        mainMenu.visibility = View.GONE
    }

    private fun showGameOver() {
        gameOverMenu.visibility = View.VISIBLE
    }

    private fun hideGameOver() {
        gameOverMenu.visibility = View.GONE
    }

    private fun buildMainMenu(onStart: () -> Unit): View {
        val container = FrameLayout(this).apply {
            setBackgroundColor(0xCC000000.toInt())
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            setBackgroundColor(0xFF1B222A.toInt())
        }

        val title = TextView(this).apply {
            text = "snakt"
            textSize = 40f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val start = Button(this).apply {
            text = "Start"
            setOnClickListener { onStart() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        card.addView(title)
        card.addView(space(24))
        card.addView(start)

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER }

        container.addView(card, lp)
        return container
    }

    private fun buildGameOverMenu(onRestart: () -> Unit, onMainMenu: () -> Unit): View {
        val container = FrameLayout(this).apply {
            setBackgroundColor(0xCC000000.toInt())
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            setBackgroundColor(0xFF1B222A.toInt())
        }

        val title = TextView(this).apply {
            text = "Game Over"
            textSize = 34f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val restart = Button(this).apply {
            text = "Restart"
            setOnClickListener { onRestart() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val menu = Button(this).apply {
            text = "Main Menu"
            setOnClickListener { onMainMenu() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        card.addView(title)
        card.addView(space(24))
        card.addView(restart)
        card.addView(space(12))
        card.addView(menu)

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER }

        container.addView(card, lp)
        return container
    }

    private fun space(dp: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp)
        }
    }
}
