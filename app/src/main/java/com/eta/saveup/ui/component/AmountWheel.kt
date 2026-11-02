package com.eta.saveup.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.size
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
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue

/**
 * 金额录入。
 *
 * 布局遵循常识写法：`[万][千][百][十][元] . [角分]`
 * 千/万默认折叠，点「展开高位」才出现，避免小数金额时六列挤成一排。
 */
@Composable
fun AmountWheel(
    amountCent: Long,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    maxCent: Long = 99_999_999L,
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
    val jiao = (clamped / 10L % 10).toInt()
    val fen = (clamped % 10L).toInt()

    val digitItems = remember { (0..9).map { it.toString() } }

    // 有高位金额时自动展开，避免藏起来的数据看不见
    var highExpanded by remember { mutableStateOf(clamped >= 10_000L) }
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
        // 顶部金额条
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
            Spacer(Modifier.height(Dimens.gap12))

            // 展开后列数变多，权重相应收窄
            val cellWeight = if (highExpanded) 0.85f else 1.1f

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 万 / 千：默认折叠
                AnimatedVisibility(
                    visible = highExpanded,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        DigitCell(wan, "万", { onAmountChange(rebuild(it, qian, bai, shi, yuan, jiao * 10 + fen)) }, cellWeight, haptics = haptics)
                        DigitCell(qian, "千", { onAmountChange(rebuild(wan, it, bai, shi, yuan, jiao * 10 + fen)) }, cellWeight, haptics = haptics)
                        DigitCell(bai, "百", { onAmountChange(rebuild(wan, qian, it, shi, yuan, jiao * 10 + fen)) }, cellWeight, haptics = haptics)
                    }
                }

                DigitCell(shi, "十", { onAmountChange(rebuild(wan, qian, bai, it, yuan, jiao * 10 + fen)) }, cellWeight, haptics = haptics)
                DigitCell(yuan, "元", { onAmountChange(rebuild(wan, qian, bai, shi, it, jiao * 10 + fen)) }, cellWeight, haptics = haptics)

                // 小数点：元与角的视觉分隔
                Text(
                    text = ".",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeColor,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                // 角
                DigitCell(jiao, "", { onAmountChange(rebuild(wan, qian, bai, shi, yuan, it * 10 + fen)) }, cellWeight, showLabel = false, haptics = haptics, keyTag = "jiao")
                // 分
                DigitCell(fen, "", { onAmountChange(rebuild(wan, qian, bai, shi, yuan, jiao * 10 + it)) }, cellWeight, showLabel = false, haptics = haptics, keyTag = "fen")
            }

            if (!highExpanded) {
                Spacer(Modifier.height(Dimens.gap8))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(Dimens.pillRadius))
                        .background(accent.copy(alpha = 0.08f))
                        .clickable {
                            haptics?.tick()
                            highExpanded = true
                        }
                        .padding(horizontal = Dimens.gap12, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "展开千位 / 万位",
                        fontSize = Fonts.micro,
                        color = accent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DigitCell(
    value: Int,
    label: String,
    onValue: (Int) -> Unit,
    weight: Float = 1f,
    showLabel: Boolean = true,
    haptics: HapticsWrapper? = null,
    keyTag: String = label
) {
    Box(Modifier.weight(weight)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            key(keyTag) {
                WheelColumn(
                    items = remember { (0..9).map { it.toString() } },
                    selectedIndex = value,
                    onSelected = onValue,
                    itemHeight = 44.dp,
                    visibleCount = 3,
                    textSize = 26,
                    haptics = haptics
                )
            }
            if (showLabel) {
                Text(label, fontSize = 10.sp, color = Color(0xFF878D99))
            } else {
                Spacer(Modifier.height(12.dp))
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
