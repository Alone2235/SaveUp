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
import androidx.compose.ui.draw.alpha
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
import kotlin.math.abs

/**
 * 单列数字滚轮（LazyColumn + snapFlingBehavior 版）。
 *
 * 滑动质感由 Compose 原生 fling + 吸附提供，这是最顺手的方案。
 *
 * 对齐说明：
 * - contentPadding 上下各留 [visibleCount]/2 行，使第 0 项一开始就在正中
 * - 选中项 = firstVisibleItemIndex + 滚动偏移换算出的整行，含偏移量误差，不会差半格
 * - 高亮带高度 == itemHeight 且垂直居中，与选中项天然重合
 */
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

    val centerOffset = visibleCount / 2
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0
    )
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val maxIndex = items.lastIndex

    var lastTick by remember { mutableIntStateOf(selectedIndex) }

    // 当前居中的整数下标（考虑滚动偏移的进位）
    fun currentIndex(): Int {
        val fractional = listState.firstVisibleItemScrollOffset / itemHeightPx
        return (listState.firstVisibleItemIndex + Math.round(fractional)).coerceIn(0, maxIndex)
    }

    // 震动：跨越整行就触发
    LaunchedEffect(listState) {
        snapshotFlow {
            val fractional = listState.firstVisibleItemScrollOffset / itemHeightPx
            (listState.firstVisibleItemIndex + Math.round(fractional)).coerceIn(0, maxIndex)
        }.collect { idx ->
            if (idx != lastTick) {
                lastTick = idx
                haptics?.wheel()
            }
        }
    }

    // 滚动停止 → 回调选中值
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) onSelected(currentIndex())
            }
    }

    // 外部改值 → 滚过去
    LaunchedEffect(selectedIndex, items.size) {
        if (selectedIndex in items.indices) {
            if (listState.firstVisibleItemIndex != selectedIndex) {
                listState.scrollToItem(selectedIndex)
                lastTick = selectedIndex
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(itemHeight * visibleCount)
            .clip(RoundedCornerShape(Dimens.controlRadius))
    ) {
        // 中央选中带（高度与一行一致，垂直居中）
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
            items(items.size) { index ->
                val fractional = listState.firstVisibleItemScrollOffset / itemHeightPx
                val center = listState.firstVisibleItemIndex + fractional
                val distance = abs(index - center)
                val centered = distance < 0.5f

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        fontSize = if (centered) textSize.sp else (textSize * 0.78f).sp,
                        fontWeight = if (centered) FontWeight.Bold else FontWeight.Medium,
                        color = if (centered) activeColor else inactiveColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .graphicsLayer {
                                val scale = if (centered) 1f else (1f - distance * 0.09f).coerceIn(0.66f, 1f)
                                scaleX = scale
                                scaleY = scale
                            }
                            .alpha(if (centered) 1f else (1f - distance * 0.30f).coerceIn(0.16f, 0.8f))
                    )
                }
            }
        }
    }
}
