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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
 * 数位分列：万 / 千 / 百 / 十 / 元 . 角 / 分，七列全部保留。
 *
 * 万、千、百默认折叠 —— 但**折叠即清零**：
 * 收起高位的瞬间把那几位写回 0，并把金额同步上报。
 * 这样既不会出现「收起后残值污染下次输入」的老问题，
 * 也不会为了绕 bug 把列合并掉。
 *
 * 顶部大字可直接点开键盘输入，含分位的精确金额走这条路。
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

    var wan by remember { mutableIntStateOf((clamped / 1_000_000L % 10).toInt()) }
    var qian by remember { mutableIntStateOf((clamped / 100_000L % 10).toInt()) }
    var bai by remember { mutableIntStateOf((clamped / 10_000L % 10).toInt()) }
    var shi by remember { mutableIntStateOf((clamped / 1_000L % 10).toInt()) }
    var yuan by remember { mutableIntStateOf((clamped / 100L % 10).toInt()) }
    var jiao by remember { mutableIntStateOf((clamped / 10L % 10).toInt()) }
    var fen by remember { mutableIntStateOf((clamped % 10L).toInt()) }

    var displayCent by remember { mutableLongStateOf(clamped) }
    var lastSeen by remember { mutableLongStateOf(clamped) }

    // 高位是否展开
    var highExpanded by remember { mutableStateOf(clamped >= 10_000L) }

    var editing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun calcCent(w: Int = wan, q: Int = qian, b: Int = bai, s: Int = shi,
                 y: Int = yuan, j: Int = jiao, f: Int = fen): Long =
        w * 1_000_000L + q * 100_000L + b * 10_000L +
            s * 1_000L + y * 100L + j * 10L + f

    /** 上报当前完整金额 */
    fun push(cent: Long = calcCent()) {
        if (cent == displayCent) return
        displayCent = cent
        lastSeen = cent
        onAmountChange(cent)
    }

    /** 外部整体赋值（键盘输入） */
    fun applyExternal(cent: Long) {
        val c = cent.coerceIn(0L, maxCent)
        wan = (c / 1_000_000L % 10).toInt()
        qian = (c / 100_000L % 10).toInt()
        bai = (c / 10_000L % 10).toInt()
        shi = (c / 1_000L % 10).toInt()
        yuan = (c / 100L % 10).toInt()
        jiao = (c / 10L % 10).toInt()
        fen = (c % 10L).toInt()
        if (c >= 10_000L) highExpanded = true
        displayCent = c
    }

    /** 收起高位：先清零再收起，避免残值留在内存里 */
    fun collapseHigh() {
        if (!highExpanded) return
        if (wan != 0 || qian != 0 || bai != 0) {
            wan = 0; qian = 0; bai = 0
            push(calcCent(w = 0, q = 0, b = 0))
        }
        highExpanded = false
    }

    /** 展开高位：按当前金额定位，不携带脏值 */
    fun expandHigh() {
        if (highExpanded) return
        highExpanded = true
    }

    LaunchedEffect(clamped) {
        if (clamped != lastSeen) {
            lastSeen = clamped
            if (clamped != calcCent()) applyExternal(clamped)
        }
    }

    LaunchedEffect(editing) {
        if (editing) {
            editText = formatInput(displayCent)
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
                            if (filtered.length <= 12) {
                                editText = filtered
                                parseCent(filtered)?.let { applyExternal(it) }
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
                    text = "¥" + formatAmount(displayCent),
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
        } else {
            Spacer(Modifier.height(Space.sm))

            // 展开后列多，权重相应收窄；收起时留白更多
            val cellWeight = if (highExpanded) 0.86f else 1.12f

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (highExpanded) {
                    DigitCell(wan, "万", cellWeight, haptics) { v ->
                        wan = v; push(calcCent(w = v))
                    }
                    DigitCell(qian, "千", cellWeight, haptics) { v ->
                        qian = v; push(calcCent(q = v))
                    }
                    DigitCell(bai, "百", cellWeight, haptics) { v ->
                        bai = v; push(calcCent(b = v))
                    }
                }

                DigitCell(shi, "十", cellWeight, haptics) { v ->
                    shi = v; push(calcCent(s = v))
                }
                DigitCell(yuan, "元", cellWeight, haptics) { v ->
                    yuan = v; push(calcCent(y = v))
                }

                DotCell()

                DigitCell(jiao, "角", cellWeight, haptics) { v ->
                    jiao = v; push(calcCent(j = v))
                }
                DigitCell(fen, "分", cellWeight, haptics) { v ->
                    fen = v; push(calcCent(f = v))
                }
            }

            Spacer(Modifier.height(Space.xs))
            Row(
                Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(accent.copy(alpha = 0.08f))
                    .clickable {
                        haptics?.tick()
                        if (highExpanded) collapseHigh() else expandHigh()
                    }
                    .padding(horizontal = Space.sm, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (highExpanded) "收起千位 / 万位" else "展开千位 / 万位",
                    fontSize = Fonts.micro,
                    color = accent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** 单个数位列：滚轮 + 下方单位标签，各列等高 */
@Composable
private fun RowScope.DigitCell(
    value: Int,
    label: String,
    weight: Float,
    haptics: HapticsWrapper?,
    onValue: (Int) -> Unit
) {
    Box(Modifier.weight(weight)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 非受控滚轮：内部只在自身被拨动时回调
            key(label) {
                WheelColumn(
                    items = (0..9).map { it.toString() },
                    initialIndex = value,
                    onSelected = onValue,
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
        Box(
            Modifier.height(44.dp * 3f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ".",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1F27)
            )
        }
        Spacer(Modifier.height(Space.xxs))
        Text(
            text = " ",
            fontSize = 10.sp,
            maxLines = 1
        )
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
