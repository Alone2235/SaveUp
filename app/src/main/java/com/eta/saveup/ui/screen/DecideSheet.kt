package com.eta.saveup.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.eta.saveup.data.repo.GoalView
import com.eta.saveup.ui.component.HapticsWrapper
import com.eta.saveup.ui.component.formatYuan
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.Radius
import com.eta.saveup.ui.theme.Space
import com.eta.saveup.ui.theme.SuccessGreen

/**
 * 决定页：面对一笔想花的钱，选「留着」还是「买」。
 *
 * 设计原则：
 * - 只给两个按钮，点完就结束，不做二次确认、不说教
 * - 选「买」不算失败，只是如实记录
 * - 商品名可以留空，价格必填
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DecideSheet(
    goals: List<GoalView>,
    haptics: HapticsWrapper,
    initialGoalId: Long? = null,
    onDismiss: () -> Unit,
    onKeep: (goalId: Long?, amountCent: Long, title: String) -> Unit,
    onBuy: (goalId: Long?, amountCent: Long, title: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var amountCent by remember { mutableLongStateOf(0L) }
    var amountText by remember { mutableStateOf("") }
    var goalId by remember { mutableStateOf(initialGoalId) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFBFCFE)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.page)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = Space.lg)
        ) {
            // 顶栏
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "想买点什么",
                    fontSize = Fonts.title1,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1F27),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(Radius.small))
                        .background(Color(0xFFEDF0F6))
                        .clickable {
                            haptics.tick()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = Color(0xFF878D99),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(Space.lg))

            // 商品名（可空）
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.small))
                    .background(Color(0xFFEDF0F6))
                    .padding(horizontal = Space.md, vertical = Space.sm)
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = "买什么？（可不填）",
                        fontSize = Fonts.body,
                        color = Color(0xFF9AA1AE)
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = { if (it.length <= 30) title = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = Fonts.body,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1B1F27)
                    ),
                    cursorBrush = SolidColor(HyperBlue),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(Space.md))

            // 金额大字
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "¥",
                    fontSize = Fonts.title1,
                    color = if (amountCent > 0) HyperBlue else Color(0xFFB6BCC8),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Spacer(Modifier.width(4.dp))
                Box {
                    if (amountText.isEmpty()) {
                        Text(
                            text = "0",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD5DBE5)
                        )
                    }
                    BasicTextField(
                        value = amountText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }
                            if (filtered.length <= 10) {
                                amountText = filtered
                                amountCent = parseCent(filtered) ?: 0L
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        textStyle = TextStyle(
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = HyperBlue,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(HyperBlue),
                        modifier = Modifier
                            .width(220.dp)
                            .focusRequester(focusRequester)
                    )
                }
            }

            Spacer(Modifier.height(Space.lg))

            // 目标选择
            if (goals.isNotEmpty()) {
                Text(
                    text = if (goalId == null) "留着的话，存进哪个目标？" else "选中的目标",
                    fontSize = Fonts.caption,
                    color = Color(0xFF878D99),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = Space.xxs, bottom = Space.xs)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    goals.take(3).forEach { gv ->
                        val selected = goalId == gv.goal.id
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) HyperBlue else Color(0xFFEDF0F6))
                                .clickable {
                                    haptics.tick()
                                    goalId = if (selected) null else gv.goal.id
                                }
                                .padding(horizontal = Space.sm, vertical = Space.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(gv.goal.emoji, fontSize = Fonts.caption)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = gv.goal.title,
                                fontSize = Fonts.micro,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) Color.White else Color(0xFF1B1F27),
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Space.lg))
            }

            val enabled = amountCent > 0

            // 留着
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(if (enabled) SuccessGreen else Color(0xFFDCE0E8))
                    .clickable(enabled = enabled) {
                        haptics.success()
                        onKeep(goalId, amountCent, title)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (enabled) "留着 · ${formatYuan(amountCent)}" else "留着",
                    fontSize = Fonts.bodyStrong,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) Color.White else Color(0xFF9AA1AE)
                )
            }

            Spacer(Modifier.height(Space.xs))

            // 买
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Color(0xFFEDF0F6))
                    .clickable(enabled = enabled) {
                        haptics.warn()
                        onBuy(goalId, amountCent, title)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "买了",
                    fontSize = Fonts.body,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) Color(0xFF878D99) else Color(0xFFB6BCC8)
                )
            }

            Spacer(Modifier.height(Space.xs))
            Text(
                text = "选哪个都行，如实记下来就好",
                fontSize = Fonts.micro,
                color = Color(0xFFB6BCC8),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
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
