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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eta.saveup.data.repo.GoalView
import com.eta.saveup.ui.component.AmountWheel
import com.eta.saveup.ui.component.HapticsWrapper
import com.eta.saveup.ui.component.formatYuan
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.Radius
import com.eta.saveup.ui.theme.Space
import com.eta.saveup.ui.theme.SuccessGreen

/**
 * 记一笔「我忍住了」。
 *
 * 流程只有三步：滚轮选金额 → 选目标 → 确认。
 * 不做二次确认，不区分"待转入"，点确认就是攒下了。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun KeepSheet(
    goals: List<GoalView>,
    haptics: HapticsWrapper,
    initialGoalId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (goalId: Long?, amountCent: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountCent by remember { mutableLongStateOf(0L) }
    var goalId by remember { mutableStateOf(initialGoalId ?: goals.firstOrNull()?.goal?.id) }

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
                .padding(bottom = Space.lg)
        ) {
            Text(
                text = "我忍住了",
                fontSize = Fonts.title1,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1F27)
            )
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = "本来要花的钱，存进目标",
                fontSize = Fonts.caption,
                color = Color(0xFF878D99)
            )

            Spacer(Modifier.height(Space.lg))

            // 滚轮选金额
            AmountWheel(
                amountCent = amountCent,
                onAmountChange = { amountCent = it },
                haptics = haptics,
                accent = SuccessGreen
            )

            Spacer(Modifier.height(Space.lg))

            // 选目标
            if (goals.isNotEmpty()) {
                Text(
                    text = "存进",
                    fontSize = Fonts.caption,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF878D99),
                    modifier = Modifier.padding(start = Space.xxs, bottom = Space.xs)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    goals.take(3).forEach { gv ->
                        val selected = goalId == gv.goal.id
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) SuccessGreen else Color(0xFFEDF0F6))
                                .clickable {
                                    haptics.tick()
                                    goalId = gv.goal.id
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

            // 确认
            val enabled = amountCent > 0
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(if (enabled) SuccessGreen else Color(0xFFDCE0E8))
                    .clickable(enabled = enabled) {
                        haptics.success()
                        onConfirm(goalId, amountCent)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (enabled) "记下 ${formatYuan(amountCent)}" else "拨一下再确认",
                    fontSize = Fonts.bodyStrong,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) Color.White else Color(0xFF9AA1AE)
                )
            }

            if (amountCent <= 0) {
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = "金额为 0 时不能记录",
                    fontSize = Fonts.micro,
                    color = Color(0xFFB6BCC8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
