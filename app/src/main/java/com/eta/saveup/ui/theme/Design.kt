package com.eta.saveup.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 间距系统：严格 4pt 网格。
 * 所有 padding / spacing / size 都从这里取，不再出现 14dp、18dp 这种游离值。
 */
object Space {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    /** 页面左右边距 */
    val page = 20.dp
    /** 卡片内边距 */
    val card = 16.dp
    /** 卡片之间 */
    val gap = 12.dp
    /** 分组之间 */
    val section = 28.dp
}

/** 圆角系统：三档，不再出现 10/12/14/18/24/28/32 混用 */
object Radius {
    /** 小控件：标签、小按钮、数字格 */
    val small = 10.dp
    /** 卡片、列表项 */
    val medium = 16.dp
    /** 大面板、底部弹层 */
    val large = 24.dp
    /** 胶囊 */
    val pill = 999.dp
}

/** 字号：保留原有层级，行高统一 */
object Fonts {
    val display = 32.sp
    val title1 = 24.sp
    val title2 = 18.sp
    val bodyStrong = 16.sp
    val body = 15.sp
    val caption = 13.sp
    val micro = 11.sp

    val amountHuge = 52.sp
    val amountWheel = 26.sp
}

/** 文本透明度分级 */
object TextAlpha {
    const val primary = 1f
    const val secondary = 0.58f
    const val tertiary = 0.38f
    const val disabled = 0.24f
}

/** 兼容旧引用 */
object Dimens {
    val controlRadius = Radius.small
    val cardRadius = Radius.medium
    val panelRadius = Radius.large
    val sheetRadius = Radius.large
    val pillRadius = Radius.pill

    val unit = Space.xs
    val pagePadding = Space.page
    val cardPadding = Space.card
    val itemPadH = Space.md
    val itemPadV = Space.sm
    val gap4 = Space.xxs
    val gap6 = 6.dp
    val gap8 = Space.xs
    val gap12 = Space.sm
    val gap16 = Space.md
    val gap24 = Space.lg
    val gap32 = Space.xl
    val gap48 = Space.xxl

    val rowHeight = 72.dp
    val touchTarget = 48.dp
    val inputHeight = 52.dp
    val buttonLarge = 56.dp
    val tabBarHeight = 64.dp

    val wheelItem = 56.dp
}
