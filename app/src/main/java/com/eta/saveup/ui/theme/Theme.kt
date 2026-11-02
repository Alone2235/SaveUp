package com.eta.saveup.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// HyperOS 澎湃白蓝配色，与同账号其他项目一致
val HyperBlue = Color(0xFF4A8CFF)
val SuccessGreen = Color(0xFF2FBF8F)
val WarnOrange = Color(0xFFFF9F45)
val DangerRed = Color(0xFFFF6B6B)
val GoldAccent = Color(0xFFFFC24B)
val IosGray = Color(0xFF878D99)
val IosBg = Color(0xFFF2F4F8)
val IosElevated = Color(0xFFFFFFFF)
val IosWhite = Color(0xFF1B1F27)

val GlassTint = Color(0x33FFFFFF)
val GlassStroke = Color(0x59FFFFFF)

private val LightColors = lightColorScheme(
    primary = HyperBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9FF),
    onPrimaryContainer = Color(0xFF00224D),
    secondary = SuccessGreen,
    onSecondary = Color.White,
    background = IosBg,
    onBackground = IosWhite,
    surface = IosElevated,
    onSurface = IosWhite,
    surfaceVariant = Color(0xFFE8ECF4),
    onSurfaceVariant = IosGray,
    error = DangerRed,
    outline = Color(0xFFD5DBE5)
)

private val DarkColors = darkColorScheme(
    primary = HyperBlue,
    onPrimary = Color.White,
    background = Color(0xFF14161B),
    onBackground = Color(0xFFEDEFF4),
    surface = Color(0xFF1C1F26),
    onSurface = Color(0xFFEDEFF4),
    surfaceVariant = Color(0xFF262A33),
    onSurfaceVariant = Color(0xFF9AA1AE),
    error = DangerRed,
    outline = Color(0xFF333844)
)

@Composable
fun SaveUpTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors) {
        content()
    }
}

/** 背景：极简白蓝，单层竖渐变 + 极弱径向蓝光 */
@Composable
fun AppBackground(dark: Boolean = false, content: @Composable () -> Unit) {
    val gradient = if (dark) listOf(
        Color(0xFF101218), Color(0xFF151820), Color(0xFF1A1E27)
    ) else listOf(
        Color(0xFFFDFDFE), Color(0xFFF6F7FA), Color(0xFFF0F2F7)
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradient))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(HyperBlue.copy(alpha = if (dark) 0.10f else 0.05f), Color.Transparent),
                        center = Offset(0.5f, 0.1f),
                        radius = 1200f
                    )
                )
        )
        content()
    }
}
