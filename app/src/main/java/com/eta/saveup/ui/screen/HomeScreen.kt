package com.eta.saveup.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eta.saveup.data.repo.GoalView
import com.eta.saveup.ui.UiState
import com.eta.saveup.ui.component.AccentCard
import com.eta.saveup.ui.component.CardStyle
import com.eta.saveup.ui.component.GoalCard
import com.eta.saveup.ui.component.HapticsWrapper
import com.eta.saveup.ui.component.PillTag
import com.eta.saveup.ui.component.ProgressRing
import com.eta.saveup.ui.component.SectionTitle
import com.eta.saveup.ui.component.SurfaceCard
import com.eta.saveup.ui.component.UnlockCover
import com.eta.saveup.ui.component.formatPercent
import com.eta.saveup.ui.component.formatYuan
import com.eta.saveup.ui.component.formatYuanInt
import com.eta.saveup.ui.component.pressScale
import com.eta.saveup.ui.component.rememberInteraction
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.GoldAccent
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.IosWhite
import com.eta.saveup.ui.theme.Radius
import com.eta.saveup.ui.theme.Space
import com.eta.saveup.ui.theme.SuccessGreen
import com.eta.saveup.ui.theme.TextAlpha
import com.eta.saveup.ui.theme.WarnOrange

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
            start = Space.page,
            end = Space.page,
            top = Space.md,
            bottom = 160.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Space.gap)
    ) {
        // ---- 顶栏：不显示余额，只显示连续天数 ----
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = Space.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "攒钱",
                        fontSize = Fonts.display,
                        fontWeight = FontWeight.Bold,
                        color = IosWhite
                    )
                    Spacer(Modifier.height(Space.xxs))
                    Text(
                        text = if (state.streak > 0) "连续 ${state.streak} 天守住了"
                        else "想买点什么？记一笔试试",
                        fontSize = Fonts.caption,
                        color = IosWhite.copy(alpha = TextAlpha.tertiary)
                    )
                }
                val settingInteraction = rememberInteraction()
                Box(
                    Modifier
                        .size(44.dp)
                        .pressScale(settingInteraction, 0.92f)
                        .clip(RoundedCornerShape(Radius.small))
                        .background(CardStyle.background)
                        .clickable(
                            interactionSource = settingInteraction,
                            indication = null
                        ) { haptics.tick(); onSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "设置",
                        tint = IosWhite.copy(alpha = TextAlpha.secondary),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ---- 主目标 ----
        item {
            if (primary != null) {
                FocusCard(primary, haptics, onClick = { onGoalClick(primary) })
            } else {
                EmptyCard(onNewGoal)
            }
        }

        // ---- 其它目标 ----
        val others = state.activeGoals.drop(1)
        if (others.isNotEmpty()) {
            item { SectionTitle("其它目标", "${others.size}") }
            items(others, key = { it.goal.id }) { gv ->
                GoalCard(gv, onClick = { haptics.tick(); onGoalClick(gv) })
            }
        }

        // ---- 已完成 ----
        if (state.doneGoals.isNotEmpty()) {
            item { SectionTitle("已达成", "${state.doneGoals.size}") }
            items(state.doneGoals, key = { it.goal.id }) { gv ->
                GoalCard(gv, onClick = { haptics.tick(); onGoalClick(gv) })
            }
        }

        // ---- 新建 ----
        item {
            Spacer(Modifier.height(Space.xs))
            NewGoalButton(onNewGoal, haptics)
        }
    }
}

@Composable
private fun FocusCard(view: GoalView, haptics: HapticsWrapper, onClick: () -> Unit) {
    val done = view.achieved
    val accent = if (done) GoldAccent else HyperBlue
    val interaction = rememberInteraction()

    AccentCard(accent = accent, modifier = Modifier.pressScale(interaction, 0.98f)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaction,
                    indication = null
                ) { haptics.tick(); onClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(view.goal.emoji, fontSize = Fonts.title2)
                Spacer(Modifier.width(Space.xs))
                Text(
                    text = view.goal.title,
                    fontSize = Fonts.title2,
                    fontWeight = FontWeight.SemiBold,
                    color = IosWhite,
                    maxLines = 1
                )
                if (done) {
                    Spacer(Modifier.width(Space.xs))
                    PillTag("已达成", GoldAccent)
                }
            }

            Spacer(Modifier.height(Space.lg))

            if (view.goal.coverUri != null) {
                Box {
                    UnlockCover(
                        model = view.goal.coverUri,
                        progress = view.progress,
                        revealMode = view.goal.revealMode,
                        accent = accent,
                        cornerRadius = 18,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(Space.sm)
                            .clip(RoundedCornerShape(Radius.small))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = Space.xs, vertical = Space.xxs)
                    ) {
                        val percentAnim by animateFloatAsState(
                            targetValue = view.progress * 100f,
                            animationSpec = tween(650),
                            label = "percent"
                        )
                        Text(
                            text = formatPercent(percentAnim / 100f),
                            fontSize = Fonts.title2,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (done) "已解锁全部" else "还差 " + formatYuanInt(view.remainCent),
                            fontSize = Fonts.micro,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            } else {
                ProgressRing(progress = view.progress, diameter = 156.dp, strokeWidth = 13.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val percentAnim by animateFloatAsState(
                            targetValue = view.progress * 100f,
                            animationSpec = tween(650),
                            label = "percent"
                        )
                        Text(
                            text = formatPercent(percentAnim / 100f),
                            fontSize = Fonts.amountHuge,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Text(
                            text = if (done) "攒满啦" else "还差 " + formatYuanInt(view.remainCent),
                            fontSize = Fonts.caption,
                            color = IosWhite.copy(alpha = TextAlpha.tertiary)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.lg))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                StatCell(
                    label = "已攒",
                    value = formatYuanInt(view.savedCent),
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = "目标",
                    value = formatYuanInt(view.goal.targetCent),
                    color = IosWhite,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(Radius.small))
            .background(CardStyle.subtle)
            .padding(vertical = Space.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = Fonts.title2,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = Fonts.micro,
            color = IosWhite.copy(alpha = TextAlpha.tertiary)
        )
    }
}

@Composable
private fun NewGoalButton(onClick: () -> Unit, haptics: HapticsWrapper) {
    val interaction = rememberInteraction()
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.98f)
            .clip(RoundedCornerShape(Radius.medium))
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { haptics.tick(); onClick() }
            .padding(vertical = Space.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = null,
            tint = HyperBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(Space.xs))
        Text(
            text = "新建攒钱目标",
            fontSize = Fonts.body,
            fontWeight = FontWeight.Medium,
            color = HyperBlue
        )
    }
}

@Composable
private fun EmptyCard(onNewGoal: () -> Unit) {
    val interaction = rememberInteraction()
    SurfaceCard(modifier = Modifier.pressScale(interaction, 0.98f)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onNewGoal
                )
                .padding(vertical = Space.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🐋", fontSize = Fonts.display)
            Spacer(Modifier.height(Space.sm))
            Text(
                text = "还没有目标",
                fontSize = Fonts.title2,
                fontWeight = FontWeight.SemiBold,
                color = IosWhite
            )
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = "点这里建一个想买的东西",
                fontSize = Fonts.caption,
                color = IosWhite.copy(alpha = TextAlpha.tertiary)
            )
        }
    }
}
