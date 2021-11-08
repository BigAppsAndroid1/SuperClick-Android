package il.co.superclick.utilities

import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.foregroundApplication
import kotlin.math.abs


open class OnSwipeTouchListener : OnTouchListener {

    companion object {
        private const val SWIPE_THRESHOLD = 10
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    private val gestureDetector =
        GestureDetector(foregroundActivity ?: foregroundApplication, GestureListener())

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            var result = false
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        result = if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                } else {
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        result = if (diffY > 0) {
                            onSwipeBottom()
                        } else {
                            onSwipeTop()
                        }
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return result
        }
    }

    open fun onSwipeRight(): Boolean {
        return false
    }

    open fun onSwipeLeft(): Boolean {
        return false
    }

    open fun onSwipeTop(): Boolean {
        return false
    }

    open fun onSwipeBottom(): Boolean {
        return false
    }
}