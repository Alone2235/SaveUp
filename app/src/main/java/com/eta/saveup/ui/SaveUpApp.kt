package com.eta.saveup.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eta.saveup.data.db.Goal
import com.eta.saveup.data.repo.GoalView
import com.eta.saveup.ui.component.pressScale
import com.eta.saveup.ui.component.rememberHaptics
import com.eta.saveup.ui.component.rememberInteraction
import com.eta.saveup.ui.screen.DepositSheet
import com.eta.saveup.ui.screen.GoalDetailScreen
import com.eta.saveup.ui.screen.GoalEditSheet
import com.eta.saveup.ui.screen.HomeScreen
import com.eta.saveup.ui.screen.SettingsScreen
import com.eta.saveup.ui.theme.AppBackground
import com.eta.saveup.ui.theme.Dimens
import com.eta.saveup.ui.theme.Fonts
import com.eta.saveup.ui.theme.HyperBlue
import com.eta.saveup.ui.theme.IosWhite
import com.eta.saveup.ui.theme.SuccessGreen

private sealed interface Route {
    data object Home : Route
    data class Detail(val goalId: Long) : Route
    data object Settings : Route
}

/** 用于决定转场方向：列表页 0，二级页 1 */
private fun Route.depth(): Int = when (this) {
    is Route.Home -> 0
    else -> 1
}

