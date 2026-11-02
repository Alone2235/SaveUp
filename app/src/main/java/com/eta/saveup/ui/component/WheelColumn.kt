package com.eta.saveup.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 单列数字滚轮。
 *
 * 位置用 [MutableFloatState] 承载（单位：行），拖动/惯性/吸附共写同一状态，
 * 避免闭包捕获快照导致方向或数值不同步。
 */
@Composable
fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 52.dp,
    visibleCount: Int = 5,
    textSize: Int = 34,
    activeColor: Color = Color(0xFF1B1F27),
    inactiveColor: Color = Color(0xFFB6BCC8),
    haptics: HapticsWrapper? = null
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val itemPx = with(density) { itemHeight.toPx() }
    val maxIndex = items.lastIndex

    val posState: MutableFloatState = remember {
        mutableFloatStateOf(selectedIndex.coerceIn(0, maxIndex).toFloat())
    }
    val scope = rememberCoroutineScope()
    val hap = rememberUpdatedState(haptics)
    val onSel = rememberUpdatedState(onSelected)
    val animJob = remember { arrayOfNulls<Job>(1) }

    LaunchedEffect(selectedIndex) {
        val target = selectedIndex.coerceIn(0, maxIndex).toFloat()
        if (abs(posState.floatValue - target) > 0.01f) {
            animJob[0]?.cancel()
            animJob[0] = scope.launch {
                animatePosition(posState, target)
                onSel.value(posState.floatValue.roundToInt())
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(itemHeight * visibleCount)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(maxIndex, itemPx) {
                var lastSampleTime = 0L
                var lastSampleValue = 0f

                detectDragGestures(
                    onDragStart = {
                        animJob[0]?.cancel()
                        lastSampleTime = 0L
                        lastSampleValue = posState.floatValue
                    },
                    onDragEnd = {
                        val now = System.nanoTime()
                        val dtMs = if (lastSampleTime > 0L) (now - lastSampleTime) / 1_000_000f else 0f
                        // 手指位移（像素）：正值=向下滑，负值=向上滑
                        val fingerDvPx = posState.floatValue - lastSampleValue
                        // 行数变化（正值=向上滚，索引增大）
                        val linesDv = fingerDvPx
                        // 每毫秒行数
                        val linesPerMs = if (dtMs > 16f) linesDv / dtMs else 0f
                        // 每帧(16.6ms)行数
                        val v0 = linesPerMs * 16.6f

                        animJob[0] = scope.launch {
                            if (abs(v0) > 0.02f) {
                                fling(posState, v0, maxIndex) { hap.value?.wheel() }
                            }
                            settle(posState, maxIndex)
                            onSel.value(posState.floatValue.roundToInt())
                        }
                    },
                    onDragCancel = {
                        animJob[0] = scope.launch {
                            settle(posState, maxIndex)
                            onSel.value(posState.floatValue.roundToInt())
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()

                    val now = System.nanoTime()
                    val dt = (now - lastSampleTime) / 1_000_000f
                    if (lastSampleTime == 0L || dt > 80f) {
                        lastSampleTime = now
                        lastSampleValue = posState.floatValue
                    }

                    // 手指上滑(dy<0) → 内容上移 → 索引变大
                    val deltaLines = -dragAmount.y / itemPx
                    val next = (posState.floatValue + deltaLines).coerceIn(0f, maxIndex.toFloat())
                    posState.floatValue = next
                }
            }
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(activeColor.copy(alpha = 0.05f))
                .border(1.dp, activeColor.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
        )

        WheelContent(
            items = items,
            position = posState.floatValue,
            itemHeightPx = itemPx,
            visibleCount = visibleCount,
            textSize = textSize,
            activeColor = activeColor,
            inactiveColor = inactiveColor
        )
    }
}

/** 惯性：v0 为每帧行数（正=索引增大） */
private suspend fun fling(
    pos: MutableFloatState,
    v0: Float,
    maxIndex: Int,
    onTick: () -> Unit
) {
    var v = v0
    var lastIdx = pos.floatValue.roundToInt()
    while (abs(v) > 0.01f) {
        withFrameNanos { }
        val current = pos.floatValue
        val next = (current + v).coerceIn(0f, maxIndex.toFloat())
        if ((next <= 0f && v < 0f) || (next >= maxIndex.toFloat() && v > 0f)) v = 0f
        pos.floatValue = next
        val idx = pos.floatValue.roundToInt()
        if (idx != lastIdx) {
            lastIdx = idx
            onTick()
        }
        v *= 0.93f
    }
}

private suspend fun settle(pos: MutableFloatState, maxIndex: Int) {
    val target = pos.floatValue.roundToInt().coerceIn(0, maxIndex).toFloat()
    if (abs(pos.floatValue - target) < 0.01f) return
    animatePosition(pos, target)
}

private suspend fun animatePosition(pos: MutableFloatState, target: Float) {
    val anim = Animatable(pos.floatValue)
    anim.animateTo(target, spring(stiffness = 620f, dampingRatio = 0.85f)) {
        pos.floatValue = value
    }
}

@Composable
private fun WheelContent(
    items: List<String>,
    position: Float,
    itemHeightPx: Float,
    visibleCount: Int,
    textSize: Int,
    activeColor: Color,
    inactiveColor: Color
) {
    val half = visibleCount / 2 + 1
    val center = position.roundToInt()
    val from = (center - half).coerceAtLeast(0)
    val to = (center + half).coerceAtMost(items.lastIndex)

    Layout(
        content = {
            for (i in from..to) {
                val distance = abs(i - position)
                val centered = distance < 0.5f
                Text(
                    text = items[i],
                    fontSize = textSize.sp,
                    fontWeight = if (centered) FontWeight.Bold else FontWeight.Normal,
                    color = if (centered) activeColor else inactiveColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer {
                        val scale = if (centered) 1f else (1f - distance * 0.10f).coerceIn(0.6f, 1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = if (centered) 1f else (1f - distance * 0.32f).coerceIn(0.10f, 0.8f)
                    }
                )
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = (itemHeightPx * visibleCount).roundToInt()
        val placeables = measurables.map {
            it.measure(constraints.copy(minWidth = width, maxWidth = width))
        }
        val centerY = height / 2f

        layout(width, height) {
            placeables.forEachIndexed { n, p ->
                val logicalIndex = from + n
                val elementCenter = centerY + (logicalIndex - position) * itemHeightPx
                p.place(x = 0, y = (elementCenter - p.height / 2f).roundToInt())
            }
        }
    }
}
