package com.eta.saveup.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.eta.saveup.ui.theme.Fonts
import kotlin.math.abs

/**
 * 单列数字滚轮：选中项放大居中，上下渐隐缩小，滚动时给齿感震动。
 * 用 snapFlingBehavior 保证停在整行上。
 *
 * @param items 候选项文本
 * @param selectedIndex 当前选中下标
 * @param onSelected 停止滚动后回调选中下标
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
    val centerOffset = visibleCount / 2
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - centerOffset).coerceIn(0, (items.size - 1).coerceAtLeast(0))
    )
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    var lastIndex by remember { mutableIntStateOf(selectedIndex) }

    // 滚动停止 → 读当前居中项
    LaunchedEffect(listState, items.size) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemScrollOffset }
            .collect { (scrolling, offset) ->
                val idx = (listState.firstVisibleItemIndex + centerOffset).coerceIn(0, items.lastIndex)
                if (scrolling) {
                    if (idx != lastIndex && offset < itemHeightPx / 3) {
                        lastIndex = idx
                        haptics?.wheel()
                    }
                } else {
                    if (idx != lastIndex) {
                        lastIndex = idx
                        haptics?.wheel()
                    }
                    onSelected(idx)
                }
            }
    }

    // 外部改值 → 滚过去
    LaunchedEffect(selectedIndex, items.size) {
        if (selectedIndex in items.indices) {
            val target = (selectedIndex - centerOffset).coerceIn(0, (items.size - 1).coerceAtLeast(0))
            if (listState.firstVisibleItemIndex != target) {
                listState.scrollToItem(target)
                lastIndex = selectedIndex
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(itemHeight * visibleCount)
            .clip(RoundedCornerShape(Dimens.controlRadius))
    ) {
        // 中央高亮条
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = Dimens.gap4)
                .clip(RoundedCornerShape(Dimens.controlRadius))
                .background(activeColor.copy(alpha = 0.06f))
        )

        LazyColumn(
            state = listState,
            flingBehavior = fling,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = itemHeight * centerOffset)
        ) {
            items(items.size) { index ->
                val centered = (listState.firstVisibleItemIndex + centerOffset) == index
                val scale = if (centered) 1f else 0.74f
                val alpha = if (centered) 1f else
                    (1f - abs(index - (listState.firstVisibleItemIndex + centerOffset)) * 0.4f)
                        .coerceIn(0.18f, 0.72f)

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        fontSize = if (centered) textSize.sp else (textSize * 0.72f).sp,
                        fontWeight = if (centered) FontWeight.Bold else FontWeight.Medium,
                        color = if (centered) activeColor else inactiveColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .alpha(alpha)
                    )
                }
            }
        }
    }
}

/**
 * 金额滚轮：元 + 角 + 分 三段联动。
 */
@Composable
fun AmountWheel(
    amountCent: Long,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    maxYuan: Int = 99999,
    haptics: HapticsWrapper? = null,
    activeColor: Color = Color(0xFF1B1F27),
    inactiveColor: Color = Color(0xFFB6BCC8),
    labelColor: Color = Color(0xFF878D99)
) {
    val yuan = (amountCent / 100).toInt().coerceIn(0, maxYuan)
    val jiao = ((amountCent / 10) % 10).toInt().coerceIn(0, 9)
    val fen = (amountCent % 10).toInt().coerceIn(0, 9)

    val yuanItems = remember(maxYuan) { (0..maxYuan).map { it.toString() } }
    val digitItems = remember { (0..9).map { it.toString() } }

    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.gap4)
    ) {
        Box(Modifier.weight(1.6f)) {
            WheelColumn(
                items = yuanItems,
                selectedIndex = yuan,
                onSelected = { onAmountChange(it * 100L + jiao * 10L + fen) },
                textSize = 40,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                haptics = haptics
            )
        }
        UnitLabel("元", labelColor)
        Box(Modifier.weight(1f)) {
            WheelColumn(
                items = digitItems,
                selectedIndex = jiao,
                onSelected = { onAmountChange(yuan * 100L + it * 10L + fen) },
                textSize = 40,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                haptics = haptics
            )
        }
        UnitLabel("角", labelColor)
        Box(Modifier.weight(1f)) {
            WheelColumn(
                items = digitItems,
                selectedIndex = fen,
                onSelected = { onAmountChange(yuan * 100L + jiao * 10L + it) },
                textSize = 40,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                haptics = haptics
            )
        }
        UnitLabel("分", labelColor)
    }
}

@Composable
private fun UnitLabel(text: String, color: Color) {
    Text(
        text = text,
        fontSize = Fonts.body,
        color = color,
        modifier = Modifier.padding(bottom = Dimens.gap4)
    )
}
