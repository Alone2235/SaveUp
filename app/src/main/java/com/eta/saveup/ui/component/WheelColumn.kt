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
    // 本列最近一次主动上报给外部的值；用于区分"自己滚的"与"外部要求改的"
    var lastReported by remember { mutableIntStateOf(selectedIndex) }

    /** 当前居中位置对应的「真实下标」 */
    fun currentRealIndex(): Int {
        val fractional = listState.firstVisibleItemScrollOffset / itemHeightPx
        val absolute = listState.firstVisibleItemIndex + fractional.roundToInt()
        return if (looping) ((absolute % n) + n) % n else absolute.coerceIn(0, n - 1)
    }

    // 震动 + 选中回调
    LaunchedEffect(listState, n) {
        var lastVibrateAt = 0L
        snapshotFlow {
            val fractional = listState.firstVisibleItemScrollOffset / itemHeightPx
            val absolute = listState.firstVisibleItemIndex + fractional.roundToInt()
            val real = if (looping) ((absolute % n) + n) % n else absolute.coerceIn(0, n - 1)
            real to listState.isScrollInProgress
        }.collect { (real, scrolling) ->
            if (real != lastTick) {
                lastTick = real
                if (scrolling) {
                    val now = System.currentTimeMillis()
                    if (now - lastVibrateAt >= 45L) {
                        lastVibrateAt = now
                        haptics?.wheel()
                    }
                }
            }
            if (!scrolling) {
                lastReported = real
                onSelected(real)
            }
        }
    }

    // 循环边界静默归位：只在本列滚动停止后校正，避免打断滚动、也避免互相干扰
    LaunchedEffect(listState, n, loops) {
        if (!looping) return@LaunchedEffect
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) return@collect
                val idx = listState.firstVisibleItemIndex
                when {
                    idx < n -> listState.scrollToItem(idx + n * (loops - 2))
                    idx >= n * (loops - 1) -> listState.scrollToItem(idx - n * (loops - 2))
                }
            }
    }

    // 外部改值 → 滚到对应位置。
    // 用「本列自己最近上报过的值」做闸门：别的列滚动引起的重组不会误触发本列滚动。
    LaunchedEffect(selectedIndex, n) {
        if (selectedIndex !in items.indices) return@LaunchedEffect
        if (selectedIndex == lastReported) return@LaunchedEffect // 就是自己上报的值，不动
        if (listState.isScrollInProgress) return@LaunchedEffect // 正在滚，不打扰

        val target = if (looping) midStart + selectedIndex else selectedIndex
        listState.animateScrollToItem(target)
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
