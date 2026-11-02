package com.eta.saveup.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eta.saveup.data.db.AppDatabase
import com.eta.saveup.data.db.DailyStat
import com.eta.saveup.data.db.Deposit
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
    val deposits: List<Deposit> = emptyList(),
    val totalSavedCent: Long = 0,
    val inboxCount: Int = 0,
    val streak: Int = 0,
    val todaySavedCent: Long = 0,
    val dailyGoalCent: Long = 0,
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

    private val _recentDaily = MutableStateFlow<List<DailyStat>>(emptyList())

    init {
        viewModelScope.launch {
            repo.goalViews().collect { goals ->
                _ui.value = _ui.value.copy(goals = goals)
            }
        }
        viewModelScope.launch {
            repo.allDeposits.collect { d -> _ui.value = _ui.value.copy(deposits = d) }
        }
        viewModelScope.launch {
            repo.totalSaved.collect { s -> _ui.value = _ui.value.copy(totalSavedCent = s) }
        }
        viewModelScope.launch {
            repo.inboxCount.collect { c -> _ui.value = _ui.value.copy(inboxCount = c) }
        }
        viewModelScope.launch {
            repo.recentDaily.collect { list ->
                val today = SaveRepository.todayKey()
                _ui.value = _ui.value.copy(
                    streak = SaveRepository.streak(list, today),
                    todaySavedCent = list.firstOrNull { it.day == today }?.savedCent ?: 0L
                )
            }
        }
        viewModelScope.launch {
            prefs.dailyGoalCent.collect { g -> _ui.value = _ui.value.copy(dailyGoalCent = g) }
        }
        viewModelScope.launch {
            prefs.hapticsEnabled.collect { h -> _ui.value = _ui.value.copy(haptics = h) }
        }
    }

    fun addDeposit(goalId: Long?, amountCent: Long, note: String = "") =
        viewModelScope.launch { repo.addDeposit(goalId, amountCent, note) }

    fun addSpent(goalId: Long?, amountCent: Long, note: String = "") =
        viewModelScope.launch { repo.addSpent(goalId, amountCent, note) }

    fun deleteDeposit(id: Long) = viewModelScope.launch { repo.deleteDeposit(id) }

    fun moveDeposit(id: Long, goalId: Long?) = viewModelScope.launch { repo.moveDeposit(id, goalId) }

    fun createGoal(
        title: String,
        emoji: String,
        targetCent: Long,
        deadline: Long?,
        coverUri: String? = null,
        revealMode: Int = 0
    ) = viewModelScope.launch {
        repo.createGoal(
            Goal(
                title = title,
                emoji = emoji,
                targetCent = targetCent,
                deadlineMillis = deadline,
                coverUri = coverUri,
                revealMode = revealMode
            )
        )
    }

    fun updateGoal(goal: Goal) = viewModelScope.launch { repo.updateGoal(goal) }

    fun deleteGoal(id: Long) = viewModelScope.launch { repo.deleteGoal(id) }

    fun archiveGoal(goal: Goal) = viewModelScope.launch { repo.archiveGoal(goal) }

    fun restoreGoal(goal: Goal) = viewModelScope.launch { repo.restoreGoal(goal) }

    fun setDailyGoal(cent: Long) = viewModelScope.launch { prefs.setDailyGoal(cent) }

    fun setHaptics(enabled: Boolean) = viewModelScope.launch { prefs.setHaptics(enabled) }

}
