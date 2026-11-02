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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 单列数字滚轮。
 *
 * 设计原则：**完全不受控**。
 *
 * - [initialIndex] 只在实例创建时读取一次，之后外部再怎么变都不影响轮子
 * - 轮子位置唯一真相是内部的 listState，绝不与外部状态双向同步
 * - 需要让轮子跳到某个值时，外部改变 [revision] 即可让整个实例重建
 *
 * 这样从结构上杜绝了「外部值 ↔ 内部滚动」的回环，
 * 也就不可能再出现自己滚动、自己跳转。
 */
private const val LOOPS = 7

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelColumn(
    items: List<String>,
    initialIndex: Int,
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
    val midStart = (loops / 2) * n

    val centerOffset = visibleCount / 2
    val startIndex = if (looping) midStart + initialIndex.coerceIn(0, n - 1) else initialIndex.coerceIn(0, n - 1)

    // 只在创建时读一次
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    fun realOf(absolute: Int): Int =
        if (looping) ((absolute % n) + n) % n else absolute.coerceIn(0, n - 1)

    var lastTick by remember { mutableIntStateOf(realOf(startIndex)) }

    // 唯一一条流：滚动中给震动，停止时归位 + 上报
    LaunchedEffect(listState, n, loops) {
        var lastVibrateAt = 0L
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress
            )
        }.collect { (rawIndex, offset, scrolling) ->
            val fractional = offset / itemHeightPx
            val absolute = rawIndex + fractional.roundToInt()
            val real = realOf(absolute)

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

            // 停止：循环边界静默归位
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

            // 上报
            if (real != lastTick) {
                lastTick = real
                onSelected(real)
            }
        }
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
