package com.eta.saveup.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eta.saveup.data.db.Deposit
import com.eta.saveup.data.db.Goal
import com.eta.saveup.data.repo.GoalView
import com.eta.saveup.ui.component.AmountRow
import com.eta.saveup.ui.component.HapticsWrapper
import com.eta.saveup.ui.component.ProgressRing
import com.eta.saveup.ui.component.SectionTitle
import com.eta.saveup.ui.component.formatYuanInt
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.GoldAccent
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.IosWhite
import com.eta.saveup.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dayFmt = SimpleDateFormat("M月d日", Locale.CHINA)
private val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)

@Composable
fun GoalDetailScreen(
    goal: Goal,
    savedCent: Long,
    deposits: List<Deposit>,
    haptics: HapticsWrapper,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onAddDeposit: () -> Unit,
    onDeleteDeposit: (Long) -> Unit
) {
    val progress = if (goal.targetCent <= 0) 0f
    else (savedCent.toFloat() / goal.targetCent).coerceIn(0f, 1f)
    val done = savedCent >= goal.targetCent

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.pagePadding,
            end = Dimens.pagePadding,
            top = Dimens.gap16,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.gap12)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Rounded.ArrowBack, "返回") {
                    haptics.tick(); onBack()
                }
                Spacer(Modifier.width(Dimens.gap12))
                Text(
                    text = goal.title,
                    fontSize = Fonts.title1,
                    fontWeight = FontWeight.Bold,
                    color = IosWhite,
                    modifier = Modifier.weight(1f)
                )
                RoundIconButton(Icons.Rounded.Edit, "编辑") {
                    haptics.tick(); onEdit()
                }
            }
        }

        // 大环
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.panelRadius))
                    .background(Color.White)
                    .padding(Dimens.gap24),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(goal.emoji, fontSize = Fonts.display)
                    Spacer(Modifier.height(Dimens.gap12))
                    ProgressRing(progress = progress, diameter = 180.dp, strokeWidth = 14.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = Fonts.amountHuge,
                                fontWeight = FontWeight.Bold,
                                color = if (done) GoldAccent else HyperBlue
                            )
                        }
                    }
                    Spacer(Modifier.height(Dimens.gap16))
                    Text(
                        text = formatYuanInt(savedCent) + " / " + formatYuanInt(goal.targetCent),
                        fontSize = Fonts.title2,
                        fontWeight = FontWeight.SemiBold,
                        color = IosWhite
                    )
                    Spacer(Modifier.height(Dimens.gap4))
                    Text(
                        text = if (done) "已经攒满，可以下手了" else "还差 " + formatYuanInt(goal.targetCent - savedCent),
                        fontSize = Fonts.caption,
                        color = IosWhite.copy(alpha = 0.42f)
                    )
                }
            }
        }

        // 操作
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap12)) {
                ActionButton("记一笔", HyperBlue, Modifier.weight(1f), onClick = {
                    haptics.tick(); onAddDeposit()
                })
                if (goal.status == Goal.ARCHIVED) {
                    ActionButton("恢复", SuccessGreen, Modifier.weight(1f), onClick = {
                        haptics.tick(); onRestore()
                    })
                } else if (!done) {
                    ActionButton("先藏起来", IosWhite.copy(alpha = 0.10f), Modifier.weight(1f), textColor = IosWhite, onClick = {
                        haptics.tick(); onArchive()
                    })
                }
                RoundIconButton(
                    Icons.Rounded.Delete,
                    "删除",
                    tint = Color(0xFFFF6B6B)
                ) {
                    haptics.warn(); onDelete()
                }
            }
        }

        // 流水
        if (deposits.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.cardRadius))
                        .background(Color.White)
                        .padding(Dimens.gap32),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = IosWhite.copy(alpha = 0.20f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(Dimens.gap8))
                        Text(
                            text = "还没有记录",
                            fontSize = Fonts.caption,
                            color = IosWhite.copy(alpha = 0.42f)
                        )
                    }
                }
            }
        } else {
            item { SectionTitle("记录", "${deposits.size} 笔") }
            items(deposits, key = { it.id }) { d ->
                val spent = d.status == Deposit.SPENT
                AmountRow(
                    title = if (d.note.isNotBlank()) d.note else if (spent) "花掉了" else "忍住没买",
                    subtitle = dayFmt.format(Date(d.createdAt)) + " " + timeFmt.format(Date(d.createdAt)),
                    amountCent = d.amountCent,
                    spent = spent,
                    onLongPress = {
                        haptics.warn()
                        onDeleteDeposit(d.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: Color = IosWhite.copy(alpha = 0.6f),
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(Dimens.touchTarget)
            .clip(RoundedCornerShape(Dimens.controlRadius))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ActionButton(
    text: String,
    bg: Color,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(Dimens.touchTarget)
            .clip(RoundedCornerShape(Dimens.controlRadius))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = Fonts.body,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
