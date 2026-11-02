package com.eta.saveup.ui.component

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * 触感反馈。
 *
 * 双重保障：
 * 1. 优先走 Vibrator 直接震，绕开系统"触摸震动"总开关，任何 ROM 都能感到
 * 2. 同时调 performHapticFeedback，保留系统级细腻手感
 */
object Haptics {

    private fun vibrator(ctx: Context?): Vibrator? {
        if (ctx == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun buzz(ctx: Context?, ms: Long, amplitude: Int) {
        val v = vibrator(ctx) ?: return
        if (!v.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, amplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        }
    }

    fun tick(view: View?, ctx: Context?) {
        buzz(ctx, 12, 120)
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun success(view: View?, ctx: Context?) {
        buzz(ctx, 24, 180)
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun warn(view: View?, ctx: Context?) {
        buzz(ctx, 40, 200)
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /** 滚轮齿感：短而轻，快速滑动时不糊成一片 */
    fun wheel(view: View?, ctx: Context?) {
        buzz(ctx, 8, 90)
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

@Composable
fun rememberHaptics(enabled: Boolean = true): HapticsWrapper {
    val view = LocalView.current
    val ctx = LocalContext.current
    return HapticsWrapper(view, ctx, enabled)
}

class HapticsWrapper(
    private val view: View?,
    private val ctx: Context?,
    private val enabled: Boolean = true
) {
    fun tick() { if (enabled) Haptics.tick(view, ctx) }
    fun success() { if (enabled) Haptics.success(view, ctx) }
    fun warn() { if (enabled) Haptics.warn(view, ctx) }
    fun wheel() { if (enabled) Haptics.wheel(view, ctx) }
}
