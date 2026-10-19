package com.cgladin.riftboundcounter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgladin.riftboundcounter.game.GameState
import com.cgladin.riftboundcounter.game.Player
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CounterViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    fun onScoreChange(player: Player, delta: Int) =
        _state.update { it.withScoreChange(player, delta) }

    fun onResetScores() = _state.update { it.withScoresReset() }

    fun onTogglePlayers() = _state.update { it.copy(twoPlayers = !it.twoPlayers) }

    fun onToggleTimer() {
        if (_state.value.isTimerRunning) stopTimer() else startTimer()
    }

    fun onResetTimer() {
        tickerJob?.cancel()
        _state.update { it.withTimerReset() }
    }

    fun onUpdateTimerLimit(minutes: Int) =
        _state.update { it.withTimerLimitMinutes(minutes) }

    fun onUpdatePlayerOneColor(colorHex: Long) =
        _state.update { it.withPlayerOneColor(colorHex) }

    fun onUpdatePlayerTwoColor(colorHex: Long) =
        _state.update { it.withPlayerTwoColor(colorHex) }

    private fun startTimer() {
        _state.update { it.copy(isTimerRunning = true) }
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MILLIS)
                _state.update { it.withTick() }
            }
        }
    }

    private fun stopTimer() {
        tickerJob?.cancel()
        _state.update { it.copy(isTimerRunning = false) }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}
