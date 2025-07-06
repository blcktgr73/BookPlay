package com.blacktiger.bookplay.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.GestureDetector.SimpleOnGestureListener

open class OnSwipeTouchListener(ctx: Context) : View.OnTouchListener {

    private val gestureDetector = GestureDetector(ctx, GestureListener())

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true // 꼭 true로!
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null || e2 == null) return false

            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            return if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY)) {
                if (kotlin.math.abs(diffX) > SWIPE_THRESHOLD &&
                    kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) onSwipeRight() else onSwipeLeft()
                    true
                } else false
            } else false
        }
    }

    open fun onSwipeRight() {}
    open fun onSwipeLeft() {}
}
