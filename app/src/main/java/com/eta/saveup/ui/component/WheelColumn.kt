package com.eta.saveup.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eta.saveup.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 单列数字滚轮（LazyColumn + snapFlingBehavior，支持循环）。
 *
 * 循环实现：
 * - items 在内部重复 [LOOPS] 圈，初始定位到正中间那圈
 * - 滑到靠近两端时静默 scrollToItem 到等价位置，视觉无缝
 * - 对外始终只报告真实下标（0..items.size-1）
 *
 * 性能：所有行字号/字重/颜色固定，居中效果只在 graphicsLayer 绘制阶段算，滚动零重组。
 */
private const val LOOPS = 7

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = Dimens.wheelItem,
    visibleCount: Int = 3,
    textSize: Int = 34,
    activeColor: Color = Color(0xFF1B1F27),
    inactiveColor: Color = Color(0xFFB6BCC8),
    haptics: HapticsWrapper? = null
) {
    if (items.isEmpty()) return

    val n = items.size
    val looping = n > 1
    val loops = if (looping) LOOPS else 1
    val total = n * loops
    val midLoop = loops / 2
    val midStart = midLoop * n

    val centerOffset = visibleCount / 2
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (looping) midStart + selectedIndex else selectedIndex
    )
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    var lastTick by remember { mutableIntStateOf(selectedIndex) }
    // 本列最近一次主动上报给外部的值；区分"自己滚的"与"外部要求改的"
    var lastReported by remember { mutableIntStateOf(selectedIndex) }
    // 程序内部滚动（循环归位 / 外部同步）期间，不向上报，避免回环
    var internalScroll by remember { mutableStateOf(false) }

    fun realOf(absolute: Int): Int =
        if (looping) ((absolute % n) + n) % n else absolute.coerceIn(0, n - 1)

    /** 当前居中位置对应的真实下标 */
    fun currentReal(): Int {
        val fractional = listState.firstVisibleItemScrollOffset / itemHeightPx
        return realOf(listState.firstVisibleItemIndex + fractional.roundToInt())
    }

    // 单一状态机：滚动中 → 震动；停止 → 归位（如有需要）→ 上报
    LaunchedEffect(listState, n, loops) {
        var lastVibrateAt = 0L
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) return@collect
                // 滚动停止

                // 1) 循环边界归位（内部动作，不上报）
                if (looping) {
                    val idx = listState.firstVisibleItemIndex
                    val fix = when {
                        idx < n -> idx + n * (loops - 2)
                        idx >= n * (loops - 1) -> idx - n * (loops - 2)
                        else -> null
                    }
                    if (fix != null) {
                        internalScroll = true
                        listState.scrollToItem(fix)
                        internalScroll = false
                        return@collect
                    }
                }

                // 2) 上报（归位或外部同步期间不上报）
                if (!internalScroll) {
                    val real = currentReal()
                    if (real != lastReported) {
                        lastReported = real
                        lastTick = real
                        onSelected(real)
                    }
                }
            }
    }

    // 跨整行震动（拖动手势进行中）
    LaunchedEffect(listState, n) {
        var lastVibrateAt = 0L
        snapshotFlow {
            val fractional = listState.firstVisibleItemScrollOffset / itemHeightPx
            listState.firstVisibleItemIndex + fractional.roundToInt()
        }.collect { absolute ->
            if (!listState.isScrollInProgress) return@collect
            val real = realOf(absolute)
            if (real != lastTick) {
                lastTick = real
                val now = System.currentTimeMillis()
                if (now - lastVibrateAt >= 45L) {
                    lastVibrateAt = now
                    haptics?.wheel()
                }
            }
        }
    }

    // 外部改值 → 滚到对应位置；只处理"不是自己上报的那个值"
    LaunchedEffect(selectedIndex, n) {
        if (selectedIndex !in items.indices) return@LaunchedEffect
        if (selectedIndex == lastReported) return@LaunchedEffect
        if (listState.isScrollInProgress) return@LaunchedEffect

        val target = if (looping) midStart + selectedIndex else selectedIndex
        internalScroll = true
        listState.animateScrollToItem(target)
        internalScroll = false
        lastTick = selectedIndex
        lastReported = selectedIndex
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(itemHeight * visibleCount)
            .clip(RoundedCornerShape(Dimens.controlRadius))
    ) {
        // 中央选中带
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(activeColor.copy(alpha = 0.05f))
                .border(1.dp, activeColor.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = fling,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = itemHeight * centerOffset)
        ) {
            items(total) { absIndex ->
                val realIndex = if (looping) absIndex % n else absIndex
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[realIndex],
                        fontSize = textSize.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = activeColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            val fractional = listState.firstVisibleItemScrollOffset / itemHeightPx
                            val center = listState.firstVisibleItemIndex + fractional
                            val distance = abs(absIndex - center)
                            val centered = distance < 0.5f

                            val scale = if (centered) 1f else (1f - distance * 0.09f).coerceIn(0.66f, 1f)
                            scaleX = scale
                            scaleY = scale
                            alpha = if (centered) 1f else (1f - distance * 0.30f).coerceIn(0.16f, 0.8f)
                        }
                    )
                }
            }
        }
    }
}
