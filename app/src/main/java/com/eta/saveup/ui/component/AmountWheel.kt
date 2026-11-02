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
 * 设计取舍（踩过坑之后的结论）：
 *
 * 1. 顶部大字是主入口，点开系统数字键盘直接输，1.5 / 1.8 / 88.88 都是打几下完事。
 * 2. 滚轮只保留 [元] 和 [角] 两列 —— 没人会用滚轮拨到「分」，
 *    而多出来的列只会带来残值污染和串位问题。
 * 3. 滚轮是非受控组件：只在自己被拨动时改值，外部改值靠重建（rev）。
 * 4. 任何一位变化都即时上报完整金额，不做延迟合并。
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

    // 滚轮的唯一两个维度：整元（0..999）+ 角（0..9）
    var yuan by remember { mutableIntStateOf(((clamped / 100) % 1000).toInt()) }
    var jiao by remember { mutableIntStateOf(((clamped / 10) % 10).toInt()) }
    var rev by remember { mutableIntStateOf(0) }

    var displayCent by remember { mutableLongStateOf(clamped) }
    var lastSeen by remember { mutableLongStateOf(clamped) }

    var editing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun emit(newYuan: Int, newJiao: Int) {
        val cent = newYuan * 100L + newJiao * 10L
        displayCent = cent
        lastSeen = cent
        onAmountChange(cent)
    }

    /** 外部整体赋值（键盘输入） */
    fun applyExternal(cent: Long) {
        val c = cent.coerceIn(0L, maxCent)
        yuan = ((c / 100) % 1000).toInt()
        jiao = ((c / 10) % 10).toInt()
        displayCent = c
        rev++
    }

    LaunchedEffect(clamped) {
        if (clamped != lastSeen) {
            lastSeen = clamped
            val local = yuan * 100L + jiao * 10L
            if (clamped != local) applyExternal(clamped)
        }
    }

    LaunchedEffect(editing) {
        if (editing) {
            editText = formatInput(displayCent)
            focusRequester.requestFocus()
        }
    }

    Column(modifier.fillMaxWidth()) {
        // ---- 顶部金额条：点开键盘 ----
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
                                parseCent(filtered)?.let {
                                    val c = it.coerceIn(0, maxCent)
                                    displayCent = c
                                    lastSeen = c
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
                        // 键盘输入的金额可能含分，需要让滚轮跟着重建
                        applyExternal(displayCent)
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

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                // 元：0..999
                WheelCell(weight = 1.6f) {
                    key(rev) {
                        WheelColumn(
                            items = (0..999).map { it.toString() },
                            initialIndex = yuan.coerceIn(0, 999),
                            onSelected = { v ->
                                yuan = v
                                emit(yuan, jiao)
                            },
                            itemHeight = 44.dp,
                            visibleCount = 3,
                            textSize = 26,
                            haptics = haptics
                        )
                    }
                }
                LabelCell()

                Text(
                    text = ".",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(10.dp)
                )

                // 角：0..9
                WheelCell(weight = 0.9f) {
                    key(rev) {
                        WheelColumn(
                            items = (0..9).map { it.toString() },
                            initialIndex = jiao.coerceIn(0, 9),
                            onSelected = { v ->
                                jiao = v
                                emit(yuan, jiao)
                            },
                            itemHeight = 44.dp,
                            visibleCount = 3,
                            textSize = 26,
                            haptics = haptics
                        )
                    }
                }
                LabelCell()
            }
        }
    }
}

@Composable
private fun RowScope.WheelCell(weight: Float, content: @Composable () -> Unit) {
    Box(Modifier.weight(weight)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

/** 占位，保证每列下方对齐（滚轮没有标签） */
@Composable
private fun RowScope.LabelCell() {
    Box(Modifier.width(Space.xxs).height(1.dp))
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
