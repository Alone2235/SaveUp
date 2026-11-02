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
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue

/**
 * 按数位分列的金额滚轮：万 / 千 / 百 / 十 / 元 / 角分。
 *
 * 每列只管一个数位（角分列管 0..99），拨动上限固定，
 * 设 8999 只需要拨 4 下，而不是从 0 拨到 8999。
 *
 * 顶部大字可直接点开系统键盘输入，大额一步到位。
 */
@Composable
fun AmountWheel(
    amountCent: Long,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    maxCent: Long = 99_999_999L, // 最高 999,999.99
    haptics: HapticsWrapper? = null,
    activeColor: Color = Color(0xFF1B1F27),
    inactiveColor: Color = Color(0xFFB6BCC8),
    labelColor: Color = Color(0xFF878D99),
    accent: Color = HyperBlue
) {
    val clamped = amountCent.coerceIn(0L, maxCent)
    val wan = (clamped / 1_000_000L % 10).toInt()
    val qian = (clamped / 100_000L % 10).toInt()
    val bai = (clamped / 10_000L % 10).toInt()
    val shi = (clamped / 1_000L % 10).toInt()
    val yuan = (clamped / 100L % 10).toInt()
    val cents = (clamped % 100).toInt()

    val digitItems = remember { (0..9).map { it.toString() } }
    val centItems = remember { (0..99).map { it.toString().padStart(2, '0') } }

    var editing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(editing) {
        if (editing) {
            editText = formatInput(clamped)
            focusRequester.requestFocus()
        }
    }

    fun rebuild(w: Int, q: Int, b: Int, s: Int, y: Int, c: Int): Long =
        w * 1_000_000L + q * 100_000L + b * 10_000L + s * 1_000L + y * 100L + c

    Column(modifier.fillMaxWidth()) {
        // 顶部金额条：点击进入键盘输入
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.pillRadius))
                .background(accent.copy(alpha = 0.07f))
                .clickable {
                    haptics?.tick()
                    editing = true
                }
                .padding(horizontal = Dimens.gap16, vertical = Dimens.gap12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (editing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¥", fontSize = Fonts.title2, color = accent, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(Dimens.gap4))
                    BasicTextField(
                        value = editText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }
                            if (filtered.length <= 12) {
                                editText = filtered
                                parseCent(filtered)?.let { onAmountChange(it.coerceIn(0, maxCent)) }
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
                    text = "¥" + formatAmount(clamped),
                    fontSize = Fonts.amountHuge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }

        if (editing) {
            Spacer(Modifier.height(Dimens.gap8))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.pillRadius))
                    .background(accent)
                    .clickable {
                        haptics?.success()
                        editing = false
                    }
                    .padding(vertical = Dimens.gap10),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "确定",
                    fontSize = Fonts.body,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        } else {
            Spacer(Modifier.height(Dimens.gap4))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                DigitCell(wan, "万") { onAmountChange(rebuild(it, qian, bai, shi, yuan, cents)) }
                DigitCell(qian, "千") { onAmountChange(rebuild(wan, it, bai, shi, yuan, cents)) }
                DigitCell(bai, "百") { onAmountChange(rebuild(wan, qian, it, shi, yuan, cents)) }
                DigitCell(shi, "十") { onAmountChange(rebuild(wan, qian, bai, it, yuan, cents)) }
                DigitCell(yuan, "元") { onAmountChange(rebuild(wan, qian, bai, shi, it, cents)) }

                Box(Modifier.weight(1.1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MiniWheel(
                            items = centItems,
                            selected = cents,
                            onSelected = { onAmountChange(rebuild(wan, qian, bai, shi, yuan, it)) },
                            haptics = haptics,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            textSize = 26
                        )
                        Text("角分", fontSize = 10.sp, color = labelColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DigitCell(
    value: Int,
    label: String,
    onValue: (Int) -> Unit
) {
    Box(Modifier.weight(0.85f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MiniWheel(
                items = remember { (0..9).map { it.toString() } },
                selected = value,
                onSelected = onValue,
                textSize = 26
            )
            Text(label, fontSize = 10.sp, color = Color(0xFF878D99))
        }
    }
}

@Composable
private fun MiniWheel(
    items: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
    haptics: HapticsWrapper? = null,
    activeColor: Color = Color(0xFF1B1F27),
    inactiveColor: Color = Color(0xFFB6BCC8),
    textSize: Int = 26
) {
    WheelColumn(
        items = items,
        selectedIndex = selected,
        onSelected = onSelected,
        itemHeight = 40.dp,
        visibleCount = 3,
        textSize = textSize,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        haptics = haptics
    )
}

private fun formatInput(cent: Long): String {
    val yuan = cent / 100
    val fen = cent % 100
    return if (fen == 0L) yuan.toString() else "$yuan.${fen.toString().padStart(2, '0')}"
}

/** "12.34" → 1234；非法返回 null */
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
