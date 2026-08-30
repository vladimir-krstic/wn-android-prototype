package dev.ipf.whitenoise

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry

/** Injects a real screen-coordinate tap so a separate popup/dialog window receives it. */
internal fun injectScreenTap(x: Float, y: Float) {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    val downTime = SystemClock.uptimeMillis()
    fun event(action: Int, eventTime: Long) = MotionEvent.obtain(
        downTime,
        eventTime,
        action,
        x,
        y,
        0,
    ).apply { source = InputDevice.SOURCE_TOUCHSCREEN }

    event(MotionEvent.ACTION_DOWN, downTime).also {
        automation.injectInputEvent(it, true)
        it.recycle()
    }
    event(MotionEvent.ACTION_UP, downTime + 50).also {
        automation.injectInputEvent(it, true)
        it.recycle()
    }
}
