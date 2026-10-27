package com.cgladin.riftboundcounter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

/**
 * Exposes game state to the UI and applies score and timer actions.
 *
 * Selected colors are persisted in the application preferences.
 */
class CounterViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, 0)
    private val _state = MutableStateFlow(
        GameState(
            playerOneColorHex = preferences.getLong(KEY_PLAYER_ONE_COLOR, GameState().playerOneColorHex),
            playerTwoColorHex = preferences.getLong(KEY_PLAYER_TWO_COLOR, GameState().playerTwoColorHex),
        ),
    )
    /** Current game state, exposed read-only to Compose observers. */
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    /** Changes [player]'s score by [delta]. */
    fun onScoreChange(player: Player, delta: Int) =
        _state.update { it.withScoreChange(player, delta) }

    /** Resets both player scores without resetting the timer. */
    fun onResetScores() = _state.update { it.withScoresReset() }

    /** Toggles between one-player and two-player modes. */
    fun onTogglePlayers() = _state.update { it.copy(twoPlayers = !it.twoPlayers) }

    /** Starts the timer if stopped, or pauses it if running. */
    fun onToggleTimer() {
        if (_state.value.isTimerRunning) stopTimer() else startTimer()
    }

    /** Stops the timer and resets elapsed time to zero. */
    fun onResetTimer() {
        tickerJob?.cancel()
        _state.update { it.withTimerReset() }
    }

    /** Updates the timer limit in minutes. */
    fun onUpdateTimerLimit(minutes: Int) =
        _state.update { it.withTimerLimitMinutes(minutes) }

    /** Updates and persists player one's color. */
    fun onUpdatePlayerOneColor(colorHex: Long) {
        preferences.edit().putLong(KEY_PLAYER_ONE_COLOR, colorHex).apply()
        _state.update { it.withPlayerOneColor(colorHex) }
    }

    /** Updates and persists player two's color. */
    fun onUpdatePlayerTwoColor(colorHex: Long) {
        preferences.edit().putLong(KEY_PLAYER_TWO_COLOR, colorHex).apply()
        _state.update { it.withPlayerTwoColor(colorHex) }
    }

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
        const val PREFERENCES_NAME = "counter_settings"
        const val KEY_PLAYER_ONE_COLOR = "player_one_color"
        const val KEY_PLAYER_TWO_COLOR = "player_two_color"
    }
}
