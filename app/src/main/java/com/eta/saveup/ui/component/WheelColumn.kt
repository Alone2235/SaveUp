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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 单列数字滚轮。
 *
 * 自绘等距布局（不用 LazyColumn），中央位置恒为容器中点；
 * 滚动位置用 Animatable<Float> 维护（单位：行），松手后按累计位移吸附到整行。
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

    val position = remember { Animatable(selectedIndex.coerceIn(0, maxIndex).toFloat()) }
    val scope = rememberCoroutineScope()
    val hap = rememberUpdatedState(haptics)
    val onSel = rememberUpdatedState(onSelected)

    // 外部选中值变化 → 平滑归位
    LaunchedEffect(selectedIndex) {
        val target = selectedIndex.coerceIn(0, maxIndex).toFloat()
        if (abs(position.value - target) > 0.01f) {
            position.animateTo(target, spring(stiffness = 900f, dampingRatio = 1f))
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(itemHeight * visibleCount)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(maxIndex, itemPx) {
                var dragTotal = 0f
                var lastTick = position.value.roundToInt()

                detectDragGestures(
                    onDragStart = {
                        dragTotal = 0f
                        lastTick = position.value.roundToInt()
                        scope.launch { position.stop() }
                    },
                    onDragEnd = {
                        // 按行吸附
                        val projected = (position.value - dragTotal * 0.08f / itemPx)
                            .coerceIn(0f, maxIndex.toFloat())
                        val target = projected.roundToInt().coerceIn(0, maxIndex).toFloat()
                        scope.launch {
                            position.animateTo(target, spring(stiffness = 520f, dampingRatio = 0.86f))
                            if (target.roundToInt() != lastTick) hap.value?.wheel()
                            onSel.value(target.roundToInt())
                        }
                    },
                    onDragCancel = {
                        val target = position.value.roundToInt().coerceIn(0, maxIndex).toFloat()
                        scope.launch {
                            position.animateTo(target, spring(stiffness = 520f, dampingRatio = 0.86f))
                            onSel.value(target.roundToInt())
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragTotal += dragAmount.y
                    val next = (position.value - dragAmount.y / itemPx)
                        .coerceIn(0f, maxIndex.toFloat())
                    scope.launch { position.snapTo(next) }

                    val idx = next.roundToInt()
                    if (idx != lastTick) {
                        lastTick = idx
                        hap.value?.wheel()
                    }
                }
            }
    ) {
        // 中央选中带：淡底 + 细描边
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
            position = position.value,
            itemHeightPx = itemPx,
            visibleCount = visibleCount,
            textSize = textSize,
            activeColor = activeColor,
            inactiveColor = inactiveColor
        )
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
    val baseIdx = position.toInt()

    Layout(
        content = {
            for (i in (baseIdx - half)..(baseIdx + half)) {
                if (i in items.indices) {
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
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = (itemHeightPx * visibleCount).roundToInt()
        val placeables = measurables.map {
            it.measure(constraints.copy(minWidth = width, maxWidth = width))
        }

        layout(width, height) {
            placeables.forEachIndexed { n, p ->
                val logicalIndex = baseIdx - half + n
                val dy = (logicalIndex - position) * itemHeightPx +
                    (height / 2f) - (p.height / 2f)
                p.place(x = 0, y = dy.roundToInt())
            }
        }
    }
}
