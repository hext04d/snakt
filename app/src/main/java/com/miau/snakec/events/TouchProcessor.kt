package com.miau.snakec.events

import android.view.MotionEvent
import android.view.View

// Sealed class representing high-level touch events (down, move, up)
sealed class TouchEvent {
    data class Down(val x: Float, val y: Float, val id: Int) : TouchEvent()
    data class Move(val x: Float, val y: Float, val id: Int) : TouchEvent()
    data class Up(val x: Float, val y: Float, val id: Int) : TouchEvent()
}

fun interface TouchListener {
    fun onTouch(event: TouchEvent)
}

class TouchProcessor(private val listener: TouchListener) : View.OnTouchListener {
    override fun onTouch(v: View?, motion: MotionEvent): Boolean {
        when (motion.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = motion.actionIndex
                val id = motion.getPointerId(idx)
                listener.onTouch(TouchEvent.Down(motion.getX(idx), motion.getY(idx), id))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until motion.pointerCount) {
                    val id = motion.getPointerId(i)
                    listener.onTouch(TouchEvent.Move(motion.getX(i), motion.getY(i), id))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val idx = motion.actionIndex
                val id = motion.getPointerId(idx)
                listener.onTouch(TouchEvent.Up(motion.getX(idx), motion.getY(idx), id))
            }
        }
        return true
    }
}