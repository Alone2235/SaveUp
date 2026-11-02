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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eta.saveup.ui.UiState
import com.eta.saveup.ui.component.AmountWheel
import com.eta.saveup.ui.component.HapticsWrapper
import com.eta.saveup.ui.component.SectionTitle
import com.eta.saveup.ui.component.formatYuanInt
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.IosWhite
import com.eta.saveup.ui.theme.SuccessGreen

@Composable
fun SettingsScreen(
    state: UiState,
    haptics: HapticsWrapper,
    onBack: () -> Unit,
    onDailyGoalChange: (Long) -> Unit,
    onHapticsChange: (Boolean) -> Unit
) {
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
                Box(
                    Modifier
                        .size(Dimens.touchTarget)
                        .clip(RoundedCornerShape(Dimens.controlRadius))
                        .background(Color.White)
                        .clickable { haptics.tick(); onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = IosWhite.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(Dimens.gap12))
                Text(
                    text = "设置",
                    fontSize = Fonts.title1,
                    fontWeight = FontWeight.Bold,
                    color = IosWhite
                )
            }
        }

        item { SectionTitle("数据总览") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap12)) {
                InfoTile("累计攒下", formatYuanInt(state.totalSavedCent), SuccessGreen, Modifier.weight(1f))
                InfoTile("连续天数", "${state.streak} 天", HyperBlue, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap12)) {
                InfoTile("进行中", "${state.activeGoals.size}", IosWhite, Modifier.weight(1f))
                InfoTile("已达成", "${state.doneGoals.size}", Color(0xFFFFC24B), Modifier.weight(1f))
            }
        }

        item { SectionTitle("每日目标") }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.cardRadius))
                    .background(Color.White)
                    .padding(Dimens.cardPadding)
            ) {
                Text(
                    text = if (state.dailyGoalCent > 0)
                        "现在设定：" + formatYuanInt(state.dailyGoalCent) + " / 天"
                    else "未设定，设为 0 则不显示",
                    fontSize = Fonts.caption,
                    color = IosWhite.copy(alpha = 0.62f),
                    modifier = Modifier.padding(bottom = Dimens.gap8)
                )
                AmountWheel(
                    amountCent = state.dailyGoalCent,
                    onAmountChange = onDailyGoalChange,
                    maxCent = 99_999_999L,
                    haptics = haptics,
                    accent = HyperBlue
                )
            }
        }

        item { SectionTitle("偏好") }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.cardRadius))
                    .background(Color.White)
                    .padding(horizontal = Dimens.itemPadH, vertical = Dimens.gap12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "震动反馈",
                        fontSize = Fonts.body,
                        fontWeight = FontWeight.Medium,
                        color = IosWhite
                    )
                    Text(
                        text = "拨轮和按钮的触感",
                        fontSize = Fonts.micro,
                        color = IosWhite.copy(alpha = 0.38f)
                    )
                }
                Switch(
                    checked = state.haptics,
                    onCheckedChange = {
                        haptics.tick()
                        onHapticsChange(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = HyperBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD5DBE5)
                    )
                )
            }
        }

        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.cardRadius))
                    .background(Color.White)
                    .padding(Dimens.cardPadding)
            ) {
                Column {
                    Text(
                        text = "关于",
                        fontSize = Fonts.caption,
                        fontWeight = FontWeight.Medium,
                        color = IosWhite.copy(alpha = 0.42f)
                    )
                    Spacer(Modifier.height(Dimens.gap4))
                    Text(
                        text = "SaveUp · 攒钱目标管理",
                        fontSize = Fonts.caption,
                        color = IosWhite
                    )
                    Text(
                        text = "所有数据只存在这台手机上，不联网、不上传。",
                        fontSize = Fonts.micro,
                        color = IosWhite.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(Dimens.cardRadius))
            .background(Color.White)
            .padding(Dimens.cardPadding)
    ) {
        Text(
            text = value,
            fontSize = Fonts.title1,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = Fonts.micro,
            color = IosWhite.copy(alpha = 0.38f)
        )
    }
}
