package com.eta.saveup.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import kotlin.math.absoluteValue

/**
 * 金额滚轮：元 / 角 / 分 三列。
 *
 * 三列等宽、标签垂直居中，中央高亮条横跨整行，视觉上只有一条选中带。
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
    val clamped = amountCent.coerceIn(0L, maxYuan * 100L + 99L)
    val yuan = (clamped / 100).toInt()
    val jiao = ((clamped / 10) % 10).toInt()
    val fen = (clamped % 10).toInt()

    val yuanItems = remember(maxYuan) { (0..maxYuan).map { it.toString() } }
    val digitItems = remember { (0..9).map { it.toString() } }

    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.gap4)
    ) {
        WheelCell(Modifier.weight(1.7f)) {
            WheelColumn(
                items = yuanItems,
                selectedIndex = yuan,
                onSelected = { y -> onAmountChange(y * 100L + jiao * 10L + fen) },
                textSize = 40,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                haptics = haptics
            )
        }
        UnitLabel("元", labelColor)
        WheelCell(Modifier.weight(1f)) {
            WheelColumn(
                items = digitItems,
                selectedIndex = jiao,
                onSelected = { j -> onAmountChange(yuan * 100L + j * 10L + fen) },
                textSize = 40,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                haptics = haptics
            )
        }
        UnitLabel("角", labelColor)
        WheelCell(Modifier.weight(1f)) {
            WheelColumn(
                items = digitItems,
                selectedIndex = fen,
                onSelected = { f -> onAmountChange(yuan * 100L + jiao * 10L + f) },
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
private fun WheelCell(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun UnitLabel(text: String, color: Color) {
    Column(
        Modifier.width(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            fontSize = Fonts.caption,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
