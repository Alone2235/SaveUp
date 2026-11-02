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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.eta.saveup.data.repo.GoalView
import com.eta.saveup.ui.component.AmountWheel
import com.eta.saveup.ui.component.HapticsWrapper
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.SuccessGreen

/**
 * 记一笔攒钱 / 记一笔花销 的底部弹层。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DepositSheet(
    goals: List<GoalView>,
    haptics: HapticsWrapper,
    initialGoalId: Long? = null,
    spendMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (goalId: Long?, amountCent: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountCent by remember { mutableLongStateOf(0L) }
    var goalId by remember { mutableStateOf(initialGoalId) }

    val accent = if (spendMode) Color(0xFFFF6B6B) else SuccessGreen

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFBFCFE)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.pagePadding)
                .padding(bottom = Dimens.gap24)
        ) {
            // 标题
            Text(
                text = if (spendMode) "破戒了，记一笔" else "我又忍住了",
                fontSize = Fonts.title1,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1F27)
            )
            Spacer(Modifier.height(Dimens.gap4))
            Text(
                text = if (spendMode) "这笔钱从目标里扣掉" else "把钱喂给哪张目标卡？",
                fontSize = Fonts.caption,
                color = Color(0xFF878D99)
            )

            Spacer(Modifier.height(Dimens.gap16))

            // 金额录入（顶部大字可点，直接键盘输入）
            AmountWheel(
                amountCent = amountCent,
                onAmountChange = { amountCent = it },
                haptics = haptics,
                accent = accent
            )

            Spacer(Modifier.height(Dimens.gap12))

            // 目标选择
            if (goals.isNotEmpty()) {
                Text(
                    text = "选择目标",
                    fontSize = Fonts.caption,
                    color = Color(0xFF878D99),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = Dimens.gap4, bottom = Dimens.gap8)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.gap8)) {
                    items(goals) { gv ->
                        val selected = goalId == gv.goal.id
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) HyperBlue else Color(0xFFEDF0F6))
                                .clickable {
                                    haptics.tick()
                                    goalId = if (selected) null else gv.goal.id
                                }
                                .padding(horizontal = Dimens.gap12, vertical = Dimens.gap8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(gv.goal.emoji, fontSize = Fonts.body)
                            Spacer(Modifier.width(Dimens.gap4))
                            Text(
                                text = gv.goal.title,
                                fontSize = Fonts.caption,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) Color.White else Color(0xFF1B1F27)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Dimens.gap24))

            // 确认按钮
            val enabled = amountCent > 0
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonLarge)
                    .clip(RoundedCornerShape(Dimens.pillRadius))
                    .background(if (enabled) accent else Color(0xFFDCE0E8))
                    .clickable(enabled = enabled) {
                        haptics.success()
                        onConfirm(goalId, amountCent)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = if (enabled) Color.White else Color(0xFF9AA1AE),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(Dimens.gap8))
                    Text(
                        text = if (spendMode) "确认花掉" else "确认攒下",
                        fontSize = Fonts.bodyStrong,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) Color.White else Color(0xFF9AA1AE)
                    )
                }
            }
        }
    }
}
