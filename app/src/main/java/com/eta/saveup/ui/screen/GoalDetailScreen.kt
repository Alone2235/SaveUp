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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eta.saveup.data.db.Entry
import com.eta.saveup.data.db.Goal
import com.eta.saveup.ui.component.HapticsWrapper
import com.eta.saveup.ui.component.ProgressRing
import com.eta.saveup.ui.component.SectionTitle
import com.eta.saveup.ui.component.SurfaceCard
import com.eta.saveup.ui.component.UnlockCover
import com.eta.saveup.ui.component.formatPercent
import com.eta.saveup.ui.component.formatYuan
import com.eta.saveup.ui.component.formatYuanInt
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.GoldAccent
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.IosWhite
import com.eta.saveup.ui.theme.Radius
import com.eta.saveup.ui.theme.Space
import com.eta.saveup.ui.theme.SuccessGreen
import com.eta.saveup.ui.theme.TextAlpha
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dayFmt = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)

@Composable
fun GoalDetailScreen(
    goal: Goal,
    savedCent: Long,
    entries: List<Entry>,
    haptics: HapticsWrapper,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDecide: () -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    val progress = if (goal.targetCent <= 0) 0f
    else (savedCent.toFloat() / goal.targetCent).coerceIn(0f, 1f)
    val done = savedCent >= goal.targetCent

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Space.page,
            end = Space.page,
            top = Space.md,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Space.gap)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Rounded.ArrowBack, "返回") {
                    haptics.tick(); onBack()
                }
                Spacer(Modifier.width(Space.sm))
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

        // 图 + 进度
        item {
            SurfaceCard(padding = PaddingValues(Space.lg)) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (goal.coverUri != null) {
                        UnlockCover(
                            model = goal.coverUri,
                            progress = progress,
                            revealMode = goal.revealMode,
                            cornerRadius = 16,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                        )
                        Spacer(Modifier.height(Space.md))
                    } else {
                        Text(goal.emoji, fontSize = Fonts.display)
                        Spacer(Modifier.height(Space.sm))
                        ProgressRing(progress = progress, diameter = 160.dp, strokeWidth = 13.dp) {
                            Text(
                                text = formatPercent(progress),
                                fontSize = Fonts.amountHuge,
                                fontWeight = FontWeight.Bold,
                                color = if (done) GoldAccent else HyperBlue
                            )
                        }
                        Spacer(Modifier.height(Space.md))
                    }

                    Text(
                        text = formatYuanInt(savedCent) + " / " + formatYuanInt(goal.targetCent),
                        fontSize = Fonts.title2,
                        fontWeight = FontWeight.SemiBold,
                        color = IosWhite
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (done) "已经攒满，可以下手了"
                        else "还差 " + formatYuanInt(goal.targetCent - savedCent),
                        fontSize = Fonts.caption,
                        color = IosWhite.copy(alpha = TextAlpha.tertiary)
                    )
                }
            }
        }

        // 操作
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                ActionButton("想买点什么", HyperBlue, Modifier.weight(1f)) {
                    haptics.tick(); onDecide()
                }
                if (goal.status == Goal.ARCHIVED) {
                    ActionButton("恢复", SuccessGreen, Modifier.weight(1f)) {
                        haptics.tick(); onRestore()
                    }
                } else if (!done) {
                    ActionButton(
                        "先藏起来",
                        Color(0xFFEDF0F6),
                        Modifier.weight(1f),
                        textColor = IosWhite
                    ) { haptics.tick(); onArchive() }
                }
                RoundIconButton(Icons.Rounded.Delete, "删除", tint = Color(0xFFFF6B6B)) {
                    haptics.warn(); onDelete()
                }
            }
        }

        // 记录
        if (entries.isEmpty()) {
            item {
                SurfaceCard(padding = PaddingValues(Space.xl)) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = IosWhite.copy(alpha = 0.20f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            text = "还没有记录",
                            fontSize = Fonts.caption,
                            color = IosWhite.copy(alpha = TextAlpha.tertiary)
                        )
                    }
                }
            }
        } else {
            item { SectionTitle("记录", "${entries.size} 笔") }
            items(entries, key = { it.id }) { e ->
                EntryRow(e, onLongPress = {
                    haptics.warn()
                    onDeleteEntry(e.id)
                })
            }
        }
    }
}

/** 一条决定记录 */
@Composable
private fun EntryRow(e: Entry, onLongPress: () -> Unit) {
    val kept = e.decision == Entry.KEPT
    val settled = e.settleState == Entry.SETTLED

    SurfaceCard(
        padding = PaddingValues(Space.sm),
        modifier = Modifier.longPress(onLongPress)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (kept) SuccessGreen else Color(0xFFFF9F45))
            )
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = e.title.ifBlank { if (kept) "留着" else "买了" },
                    fontSize = Fonts.body,
                    fontWeight = FontWeight.Medium,
                    color = IosWhite,
                    maxLines = 1
                )
                Text(
                    text = buildString {
                        append(dayFmt.format(Date(e.createdAt)))
                        if (kept) append(if (settled) " · 已转入" else " · 待转入")
                    },
                    fontSize = Fonts.micro,
                    color = IosWhite.copy(alpha = TextAlpha.tertiary)
                )
            }
            Text(
                text = (if (kept) "+" else "-") + formatYuan(e.amountCent),
                fontSize = Fonts.bodyStrong,
                fontWeight = FontWeight.SemiBold,
                color = if (kept) {
                    if (settled) SuccessGreen else SuccessGreen.copy(alpha = 0.55f)
                } else {
                    Color(0xFFFF9F45)
                }
            )
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
            .size(44.dp)
            .clip(RoundedCornerShape(Radius.small))
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
            .height(44.dp)
            .clip(RoundedCornerShape(Radius.small))
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun Modifier.longPress(action: () -> Unit): Modifier =
    this.combinedClickable(onClick = {}, onLongClick = action)
