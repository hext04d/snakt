package com.miau.snakec.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.miau.snakec.logic.SnakeEngine
import java.io.IOException

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Grid size
    private val cols = 10
    private val rows = 16

    val engine = SnakeEngine(cols, rows)

    // Called once when detected the snake has died
    var onGameOver: (() -> Unit)? = null
    private var gameOverNotified = false

    private val paint = Paint().apply {
        isFilterBitmap = false
    }

    // Sprites
    private var bgBitmap: Bitmap? = null

    init {
        loadSprites()
    }

    private fun loadSprites() {
        try {
            bgBitmap = loadBitmapFromAssets("sprites/Sprite-bg.png")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadBitmapFromAssets(fileName: String): Bitmap? {
        return try {
            context.assets.open(fileName).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            null
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun resetGame() {
        engine.reset()
        gameOverNotified = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cellW = width.toFloat() / cols
        val cellH = height.toFloat() / rows

        // Draw Background
        if (bgBitmap != null) {
            val spriteCellsW = 2
            val spriteCellsH = 2
            for (i in 0 until cols step spriteCellsW) {
                for (j in 0 until rows step spriteCellsH) {
                    val dest = Rect(
                        (i * cellW).toInt(),
                        (j * cellH).toInt(),
                        ((i + spriteCellsW) * cellW).toInt(),
                        ((j + spriteCellsH) * cellH).toInt()
                    )
                    canvas.drawBitmap(bgBitmap!!, null, dest, paint)
                }
            }
        } else {
            // Fallback checkered background
            val color1 = 0xFFAFEEEE.toInt()
            val color2 = 0xFFFFFFFF.toInt()
            paint.style = Paint.Style.FILL
            for (i in 0 until cols) {
                for (j in 0 until rows) {
                    paint.color = if ((i + j) % 2 == 0) color1 else color2
                    canvas.drawRect(i * cellW, j * cellH, (i + 1) * cellW, (j + 1) * cellH, paint)
                }
            }
        }

        // Draw Food
        val food = engine.state.food
        paint.style = Paint.Style.FILL
        paint.color = 0xFFA93226.toInt() // Darker pastel red
        canvas.drawRect(
            food.x * cellW + 2f,
            food.y * cellH + 2f,
            (food.x + 1) * cellW - 2f,
            (food.y + 1) * cellH - 2f,
            paint
        )

        // Draw Snake
        for (index in engine.state.snake.indices) {
            val c = engine.state.snake[index]
            paint.color = 0xFF667C4D.toInt() // Dark pastel green
            canvas.drawRect(
                c.x * cellW + 2f,
                c.y * cellH + 2f,
                (c.x + 1) * cellW - 2f,
                (c.y + 1) * cellH - 2f,
                paint
            )
        }

        // Game Over Overlay
        if (!engine.state.alive) {
            paint.color = 0xAA000000.toInt()
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            paint.color = 0xFFFFFFFF.toInt()
            paint.textSize = 64f
            val msg = "Game Over"
            val textW = paint.measureText(msg)
            canvas.drawText(msg, (width - textW) / 2f, height / 2f, paint)

            if (!gameOverNotified) {
                gameOverNotified = true
                onGameOver?.invoke()
            }
        }
    }
}
