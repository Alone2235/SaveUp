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
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue

/**
 * 金额录入：数位滚轮 + 直接键盘输入。
 *
 * 滚轮是非受控组件：每列只在自己被拨动时更新数值，
 * 外部修改（键盘输入、金额归零）通过递增 [rev] 让该列重建，不做双向同步。
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

    // 各数位本地状态。滚轮是唯一修改源，这里只做镜像用于顶部大字显示。
    var wan by remember { mutableIntStateOf((clamped / 1_000_000L % 10).toInt()) }
    var qian by remember { mutableIntStateOf((clamped / 100_000L % 10).toInt()) }
    var bai by remember { mutableIntStateOf((clamped / 10_000L % 10).toInt()) }
    var shi by remember { mutableIntStateOf((clamped / 1_000L % 10).toInt()) }
    var yuan by remember { mutableIntStateOf((clamped / 100L % 10).toInt()) }
    var jiao by remember { mutableIntStateOf((clamped / 10L % 10).toInt()) }
    var fen by remember { mutableIntStateOf((clamped % 10L).toInt()) }

    // 每列的重建版本号：外部改值时 +1，让滚轮按新值重新创建
    var rev by remember { mutableIntStateOf(0) }

    // 上一次父级可见的金额；用于区分"自己推送的"与"外部修改的"
    var lastSeen by remember { mutableLongStateOf(clamped) }

    var highExpanded by remember { mutableStateOf(clamped >= 10_000L) }
    var editing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun currentValue(): Long =
        wan * 1_000_000L + qian * 100_000L + bai * 10_000L +
            shi * 1_000L + yuan * 100L + jiao * 10L + fen

    fun push() {
        lastSeen = currentValue()
        onAmountChange(currentValue())
    }

    /** 外部设定整个金额（键盘输入）：重算所有位并重建滚轮 */
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
        rev++
    }

    LaunchedEffect(clamped) {
        // 父级值变了 != 自己推送出去的值 → 属于外部修改，重建滚轮
        if (clamped != lastSeen) {
            lastSeen = clamped
            if (clamped != currentValue()) applyExternal(clamped)
        }
    }

    LaunchedEffect(editing) {
        if (editing) {
            editText = formatInput(currentValue())
            focusRequester.requestFocus()
        }
    }

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
                    text = "¥" + formatAmount(currentValue()),
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
                        push()
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

            val cellWeight = if (highExpanded) 0.85f else 1.1f

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AnimatedVisibility(
                    visible = highExpanded,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        DigitCell(wan, "万", cellWeight, rev, haptics) {
                            wan = it; push()
                        }
                        DigitCell(qian, "千", cellWeight, rev, haptics) {
                            qian = it; push()
                        }
                        DigitCell(bai, "百", cellWeight, rev, haptics) {
                            bai = it; push()
                        }
                    }
                }

                DigitCell(shi, "十", cellWeight, rev, haptics) {
                    shi = it; push()
                }
                DigitCell(yuan, "元", cellWeight, rev, haptics) {
                    yuan = it; push()
                }

                Text(
                    text = ".",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeColor,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                DigitCell(jiao, "", cellWeight, rev, haptics, showLabel = false) {
                    jiao = it; push()
                }
                DigitCell(fen, "", cellWeight, rev, haptics, showLabel = false) {
                    fen = it; push()
                }
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
    weight: Float,
    revision: Int,
    haptics: HapticsWrapper?,
    showLabel: Boolean = true,
    onValue: (Int) -> Unit
) {
    Box(Modifier.weight(weight)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // revision 变化 → 整个滚轮重建并按新值定位
            key(revision) {
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
