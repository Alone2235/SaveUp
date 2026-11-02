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

    // 唯一真相：listState 的位置。不维护影子状态，只用它判断"是否用户主动操作"。
    var lastTick by remember { mutableIntStateOf(selectedIndex) }

    var lastSyncedValue by remember { mutableIntStateOf(selectedIndex) }
    // 外部同步期间屏蔽上报，避免"同步→上报→重组→再同步"的回环
    var suppressReport by remember { mutableStateOf(false) }

    fun realOf(absolute: Int): Int =
        if (looping) ((absolute % n) + n) % n else absolute.coerceIn(0, n - 1)

    fun currentReal(): Int {
        val fractional = listState.firstVisibleItemScrollOffset / itemHeightPx
        return realOf(listState.firstVisibleItemIndex + fractional.roundToInt())
    }

    // 用户操作驱动：跨行震动 + 停止后上报
    LaunchedEffect(listState, n, loops) {
        var lastVibrateAt = 0L
        snapshotFlow {
            val fractional = listState.firstVisibleItemScrollOffset / itemHeightPx
            val absolute = listState.firstVisibleItemIndex + fractional.roundToInt()
            Triple(realOf(absolute), listState.isScrollInProgress, listState.firstVisibleItemIndex)
        }.collect { (real, scrolling, rawIndex) ->
            if (scrolling) {
                if (real != lastTick) {
                    lastTick = real
                    val now = System.currentTimeMillis()
                    if (now - lastVibrateAt >= 45L) {
                        lastVibrateAt = now
                        haptics?.wheel()
                    }
                }
                return@collect
            }

            // 停止：先做循环归位，再上报
            if (looping) {
                val fix = when {
                    rawIndex < n -> rawIndex + n * (loops - 2)
                    rawIndex >= n * (loops - 1) -> rawIndex - n * (loops - 2)
                    else -> null
                }
                if (fix != null) {
                    listState.scrollToItem(fix)
                    return@collect
                }
            }

            if (suppressReport) return@collect

            val settled = currentReal()
            if (settled != lastTick || settled != lastSyncedValue) {
                lastTick = settled
                lastSyncedValue = settled
                onSelected(settled)
            }
        }
    }

    // 外部同步：只在"外部值确实变了"时执行一次，用序号避免重复
    LaunchedEffect(selectedIndex) {
        if (selectedIndex !in items.indices) return@LaunchedEffect
        if (listState.isScrollInProgress) return@LaunchedEffect
        // 与当前轮子位置一致 → 不需要动
        if (selectedIndex == currentReal()) {
            lastSyncedValue = selectedIndex
            lastTick = selectedIndex
            return@LaunchedEffect
        }

        suppressReport = true
        listState.scrollToItem(if (looping) midStart + selectedIndex else selectedIndex)
        lastSyncedValue = selectedIndex
        lastTick = selectedIndex
        // 等一拍让 snapshotFlow 把这次内部滚动消化掉，再恢复上报
        kotlinx.coroutines.delay(80)
        suppressReport = false
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
