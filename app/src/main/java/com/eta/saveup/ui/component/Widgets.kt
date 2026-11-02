package com.eta.saveup.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.IosWhite

/** 分 → "1,234.56"，不带符号 */
fun formatAmount(cent: Long): String {
    val neg = cent < 0
    val abs = kotlin.math.abs(cent)
    val yuan = abs / 100
    val fen = abs % 100
    val yuanStr = yuan.toString().reversed().chunked(3).joinToString(",").reversed()
    return (if (neg) "-" else "") + yuanStr + "." + fen.toString().padStart(2, '0')
}

/** 分 → "¥1,234.56" */
fun formatYuan(cent: Long): String = "¥" + formatAmount(cent)

/** 只保留整数部分，用于大字显示 */
fun formatYuanInt(cent: Long): String {
    val yuan = kotlin.math.abs(cent) / 100
    val s = yuan.toString().reversed().chunked(3).joinToString(",").reversed()
    return "¥" + s
}

/** 状态小圆点 */
@Composable
fun StatusDot(color: Color = HyperBlue, diameter: Int = 8) {
    Box(
        Modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/** 实心/描边胶囊标签 */
@Composable
fun PillTag(
    text: String,
    color: Color = HyperBlue,
    filled: Boolean = true
) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .then(if (filled) Modifier.background(color) else Modifier.background(color.copy(alpha = 0.12f)))
            .padding(horizontal = Dimens.gap8, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = if (filled) Color.White else color,
            fontSize = Fonts.micro,
            fontWeight = FontWeight.Medium
        )
    }
}

/** 分组标题 */
@Composable
fun SectionTitle(text: String, trailing: String? = null) {
    Row(
        Modifier.padding(horizontal = Dimens.gap4, vertical = Dimens.gap8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = Fonts.caption,
            fontWeight = FontWeight.SemiBold,
            color = IosWhite.copy(alpha = 0.42f)
        )
        Spacer(Modifier.weight(1f))
        trailing?.let {
            Text(text = it, fontSize = Fonts.micro, color = IosWhite.copy(alpha = 0.32f))
        }
            ?: Spacer(Modifier.width(0.dp))
    }
}