@Composable
fun SaveUpApp(vm: MainViewModel = viewModel()) {
    val state by vm.ui.collectAsState()
    val haptics = rememberHaptics(state.haptics)

    var route by remember { mutableStateOf<Route>(Route.Home) }
    var showDeposit by remember { mutableStateOf(false) }
    var showSpend by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf<Goal?>(null) }
    var showNewGoal by remember { mutableStateOf(false) }
    var confirmDeleteGoal by remember { mutableStateOf<Goal?>(null) }
    var deleteDepositId by remember { mutableStateOf<Long?>(null) }

    val sheetGoals = state.activeGoals + state.doneGoals

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        val forward = targetState.depth() >= initialState.depth()
                        val enter = if (forward) {
                            slideInHorizontally(
                                animationSpec = tween(320, easing = FastOutSlowInEasing),
                                initialOffsetX = { it / 6 }
                            ) + fadeIn(tween(220)) + scaleIn(
                                initialScale = 0.96f,
                                animationSpec = tween(320, easing = FastOutSlowInEasing)
                            )
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(320, easing = FastOutSlowInEasing),
                                initialOffsetX = { -it / 6 }
                            ) + fadeIn(tween(220))
                        }
                        val exit = if (forward) {
                            fadeOut(tween(160)) + scaleOut(targetScale = 0.98f, animationSpec = tween(200))
                        } else {
                            fadeOut(tween(160)) + slideOutHorizontally(
                                animationSpec = tween(240, easing = FastOutSlowInEasing),
                                targetOffsetX = { it / 8 }
                            )
                        }
                        enter togetherWith exit
                    },
                    label = "route"
                ) { r ->
                    when (r) {
                        is Route.Home -> HomeScreen(
                            state = state,
                            haptics = haptics,
                            onGoalClick = { gv -> route = Route.Detail(gv.goal.id) },
                            onNewGoal = { showNewGoal = true },
                            onSettings = { route = Route.Settings }
                        )

                        is Route.Settings -> SettingsScreen(
                            state = state,
                            haptics = haptics,
                            onBack = { route = Route.Home },
                            onDailyGoalChange = vm::setDailyGoal,
                            onHapticsChange = vm::setHaptics
                        )

                        is Route.Detail -> {
                            val gv = state.goals.firstOrNull { it.goal.id == r.goalId }
                            val deposits = state.deposits.filter { it.goalId == r.goalId }
                            if (gv == null) {
                                route = Route.Home
                            } else {
                                GoalDetailScreen(
                                    goal = gv.goal,
                                    savedCent = gv.savedCent,
                                    deposits = deposits,
                                    haptics = haptics,
                                    onBack = { route = Route.Home },
                                    onEdit = { editGoal = gv.goal },
                                    onDelete = { confirmDeleteGoal = gv.goal },
                                    onArchive = { vm.archiveGoal(gv.goal) },
                                    onRestore = { vm.restoreGoal(gv.goal) },
                                    onAddDeposit = { showDeposit = true },
                                    onDeleteDeposit = { id -> deleteDepositId = id }
                                )
                            }
                        }
                    }
                }
            }

            if (route == Route.Home) {
                BottomBar(
                    streak = state.streak,
                    haptics = haptics,
                    onKeep = { showDeposit = true },
                    onSpend = { showSpend = true }
                )
            }
        }
    }

    // ---- 弹层 ----
    if (showDeposit) {
        DepositSheet(
            goals = sheetGoals,
            haptics = haptics,
            initialGoalId = (route as? Route.Detail)?.goalId,
            spendMode = false,
            onDismiss = { showDeposit = false },
            onConfirm = { gid, cent ->
                vm.addDeposit(gid, cent)
                showDeposit = false
            }
        )
    }

    if (showSpend) {
        DepositSheet(
            goals = sheetGoals,
            haptics = haptics,
            initialGoalId = (route as? Route.Detail)?.goalId,
            spendMode = true,
            onDismiss = { showSpend = false },
            onConfirm = { gid, cent ->
                vm.addSpent(gid, cent)
                showSpend = false
            }
        )
    }

    if (showNewGoal || editGoal != null) {
        GoalEditSheet(
            existing = editGoal,
            haptics = haptics,
            onDismiss = {
                showNewGoal = false
                editGoal = null
            },
            onSave = { title, emoji, cent ->
                val ex = editGoal
                if (ex == null) {
                    vm.createGoal(title, emoji, cent, null)
                } else {
                    vm.updateGoal(ex.copy(title = title, emoji = emoji, targetCent = cent))
                }
                showNewGoal = false
                editGoal = null
            }
        )
    }

    confirmDeleteGoal?.let { g ->
        AlertDialog(
            onDismissRequest = { confirmDeleteGoal = null },
            title = { Text("删除目标？") },
            text = { Text("「${g.title}」的记录会退回未分配，不会被删掉。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteGoal(g.id)
                    confirmDeleteGoal = null
                    route = Route.Home
                }) { Text("删除", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteGoal = null }) { Text("取消") }
            }
        )
    }

    deleteDepositId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteDepositId = null },
            title = { Text("删除这条记录？") },
            text = { Text("对应金额会从统计里扣回去。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteDeposit(id)
                    deleteDepositId = null
                }) { Text("删除", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteDepositId = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun BottomBar(
    streak: Int,
    haptics: com.eta.saveup.ui.component.HapticsWrapper,
    onKeep: () -> Unit,
    onSpend: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Dimens.pagePadding, vertical = Dimens.gap12),
        horizontalArrangement = Arrangement.spacedBy(Dimens.gap12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 主按钮：我忍住了
        val keepInteraction = rememberInteraction()
        Row(
            Modifier
                .weight(1f)
                .height(Dimens.buttonLarge)
                .pressScale(keepInteraction, pressedScale = 0.96f)
                .clip(RoundedCornerShape(Dimens.pillRadius))
                .background(SuccessGreen)
                .clickable(
                    interactionSource = keepInteraction,
                    indication = null
                ) {
                    haptics.tick()
                    onKeep()
                }
                .padding(horizontal = Dimens.gap24),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(Dimens.gap6))
            Text(
                text = "我忍住了",
                fontSize = Fonts.bodyStrong,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // 次按钮：破戒了
        Box(
            Modifier
                .size(Dimens.buttonLarge)
                .clip(RoundedCornerShape(Dimens.pillRadius))
                .background(Color.White)
                .clickable {
                    haptics.warn()
                    onSpend()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.RemoveCircleOutline,
                contentDescription = "记一笔花销",
                tint = IosWhite.copy(alpha = 0.42f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
