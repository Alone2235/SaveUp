package com.eta.saveup.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eta.saveup.ui.theme.HyperBlue

object RevealMode {
    const val BOTTOM_UP = 0
    const val LEFT_RIGHT = 1
    const val RADIAL = 2
}

/**
 * 解锁式封面：一张图，进度决定彩色揭示的范围。
 *
 * 实现方式刻意避开 BlendMode —— AsyncImage 自绘内容，
 * 在外层 drawWithContent 里做 DstIn/Offscreen 容易被图层吞掉，导致"一拨就全亮"。
 *
 * 改为：彩色图放在一个尺寸受进度控制的裁剪容器里，
 * 容器根据方向改变高度 / 宽度 / 缩放，天然按比例，不会越界。
 *
 * 灰度层用 setToSaturation(0) + 亮度压暗，确保未解锁部分明显发灰。
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
        animationSpec = tween(800),
        label = "unlock"
    )

    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color(0xFF20242C))
    ) {
        // ---- 底层：灰度 + 压暗，这就是"没解锁"的样子 ----
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.colorMatrix(
                // 去饱和 + 提高对比 + 压暗，灰得很明显
                ColorMatrix().apply {
                    setToSaturation(0f)
                    // 亮度压低到 0.62
                    val b = 0.62f
                    this[0, 0] *= b; this[1, 1] *= b; this[2, 2] *= b
                }
            )
        )

        // 灰层上再盖一层深色，进一步压暗
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
        )

        // ---- 上层：彩色图，按方向裁剪 ----
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w: Dp = maxWidth
            val h: Dp = maxHeight

            when (revealMode) {
                RevealMode.LEFT_RIGHT -> {
                    // 从左往右展开
                    Box(
                        Modifier
                            .fillMaxWidth(animated)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(cornerRadius.dp))
                    ) {
                        AsyncImage(
                            model = model,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(w, h)
                                .align(Alignment.CenterStart)
                        )
                    }
                    // 推进边缘亮线
                    if (animated > 0.01f && animated < 0.99f) {
                        Box(
                            Modifier
                                .offset(x = w * animated - 1.dp)
                                .width(2.dp)
                                .height(h)
                                .background(accent.copy(alpha = 0.85f))
                        )
                    }
                }

                RevealMode.RADIAL -> {
                    // 从中心向外扩散：用整体缩放模拟，半径与进度线性对应
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(cornerRadius.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = model,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(w, h)
                                .graphicsLayer {
                                    // 半径按 sqrt 增长更符合视觉上的"面积"直觉
                                    val s = animated.coerceIn(0f, 1f)
                                    scaleX = s
                                    scaleY = s
                                    alpha = if (s <= 0.001f) 0f else 1f
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.62f)
                                }
                                .clip(RoundedCornerShape(percent = (animated * 50).toInt()))
                        )
                    }
                }

                else -> {
                    // 自下而上：高度按进度增长
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(h * animated)
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(cornerRadius.dp))
                    ) {
                        AsyncImage(
                            model = model,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(w, h)
                                .offset(y = -(h - h * animated))
                        )
                    }
                    // 推进边缘亮线
                    if (animated > 0.01f && animated < 0.99f) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = -(h * animated) + 1.dp)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(accent.copy(alpha = 0.85f))
                        )
                    }
                }
            }
        }
    }
}
