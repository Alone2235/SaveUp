package com.eta.saveup.ui.component

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/** 触感反馈封装：轻点 / 成功 / 警告 */
object Haptics {
    fun tick(view: View?) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    fun success(view: View?) {
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun warn(view: View?) {
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /** 滚轮齿感：用 KEYBOARD_TAP，兼容性最好 */
    fun wheel(view: View?) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
}

@Composable
fun rememberHaptics(enabled: Boolean = true): HapticsWrapper {
    val view = LocalView.current
    return HapticsWrapper(view, enabled)
}

class HapticsWrapper(private val view: View?, private val enabled: Boolean = true) {
    fun tick() { if (enabled) Haptics.tick(view) }
    fun success() { if (enabled) Haptics.success(view) }
    fun warn() { if (enabled) Haptics.warn(view) }
    fun wheel() { if (enabled) Haptics.wheel(view) }
}
