package com.eta.saveup.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eta.saveup.ui.theme.HyperBlue

/** 揭示方向 */
object RevealMode {
    const val BOTTOM_UP = 0
    const val LEFT_RIGHT = 1
    const val RADIAL = 2
}

/**
 * 解锁式封面：一张图，攒钱进度决定"彩色"的比例。
 *
 * 实现：两层同一张图叠放。
 * - 底层：去饱和 + 压暗，代表"未解锁"
 * - 上层：原图，用 drawWithContent 裁出一块按进度扩张的揭示区域
 *   边缘加一条品牌色细亮边，像刀锋扫过
 *
 * progress = 0 全灰，1 全彩。满了之后整图恢复饱和。
 */
@Composable
fun UnlockCover(
    model: Any?,
    progress: Float,
    modifier: Modifier = Modifier,
    revealMode: Int = RevealMode.BOTTOM_UP,
    accent: Color = HyperBlue,
    cornerRadius: Int = 20
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "unlock"
    )

    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color(0xFF1A1D24))
    ) {
        // 底层：灰度 + 压暗
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.55f },
            colorFilter = ColorFilter.colorMatrix(
                ColorMatrix().apply { setToSaturation(0f) }
            )
        )

        // 上层：原图，按进度揭示
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    val p = animated
                    if (p <= 0.001f) return@drawWithContent

                    val w = size.width
                    val h = size.height

                    when (revealMode) {
                        RevealMode.LEFT_RIGHT -> {
                            drawContent()
                            // 遮罩：只保留左侧 p 比例
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(w * p, 0f),
                                size = Size(w * (1f - p), h),
                                blendMode = BlendMode.DstIn
                            )
                            // 揭示边缘亮线
                            if (p < 0.999f) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            accent.copy(alpha = 0f),
                                            accent.copy(alpha = 0.9f),
                                            accent.copy(alpha = 0f)
                                        )
                                    ),
                                    topLeft = Offset(w * p - 1f, 0f),
                                    size = Size(2.dp.toPx(), h)
                                )
                            }
                        }

                        RevealMode.RADIAL -> {
                            drawContent()
                            drawCircle(
                                color = Color.Black,
                                radius = (w.coerceAtMost(h) * p) * 1.15f,
                                center = Offset(w / 2f, h * 0.62f),
                                blendMode = BlendMode.DstOut
                            )
                            if (p < 0.999f) {
                                drawCircle(
                                    color = accent.copy(alpha = 0.7f),
                                    radius = w.coerceAtMost(h) * p * 1.15f,
                                    center = Offset(w / 2f, h * 0.62f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                                )
                            }
                        }

                        else -> {
                            // BOTTOM_UP：从底部往上长
                            drawContent()
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(0f, 0f),
                                size = Size(w, h * (1f - p)),
                                blendMode = BlendMode.DstIn
                            )
                            if (p < 0.999f) {
                                val y = h * (1f - p)
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            accent.copy(alpha = 0f),
                                            accent.copy(alpha = 0.9f),
                                            accent.copy(alpha = 0f)
                                        )
                                    ),
                                    topLeft = Offset(0f, y - 1f),
                                    size = Size(w, 2.dp.toPx())
                                )
                            }
                        }
                    }
                }
        )
    }
}
