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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eta.saveup.data.db.Goal
import com.eta.saveup.ui.component.AmountWheel
import com.eta.saveup.ui.component.HapticsWrapper
import com.eta.saveup.ui.component.formatYuan
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.IosWhite

private val EMOJI_CHOICES = listOf(
    "🎯", "🎮", "📱", "💻", "🎧", "📷", "🚲", "✈️",
    "👟", "🎸", "📚", "🍰", "🐋", "🌊", "💎", "🎁"
)

/**
 * 新建 / 编辑目标。existing 为 null 时是新建。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GoalEditSheet(
    existing: Goal?,
    haptics: HapticsWrapper,
    onDismiss: () -> Unit,
    onSave: (title: String, emoji: String, targetCent: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "🎯") }
    var targetCent by remember { mutableLongStateOf(existing?.targetCent ?: 100000L) }

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
            Text(
                text = if (existing == null) "新建目标" else "编辑目标",
                fontSize = Fonts.title1,
                fontWeight = FontWeight.Bold,
                color = IosWhite
            )
            Spacer(Modifier.height(Dimens.gap16))

            // 名称输入
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.controlRadius))
                    .background(Color(0xFFEDF0F6))
                    .padding(horizontal = Dimens.gap16, vertical = Dimens.gap16),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = Fonts.title2)
                Spacer(Modifier.width(Dimens.gap12))
                Box(Modifier.weight(1f)) {
                    if (title.isEmpty()) {
                        Text(
                            text = "想买什么？",
                            fontSize = Fonts.body,
                            color = IosWhite.copy(alpha = 0.30f)
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { if (it.length <= 24) title = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = Fonts.body,
                            fontWeight = FontWeight.Medium,
                            color = IosWhite
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(HyperBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(Dimens.gap12))

            // emoji 选择
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.gap8)) {
                items(EMOJI_CHOICES) { e ->
                    val sel = e == emoji
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) HyperBlue.copy(alpha = 0.14f) else Color(0xFFEDF0F6))
                            .clickable {
                                haptics.tick()
                                emoji = e
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(e, fontSize = Fonts.title2)
                    }
                }
            }

            Spacer(Modifier.height(Dimens.gap16))

            Text(
                text = "目标金额",
                fontSize = Fonts.caption,
                fontWeight = FontWeight.Medium,
                color = IosWhite.copy(alpha = 0.42f),
                modifier = Modifier.padding(bottom = Dimens.gap4)
            )

            AmountWheel(
                amountCent = targetCent,
                onAmountChange = { targetCent = it },
                haptics = haptics,
                accent = HyperBlue
            )

            Spacer(Modifier.height(Dimens.gap16))
            Text(
                text = "「" + title.ifBlank { "未命名" } + "」目标 " + formatYuan(targetCent),
                fontSize = Fonts.micro,
                color = IosWhite.copy(alpha = 0.30f),
                modifier = Modifier.padding(horizontal = Dimens.gap4)
            )

            Spacer(Modifier.height(Dimens.gap16))

            val enabled = title.isNotBlank() && targetCent > 0
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonLarge)
                    .clip(RoundedCornerShape(Dimens.pillRadius))
                    .background(if (enabled) HyperBlue else Color(0xFFDCE0E8))
                    .clickable(enabled = enabled) {
                        haptics.success()
                        onSave(title.trim(), emoji, targetCent)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (existing == null) "创建目标" else "保存",
                    fontSize = Fonts.bodyStrong,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) Color.White else Color(0xFF9AA1AE)
                )
            }
        }
    }
}
