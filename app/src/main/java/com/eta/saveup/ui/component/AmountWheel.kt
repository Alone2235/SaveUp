package com.eta.saveup.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.Radius
import com.eta.saveup.ui.theme.Space

/**
 * 金额录入。
 *
 * ## 逻辑原则（这是修了很多次 bug 之后定下来的，别再破坏）
 *
 * 1. **金额只有一个真相：[amountCent]。** 本组件不维护任何数位副本。
 *    每次绘制都从 [amountCent] 现算各位数字，改值也只通过 [onAmountChange] 往外报。
 *    这样不可能出现"内部值 ≠ 外部值"的错位。
 *
 * 2. **折叠 = 该位不存在。** 万、千、百收起时，切换动作里直接把这些位
 *    从金额中清零，没有"藏着但还留着"的中间态。
 *    所以永远不会有残值污染。
 *
 * 3. **滚轮只是投影。** 滚轮的位置来自 [amountCent] 推导出的数字，
 *    外部改金额时用 key(金额) 重建，不做双向同步。
 *
 * @param highExpanded 是否展开万/千/百，由调用方持有，切换时负责清零高位
 */
@Composable
fun AmountWheel(
    amountCent: Long,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    maxCent: Long = 99_999_999L,
    haptics: HapticsWrapper? = null,
    accent: Color = HyperBlue,
    /** 是否展开高位；null 表示组件自己管理 */
    highExpanded: Boolean? = null,
    onHighExpandedChange: ((Boolean) -> Unit)? = null
) {
    val cent = amountCent.coerceIn(0L, maxCent)

    // 各位数字：直接从当前金额现算，不缓存
    val wan = ((cent / 1_000_000L) % 10).toInt()
    val qian = ((cent / 100_000L) % 10).toInt()
    val bai = ((cent / 10_000L) % 10).toInt()
    val shi = ((cent / 1_000L) % 10).toInt()
    val yuan = ((cent / 100L) % 10).toInt()
    val jiao = ((cent / 10L) % 10).toInt()
    val fen = (cent % 10L).toInt()

    var internalExpanded by remember { mutableStateOf(cent >= 10_000L) }
    val expanded = highExpanded ?: internalExpanded

    var editing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun setExpanded(v: Boolean) {
        if (highExpanded == null) internalExpanded = v
        onHighExpandedChange?.invoke(v)
    }

    /**
     * 改某一位的值。
     * @param power 该位在「分」单位下的权重（万=1_000_000 ... 分=1）
     */
    fun setDigit(power: Long, value: Int) {
        val clampedValue = value.coerceIn(0, 9).toLong()
        val current = (cent / power) % 10
        val next = cent - current * power + clampedValue * power
        onAmountChange(next.coerceIn(0L, maxCent))
    }

    /** 收起高位：高位彻底清零，不是藏起来 */
    fun collapseHigh() {
        val cleared = cent % 10_000L  // 只保留千位以下
        if (cleared != cent) onAmountChange(cleared)
        setExpanded(false)
    }

    fun expandHigh() = setExpanded(true)

    LaunchedEffect(editing) {
        if (editing) {
            editText = formatInput(cent)
            focusRequester.requestFocus()
        }
    }

    Column(modifier.fillMaxWidth()) {
        // ---- 顶部金额：点开键盘 ----
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.pill))
                .background(accent.copy(alpha = 0.07f))
                .clickable {
                    haptics?.tick()
                    editing = true
                }
                .padding(horizontal = Space.md, vertical = Space.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (editing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¥", fontSize = Fonts.title2, color = accent, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(Space.xxs))
                    BasicTextField(
                        value = editText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }
                            if (filtered.length <= 2 + 8) {
                                editText = filtered
                                parseCent(filtered)?.let {
                                    val c = it.coerceIn(0L, maxCent)
                                    onAmountChange(c)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        textStyle = TextStyle(
                            fontSize = Fonts.amountHuge,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(accent),
                        modifier = Modifier
                            .width(260.dp)
                            .focusRequester(focusRequester)
                    )
                }
            } else {
                Text(
                    text = "¥" + formatAmount(cent),
                    fontSize = Fonts.amountHuge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }

        if (editing) {
            Spacer(Modifier.height(Space.xs))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(accent)
                    .clickable {
                        haptics?.success()
                        editing = false
                    }
                    .padding(vertical = Space.xs),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "确定",
                    fontSize = Fonts.body,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            return@Column
        }

        Spacer(Modifier.height(Space.sm))

        val cellWeight = if (expanded) 0.86f else 1.12f

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (expanded) {
                DigitCell(wan, "万", cellWeight, cent, haptics) { setDigit(1_000_000L, it) }
                DigitCell(qian, "千", cellWeight, cent, haptics) { setDigit(100_000L, it) }
                DigitCell(bai, "百", cellWeight, cent, haptics) { setDigit(10_000L, it) }
            }

            DigitCell(shi, "十", cellWeight, cent, haptics) { setDigit(1_000L, it) }
            DigitCell(yuan, "元", cellWeight, cent, haptics) { setDigit(100L, it) }

            DotCell()

            DigitCell(jiao, "角", cellWeight, cent, haptics) { setDigit(10L, it) }
            DigitCell(fen, "分", cellWeight, cent, haptics) { setDigit(1L, it) }
        }

        Spacer(Modifier.height(Space.xs))
        Row(
            Modifier
                .clip(RoundedCornerShape(Radius.pill))
                .background(accent.copy(alpha = 0.08f))
                .clickable {
                    haptics?.tick()
                    if (expanded) collapseHigh() else expandHigh()
                }
                .padding(horizontal = Space.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "收起千位 / 万位（高位清零）" else "展开千位 / 万位",
                fontSize = Fonts.micro,
                color = accent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 单个数位列。
 *
 * @param currentCent 当前金额，作为滚轮重建的 key —— 金额一变就按新值重建，
 *                    滚轮自身不持有业务状态，只负责"拨到几就报几"。
 */
@Composable
private fun RowScope.DigitCell(
    digit: Int,
    label: String,
    weight: Float,
    currentCent: Long,
    haptics: HapticsWrapper?,
    onDigit: (Int) -> Unit
) {
    Box(Modifier.weight(weight)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 只在「外部改了本位」时重建滚轮。
            // 自己拨动上报 → 外部值绕回来 → 不重建（否则会打断手感）。
            var lastReported by remember { mutableStateOf(digit) }
            var rebuildKey by remember { mutableStateOf(0) }

            LaunchedEffect(digit) {
                if (digit != lastReported) {
                    lastReported = digit
                    rebuildKey++
                }
            }

            key(rebuildKey) {
                WheelColumn(
                    items = (0..9).map { it.toString() },
                    initialIndex = digit,
                    onSelected = { v ->
                        lastReported = v
                        onDigit(v)
                    },
                    itemHeight = 44.dp,
                    visibleCount = 3,
                    textSize = 26,
                    haptics = haptics
                )
            }
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF878D99),
                maxLines = 1
            )
        }
    }
}

/** 小数点：与数字列等高，垂直居中 */
@Composable
private fun DotCell() {
    Column(Modifier.width(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(44.dp * 3f), contentAlignment = Alignment.Center) {
            Text(
                text = ".",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1F27)
            )
        }
        Spacer(Modifier.height(Space.xxs))
        Text(text = " ", fontSize = 10.sp, maxLines = 1)
    }
}

private fun formatInput(cent: Long): String {
    val yuan = cent / 100
    val fen = cent % 100
    return if (fen == 0L) yuan.toString() else "$yuan.${fen.toString().padStart(2, '0')}"
}

private fun parseCent(text: String): Long? {
    if (text.isBlank() || text == ".") return null
    val parts = text.split('.')
    if (parts.size > 2) return null
    val yuan = parts[0].ifBlank { "0" }.toLongOrNull() ?: return null
    val fen = if (parts.size == 2) {
        val f = parts[1].padEnd(2, '0').take(2)
        f.toLongOrNull() ?: return null
    } else 0L
    return yuan * 100 + fen
}
