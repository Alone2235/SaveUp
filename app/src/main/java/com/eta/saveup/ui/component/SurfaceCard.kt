package com.eta.saveup.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eta.saveup.ui.theme.Radius
import com.eta.saveup.ui.theme.Space

/** 卡片统一外观：白底 + 极淡描边 + 极轻投影，替代纯白平铺 */
object CardStyle {
    val background = Color(0xFFFFFFFF)
    val stroke = Color(0x0F1B1F27)      // 约 6% 黑
    val shadow = Color(0x141B1F27)      // 约 8% 黑
    val subtle = Color(0xFFF7F9FC)      // 次级底
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    radius: Dp = Radius.medium,
    padding: PaddingValues = PaddingValues(Space.card),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(radius),
                ambientColor = CardStyle.shadow,
                spotColor = CardStyle.shadow
            )
            .clip(RoundedCornerShape(radius))
            .background(CardStyle.background)
            .border(1.dp, CardStyle.stroke, RoundedCornerShape(radius))
            .padding(padding),
        content = content
    )
}

/** 强调卡：带品牌色渐变底，用于主目标 */
@Composable
fun AccentCard(
    accent: Color,
    modifier: Modifier = Modifier,
    radius: Dp = Radius.large,
    padding: PaddingValues = PaddingValues(Space.lg),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .shadow(2.dp, RoundedCornerShape(radius), ambientColor = accent.copy(alpha = 0.18f), spotColor = accent.copy(alpha = 0.18f))
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White,
                        accent.copy(alpha = 0.04f)
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = 0.10f), RoundedCornerShape(radius))
            .padding(padding),
        content = content
    )
}
