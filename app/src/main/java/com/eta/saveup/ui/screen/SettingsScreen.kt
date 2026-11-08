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
import com.eta.saveup.ui.component.HapticsWrapper
import com.eta.saveup.ui.component.SectionTitle
import com.eta.saveup.ui.component.SurfaceCard
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
import com.eta.saveup.ui.theme.WarnOrange

@Composable
fun SettingsScreen(
    state: UiState,
    haptics: HapticsWrapper,
    onBack: () -> Unit,
    onHapticsChange: (Boolean) -> Unit
) {
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
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(Radius.small))
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
                Spacer(Modifier.width(Space.sm))
                Text(
                    text = "设置",
                    fontSize = Fonts.title1,
                    fontWeight = FontWeight.Bold,
                    color = IosWhite
                )
            }
        }

        item { SectionTitle("总览") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                InfoTile("累计攒下", formatYuanInt(state.totalSettledCent), SuccessGreen, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                InfoTile("进行中", "${state.activeGoals.size}", IosWhite, Modifier.weight(1f))
                InfoTile("已达成", "${state.doneGoals.size}", GoldAccent, Modifier.weight(1f))
            }
        }
        item { SectionTitle("偏好") }
        item {
            SurfaceCard(padding = PaddingValues(Space.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "震动反馈",
                            fontSize = Fonts.body,
                            fontWeight = FontWeight.Medium,
                            color = IosWhite
                        )
                        Text(
                            text = "按钮和滚轮的触感",
                            fontSize = Fonts.micro,
                            color = IosWhite.copy(alpha = TextAlpha.tertiary)
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
        }

        item {
            SurfaceCard {
                Column {
                    Text(
                        text = "关于",
                        fontSize = Fonts.caption,
                        fontWeight = FontWeight.Medium,
                        color = IosWhite.copy(alpha = TextAlpha.secondary)
                    )
                    Spacer(Modifier.height(Space.xxs))
                    Text(
                        text = "SaveUp · 攒钱目标管理",
                        fontSize = Fonts.caption,
                        color = IosWhite
                    )
                    Text(
                        text = "所有数据只存在这台手机上，不联网、不上传。",
                        fontSize = Fonts.micro,
                        color = IosWhite.copy(alpha = TextAlpha.tertiary)
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
            .clip(RoundedCornerShape(Radius.medium))
            .background(Color.White)
            .padding(Space.md)
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
            color = IosWhite.copy(alpha = TextAlpha.tertiary)
        )
    }
}
