package com.eta.saveup.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eta.saveup.data.db.Goal
import com.eta.saveup.data.repo.GoalView
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.GoldAccent
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.IosWhite
import com.eta.saveup.ui.theme.SuccessGreen

/** 目标卡片 —— 主列表里的一行 */
@Composable
fun GoalCard(
    view: GoalView,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val done = view.achieved
    val accent = when {
        done -> GoldAccent
        view.goal.status == Goal.ARCHIVED -> IosWhite.copy(alpha = 0.30f)
        else -> HyperBlue
    }
    val progress by animateFloatAsState(view.progress, label = "p")

    val interaction = rememberInteraction()

    Box(
        modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(RoundedCornerShape(Dimens.cardRadius))
            .background(Color.White)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(Dimens.cardPadding)
    ) {
        // 完成后右侧淡淡的金色底光
        if (done) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, GoldAccent.copy(alpha = 0.14f))
                        )
                    )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 有解锁图就用缩略图，否则用 emoji
            if (view.goal.coverUri != null) {
                UnlockCover(
                    model = view.goal.coverUri,
                    progress = view.progress,
                    revealMode = view.goal.revealMode,
                    accent = accent,
                    cornerRadius = 12,
                    modifier = Modifier.size(48.dp)
                )
            } else {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(view.goal.emoji, fontSize = Fonts.title2)
            }
            }

            Spacer(Modifier.width(Dimens.gap12))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = view.goal.title,
                        fontSize = Fonts.bodyStrong,
                        fontWeight = FontWeight.SemiBold,
                        color = if (view.goal.status == Goal.ARCHIVED)
                            IosWhite.copy(alpha = 0.42f) else IosWhite,
                        maxLines = 1
                    )
                    if (done) {
                        Spacer(Modifier.width(Dimens.gap6))
                        PillTag(text = "已达成", color = GoldAccent)
                    }
                }
                Spacer(Modifier.height(Dimens.gap6))
                // 细进度条
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.10f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accent, accent.copy(alpha = 0.7f))
                                )
                            )
                    )
                }
                Spacer(Modifier.height(Dimens.gap6))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatYuanInt(view.savedCent),
                        fontSize = Fonts.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                    Text(
                        text = " / " + formatYuanInt(view.goal.targetCent),
                        fontSize = Fonts.caption,
                        color = IosWhite.copy(alpha = 0.38f)
                    )
                }
            }

            Spacer(Modifier.width(Dimens.gap12))

            // 右侧百分比
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${(view.progress * 100).toInt()}%",
                    fontSize = Fonts.title2,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (done) "毕业" else "还差" + formatYuanInt(view.remainCent).removePrefix("¥"),
                    fontSize = Fonts.micro,
                    color = IosWhite.copy(alpha = 0.38f)
                )
            }
        }
    }
}

/** 流水/记录一行 */
@Composable
fun AmountRow(
    title: String,
    subtitle: String,
    amountCent: Long,
    spent: Boolean,
    onLongPress: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.controlRadius))
            .background(Color.White)
            .combinedLongPress(onLongPress)
            .padding(horizontal = Dimens.itemPadH, vertical = Dimens.itemPadV),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(if (spent) Color(0xFFFF6B6B) else SuccessGreen)
        )
        Spacer(Modifier.width(Dimens.gap12))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = Fonts.body,
                fontWeight = FontWeight.Medium,
                color = IosWhite,
                maxLines = 1
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    fontSize = Fonts.micro,
                    color = IosWhite.copy(alpha = 0.38f),
                    maxLines = 1
                )
            }
        }
        Text(
            text = (if (spent) "-" else "+") + formatYuan(amountCent),
            fontSize = Fonts.bodyStrong,
            fontWeight = FontWeight.SemiBold,
            color = if (spent) Color(0xFFFF6B6B) else SuccessGreen
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun Modifier.combinedLongPress(onLongPress: () -> Unit): Modifier =
    this.combinedClickable(
        onClick = {},
        onLongClick = onLongPress
    )
