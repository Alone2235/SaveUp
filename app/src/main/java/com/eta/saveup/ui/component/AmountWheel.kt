package com.eta.saveup.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue

/**
 * 金额录入：滚轮 + 快速输入。
 *
 * 设计取舍：
 * - 「元」列负责整数部分，滚起来快
 * - 「角分」合并成一列 0..99，两列变一列，减少拨动次数
 * - 点顶部的金额可直接用系统键盘输入，大额不用拨半天
 */
@Composable
fun AmountWheel(
    amountCent: Long,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    maxYuan: Int = 999999,
    haptics: HapticsWrapper? = null,
    activeColor: Color = Color(0xFF1B1F27),
    inactiveColor: Color = Color(0xFFB6BCC8),
    labelColor: Color = Color(0xFF878D99),
    accent: Color = HyperBlue
) {
    val maxCent = maxYuan * 100L + 99L
    val clamped = amountCent.coerceIn(0L, maxCent)
    val yuan = (clamped / 100).toInt()
    val cents = (clamped % 100).toInt()

    val yuanItems = remember(maxYuan) { (0..maxYuan).map { it.toString() } }
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
                            .width(240.dp)
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
                    .padding(vertical = Dimens.gap8),
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
                horizontalArrangement = Arrangement.spacedBy(Dimens.gap4)
            ) {
                Box(Modifier.weight(1.6f)) {
                    WheelColumn(
                        items = yuanItems,
                        selectedIndex = yuan,
                        onSelected = { y -> onAmountChange(y * 100L + cents) },
                        textSize = 40,
                        activeColor = activeColor,
                        inactiveColor = inactiveColor,
                        haptics = haptics
                    )
                }
                Text("元", fontSize = Fonts.body, color = labelColor)
                Box(Modifier.weight(1f)) {
                    WheelColumn(
                        items = centItems,
                        selectedIndex = cents,
                        onSelected = { c -> onAmountChange(yuan * 100L + c) },
                        textSize = 40,
                        activeColor = activeColor,
                        inactiveColor = inactiveColor,
                        haptics = haptics
                    )
                }
                Text("角分", fontSize = Fonts.caption, color = labelColor)
            }
        }
    }
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
