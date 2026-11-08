package com.eta.saveup.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eta.saveup.data.db.AppDatabase
import com.eta.saveup.data.db.DailyStat
import com.eta.saveup.data.db.Entry
import com.eta.saveup.data.db.Goal
import com.eta.saveup.data.prefs.AppPrefs
import com.eta.saveup.data.repo.GoalView
import com.eta.saveup.data.repo.SaveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val goals: List<GoalView> = emptyList(),
    val entries: List<Entry> = emptyList(),
    val totalSettledCent: Long = 0,
    val streak: Int = 0,
    val todayKeptCent: Long = 0,
    val haptics: Boolean = true
) {
    val activeGoals: List<GoalView> get() = goals.filter { it.goal.status == Goal.ACTIVE }
    val doneGoals: List<GoalView> get() = goals.filter { it.goal.status == Goal.DONE }
    val archivedGoals: List<GoalView> get() = goals.filter { it.goal.status == Goal.ARCHIVED }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val repo = SaveRepository(db)
    private val prefs = AppPrefs(app)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repo.goalViews().collect { g -> _ui.value = _ui.value.copy(goals = g) }
        }
        viewModelScope.launch {
            repo.allEntries.collect { e -> _ui.value = _ui.value.copy(entries = e) }
        }
        viewModelScope.launch {
            repo.totalSettled.collect { s -> _ui.value = _ui.value.copy(totalSettledCent = s) }
        }
        viewModelScope.launch {
            repo.recentDaily.collect { list ->
                val today = SaveRepository.todayKey()
                _ui.value = _ui.value.copy(
                    streak = SaveRepository.streak(list, today),
                    todayKeptCent = list.firstOrNull { it.day == today }?.savedCent ?: 0L
                )
            }
        }
        viewModelScope.launch {
            prefs.hapticsEnabled.collect { h -> _ui.value = _ui.value.copy(haptics = h) }
        }
    }

    // ---- 决定 ----

    fun recordKept(goalId: Long?, amountCent: Long, title: String) =
        viewModelScope.launch { repo.recordDecision(goalId, amountCent, title, kept = true) }

    fun recordBought(goalId: Long?, amountCent: Long, title: String) =
        viewModelScope.launch { repo.recordDecision(goalId, amountCent, title, kept = false) }

    fun deleteEntry(id: Long) = viewModelScope.launch { repo.deleteEntry(id) }

    // ---- 目标 ----

    fun createGoal(
        title: String,
        emoji: String,
        targetCent: Long,
        coverUri: String? = null,
        revealMode: Int = 0
    ) = viewModelScope.launch {
        repo.createGoal(
            Goal(
                title = title,
                emoji = emoji,
                targetCent = targetCent,
                coverUri = coverUri,
                revealMode = revealMode
            )
        )
    }

    fun updateGoal(goal: Goal) = viewModelScope.launch { repo.updateGoal(goal) }

    fun deleteGoal(id: Long) = viewModelScope.launch { repo.deleteGoal(id) }

    fun archiveGoal(goal: Goal) = viewModelScope.launch { repo.archiveGoal(goal) }

    fun restoreGoal(goal: Goal) = viewModelScope.launch { repo.restoreGoal(goal) }

    fun setHaptics(enabled: Boolean) = viewModelScope.launch { prefs.setHaptics(enabled) }
}
