package com.eta.saveup.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.Radius
import com.eta.saveup.ui.theme.Space

/**
 * 金额滚轮：**元** 和 **角** 两列。
 *
 * 这是踩了很多坑之后定下来的最简形态：
 * - 只有两列，不存在折叠、残值污染、列数变化这些衍生问题
 * - 元 0..999，角 0..9
 * - 需要精确到分的时候，用系统键盘（由调用方负责）
 * - 滚轮是非受控的：只在被拨动时回调，外部改值靠 key 重建
 */
@Composable
fun AmountWheel(
    amountCent: Long,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    haptics: HapticsWrapper? = null,
    accent: Color = HyperBlue
) {
    val cent = amountCent.coerceIn(0L, 99999_900L)
    val yuan = ((cent / 100) % 1000).toInt()
    val jiao = ((cent / 10) % 10).toInt()

    // 每列的外部同步版本号：金额被外部整体替换时递增，让滚轮按新值重建
    var yuanRev by remember { mutableIntStateOf(0) }
    var jiaoRev by remember { mutableIntStateOf(0) }
    var lastReported by remember { mutableIntStateOf(cent.toInt()) }

    // 外部值与本地上次上报不一致 → 认为是外部改动，重建两列
    if (cent.toInt() != lastReported) {
        val newYuan = ((cent / 100) % 1000).toInt()
        val newJiao = ((cent / 10) % 10).toInt()
        val oldYuan = ((lastReported / 100) % 1000)
        val oldJiao = ((lastReported / 10) % 10)
        lastReported = cent.toInt()
        if (newYuan != oldYuan) yuanRev++
        if (newJiao != oldJiao) jiaoRev++
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            // 元
            UnitCell(weight = 1.7f, label = "元") {
                key(yuanRev) {
                    WheelColumn(
                        items = (0..999).map { it.toString() },
                        initialIndex = yuan,
                        onSelected = { v ->
                            lastReported = (v * 100 + jiao * 10)
                            onAmountChange(v * 100L + jiao * 10L)
                        },
                        itemHeight = 52.dp,
                        visibleCount = 3,
                        textSize = 34,
                        haptics = haptics
                    )
                }
            }

            Text(
                text = ".",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1F27),
                modifier = Modifier.width(14.dp)
            )

            // 角
            UnitCell(weight = 1f, label = "角") {
                key(jiaoRev) {
                    WheelColumn(
                        items = (0..9).map { it.toString() },
                        initialIndex = jiao,
                        onSelected = { v ->
                            lastReported = (yuan * 100 + v * 10)
                            onAmountChange(yuan * 100L + v * 10L)
                        },
                        itemHeight = 52.dp,
                        visibleCount = 3,
                        textSize = 34,
                        haptics = haptics
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.xs))
        Text(
            text = formatYuan(cent),
            fontSize = Fonts.caption,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF878D99)
        )
    }
}

@Composable
private fun RowScope.UnitCell(
    weight: Float,
    label: String,
    content: @Composable () -> Unit
) {
    Column(
        Modifier.weight(weight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
        Spacer(Modifier.height(Space.xxs))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF878D99),
            maxLines = 1
        )
    }
}
