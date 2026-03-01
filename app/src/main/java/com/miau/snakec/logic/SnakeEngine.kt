package com.miau.snakec.logic

import kotlin.random.Random

data class Cell(val x: Int, val y: Int)

enum class Direction { UP, DOWN, LEFT, RIGHT }

data class GameState(
    val cols: Int,
    val rows: Int,
    val snake: ArrayDeque<Cell>,
    var dir: Direction,
    var food: Cell,
    var alive: Boolean = true,
    var score: Int = 0,
    var waitingToStart: Boolean = true
)

class SnakeEngine(
    private val cols: Int,
    private val rows: Int
) {
    val state: GameState

    init {
        val start = Cell(cols / 2, rows / 2)
        val snake = ArrayDeque<Cell>().apply {
            addFirst(start)
            addLast(Cell(start.x - 1, start.y))
            addLast(Cell(start.x - 2, start.y))
        }
        state = GameState(
            cols = cols,
            rows = rows,
            snake = snake,
            dir = Direction.RIGHT,
            food = spawnFood(snake),
            waitingToStart = true
        )
    }

    fun start(dir: Direction) {
        state.dir = dir
        state.waitingToStart = false
    }

    fun setDirection(next: Direction) {
        if (state.waitingToStart) {
            start(next)
            return
        }
        // prevent instant reverse
        val cur = state.dir
        val isReverse =
            (cur == Direction.UP && next == Direction.DOWN) ||
            (cur == Direction.DOWN && next == Direction.UP) ||
            (cur == Direction.LEFT && next == Direction.RIGHT) ||
            (cur == Direction.RIGHT && next == Direction.LEFT)

        if (!isReverse) state.dir = next
    }

    fun step() {
        if (!state.alive || state.waitingToStart) return

        val head = state.snake.first()
        val newHead = when (state.dir) {
            Direction.UP -> Cell(head.x, head.y - 1)
            Direction.DOWN -> Cell(head.x, head.y + 1)
            Direction.LEFT -> Cell(head.x - 1, head.y)
            Direction.RIGHT -> Cell(head.x + 1, head.y)
        }

        // wall collision
        if (newHead.x !in 0 until cols || newHead.y !in 0 until rows) {
            state.alive = false
            return
        }

        // self collision
        if (state.snake.contains(newHead)) {
            state.alive = false
            return
        }

        state.snake.addFirst(newHead)

        // eat
        if (newHead == state.food) {
            state.score += 1
            state.food = spawnFood(state.snake)
        } else {
            state.snake.removeLast()
        }
    }

    fun reset() {
        val start = Cell(cols / 2, rows / 2)
        state.snake.clear()
        state.snake.addFirst(start)
        state.snake.addLast(Cell(start.x - 1, start.y))
        state.snake.addLast(Cell(start.x - 2, start.y))
        state.dir = Direction.RIGHT
        state.alive = true
        state.score = 0
        state.waitingToStart = true
        state.food = spawnFood(state.snake)
    }

    private fun spawnFood(snake: ArrayDeque<Cell>): Cell {
        val occupied = snake.toHashSet()
        while (true) {
            val c = Cell(Random.nextInt(cols), Random.nextInt(rows))
            if (!occupied.contains(c)) return c
        }
    }
}