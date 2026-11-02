package com.eta.saveup.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
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
 * 关键设计：拖动期间只写普通 Float 状态（同步、零协程），保证跟手；
 * 松手后才交给 [Animatable] + [exponentialDecay] 做原生惯性，最后 spring 吸附整行。
 * 惯性物理和 LazyColumn 的 fling 同源，所以手感一致。
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

    // 唯一的位置来源：行（浮点）
    var position by remember { mutableFloatStateOf(selectedIndex.coerceIn(0, maxIndex).toFloat()) }

    val scope = rememberCoroutineScope()
    val hap = rememberUpdatedState(haptics)
    val onSel = rememberUpdatedState(onSelected)
    val animJob = remember { arrayOfNulls<Job>(1) }
    var lastTickIdx = selectedIndex

    // 外部值变化 → 归位
    LaunchedEffect(selectedIndex) {
        val target = selectedIndex.coerceIn(0, maxIndex).toFloat()
        if (abs(position - target) > 0.01f) {
            animJob[0]?.cancel()
            animJob[0] = scope.launch {
                animatePosition(position, target) { position = it }
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(itemHeight * visibleCount)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(maxIndex, itemPx) {
                val tracker = VelocityTracker()

                detectDragGestures(
                    onDragStart = {
                        animJob[0]?.cancel()
                        tracker.resetTracking()
                    },
                    onDragEnd = {
                        val vY = tracker.calculateVelocity().y // px/s，向下为正
                        val vLines = -vY / itemPx              // 行/秒，上滑为正

                        animJob[0] = scope.launch {
                            if (abs(vLines) > 40f) {
                                position = decayTo(position, vLines, maxIndex)
                            }
                            val target = position.roundToInt().coerceIn(0, maxIndex).toFloat()
                            animatePosition(position, target) { position = it }
                            onSel.value(target.roundToInt())
                        }
                    },
                    onDragCancel = {
                        animJob[0] = scope.launch {
                            val target = position.roundToInt().coerceIn(0, maxIndex).toFloat()
                            animatePosition(position, target) { position = it }
                            onSel.value(target.roundToInt())
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    tracker.addPointerInputChange(change)
                    // 手指上滑(dy<0) → 索引增大；同步写入状态，立刻重组
                    position = (position - dragAmount.y / itemPx)
                        .coerceIn(0f, maxIndex.toFloat())
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
            position = position,
            itemHeightPx = itemPx,
            visibleCount = visibleCount,
            textSize = textSize,
            activeColor = activeColor,
            inactiveColor = inactiveColor
        )
    }

    // 每跨过一整行给一次齿感
    LaunchedEffect(position) {
        val idx = position.roundToInt()
        if (idx != lastTickIdx) {
            lastTickIdx = idx
            hap.value?.wheel()
        }
    }
}

/** 用指数衰减跑惯性，返回最终位置 */
private suspend fun decayTo(
    from: Float,
    velocityLinesPerSec: Float,
    maxIndex: Int
): Float {
    val anim = Animatable(from)
    anim.animateDecay(
        initialVelocity = velocityLinesPerSec,
        animationSpec = exponentialDecay(frictionMultiplier = 1.6f)
    ) {
        if (value < 0f || value > maxIndex) {
            // 撞到边界直接停
            this.cancelAnimation()
        }
    }
    return anim.value.coerceIn(0f, maxIndex.toFloat())
}

private suspend fun animatePosition(from: Float, target: Float, set: (Float) -> Unit) {
    val anim = Animatable(from)
    anim.animateTo(
        target,
        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    ) {
        set(value)
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
