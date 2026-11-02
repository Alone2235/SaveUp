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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eta.saveup.data.repo.GoalView
import com.eta.saveup.ui.UiState
import com.eta.saveup.ui.component.GoalCard
import com.eta.saveup.ui.component.HapticsWrapper
import com.eta.saveup.ui.component.PillTag
import com.eta.saveup.ui.component.ProgressRing
import com.eta.saveup.ui.component.SectionTitle
import com.eta.saveup.ui.component.formatYuanInt
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.GoldAccent
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.IosWhite
import com.eta.saveup.ui.theme.SuccessGreen

@Composable
fun HomeScreen(
    state: UiState,
    haptics: HapticsWrapper,
    onGoalClick: (GoalView) -> Unit,
    onNewGoal: () -> Unit,
    onSettings: () -> Unit
) {
    val primary = state.activeGoals.firstOrNull() ?: state.doneGoals.firstOrNull()

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
        // 顶栏
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "攒钱",
                        fontSize = Fonts.display,
                        fontWeight = FontWeight.Bold,
                        color = IosWhite
                    )
                    Text(
                        text = if (state.streak > 0) "已连续攒钱 ${state.streak} 天"
                        else "今天还没攒，动动手指",
                        fontSize = Fonts.caption,
                        color = IosWhite.copy(alpha = 0.42f)
                    )
                }
                Box(
                    Modifier
                        .size(Dimens.touchTarget)
                        .clip(RoundedCornerShape(Dimens.controlRadius))
                        .background(Color.White)
                        .clickable { haptics.tick(); onSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "设置",
                        tint = IosWhite.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 主视觉：当前目标环
        item {
            if (primary != null) {
                FocusCard(primary, haptics, onClick = { onGoalClick(primary) })
            } else {
                EmptyCard(onNewGoal)
            }
        }

        // 今日进度
        if (state.dailyGoalCent > 0) {
            item { DailyProgressCard(state) }
        }

        // 其它进行中目标
        val others = state.activeGoals.drop(1)
        if (others.isNotEmpty()) {
            item { SectionTitle("其它目标", "${others.size} 个") }
            items(others, key = { it.goal.id }) { gv ->
                GoalCard(gv, onClick = { haptics.tick(); onGoalClick(gv) })
            }
        }

        // 已完成
        if (state.doneGoals.isNotEmpty()) {
            item { SectionTitle("已达成 🎉", "${state.doneGoals.size} 个") }
            items(state.doneGoals, key = { it.goal.id }) { gv ->
                GoalCard(gv, onClick = { haptics.tick(); onGoalClick(gv) })
            }
        }

        item {
            Spacer(Modifier.height(Dimens.gap8))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.cardRadius))
                    .clickable { haptics.tick(); onNewGoal() }
                    .background(HyperBlue.copy(alpha = 0.08f))
                    .padding(Dimens.gap16),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = HyperBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Dimens.gap6))
                Text(
                    text = "新建攒钱目标",
                    fontSize = Fonts.body,
                    fontWeight = FontWeight.Medium,
                    color = HyperBlue
                )
            }
        }
    }
}

@Composable
private fun FocusCard(view: GoalView, haptics: HapticsWrapper, onClick: () -> Unit) {
    val done = view.achieved
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.panelRadius))
            .background(
                if (done) Brush.linearGradient(listOf(Color(0xFFFFF8E6), Color(0xFFFFF2D4)))
                else Brush.linearGradient(listOf(Color.White, Color(0xFFF8FAFF)))
            )
            .clickable { haptics.tick(); onClick() }
            .padding(Dimens.gap24)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(view.goal.emoji, fontSize = Fonts.title1)
                Spacer(Modifier.width(Dimens.gap8))
                Text(
                    text = view.goal.title,
                    fontSize = Fonts.title2,
                    fontWeight = FontWeight.SemiBold,
                    color = IosWhite,
                    maxLines = 1
                )
                if (done) {
                    Spacer(Modifier.width(Dimens.gap8))
                    PillTag("已达成", GoldAccent)
                }
            }
            Spacer(Modifier.height(Dimens.gap16))

            ProgressRing(progress = view.progress) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(view.progress * 100).toInt()}%",
                        fontSize = Fonts.amountHuge,
                        fontWeight = FontWeight.Bold,
                        color = if (done) GoldAccent else HyperBlue
                    )
                    Text(
                        text = if (done) "攒满啦" else "还差 " + formatYuanInt(view.remainCent),
                        fontSize = Fonts.caption,
                        color = IosWhite.copy(alpha = 0.42f)
                    )
                }
            }

            Spacer(Modifier.height(Dimens.gap16))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap24)) {
                StatBlock("已攒", formatYuanInt(view.savedCent), SuccessGreen)
                Box(
                    Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(IosWhite.copy(alpha = 0.08f))
                )
                StatBlock("目标", formatYuanInt(view.goal.targetCent), IosWhite)
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = Fonts.title2,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = Fonts.micro,
            color = IosWhite.copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun DailyProgressCard(state: UiState) {
    val ratio = if (state.dailyGoalCent <= 0) 0f
    else (state.todaySavedCent.toFloat() / state.dailyGoalCent).coerceIn(0f, 1f)
    val reached = state.todaySavedCent >= state.dailyGoalCent

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.cardRadius))
            .background(Color.White)
            .padding(Dimens.cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (reached) SuccessGreen.copy(alpha = 0.14f) else HyperBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (reached) "✓" else "今", fontSize = Fonts.body, color = if (reached) SuccessGreen else HyperBlue)
        }
        Spacer(Modifier.width(Dimens.gap12))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (reached) "今日目标达成" else "今日目标",
                fontSize = Fonts.caption,
                fontWeight = FontWeight.Medium,
                color = IosWhite.copy(alpha = 0.62f)
            )
            Spacer(Modifier.height(Dimens.gap6))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(HyperBlue.copy(alpha = 0.10f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(ratio)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (reached) SuccessGreen else HyperBlue)
                )
            }
        }
        Spacer(Modifier.width(Dimens.gap12))
        Text(
            text = formatYuanInt(state.todaySavedCent) + " / " + formatYuanInt(state.dailyGoalCent),
            fontSize = Fonts.micro,
            color = IosWhite.copy(alpha = 0.42f)
        )
    }
}

@Composable
private fun EmptyCard(onNewGoal: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.panelRadius))
            .background(Color.White)
            .clickable(onClick = onNewGoal)
            .padding(Dimens.gap32),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🐋", fontSize = Fonts.display)
        Spacer(Modifier.height(Dimens.gap12))
        Text(
            text = "还没有目标",
            fontSize = Fonts.title2,
            fontWeight = FontWeight.SemiBold,
            color = IosWhite
        )
        Spacer(Modifier.height(Dimens.gap4))
        Text(
            text = "点这里建一个想买的东西",
            fontSize = Fonts.caption,
            color = IosWhite.copy(alpha = 0.42f)
        )
    }
}
