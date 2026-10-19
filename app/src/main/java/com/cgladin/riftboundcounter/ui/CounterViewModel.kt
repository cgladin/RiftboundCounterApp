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
 * Expose l’état de la partie à l’interface et applique les actions sur les scores et le chronomètre.
 *
 * Les couleurs choisies sont persistées dans les préférences de l’application.
 */
class CounterViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, 0)
    private val _state = MutableStateFlow(
        GameState(
            playerOneColorHex = preferences.getLong(KEY_PLAYER_ONE_COLOR, GameState().playerOneColorHex),
            playerTwoColorHex = preferences.getLong(KEY_PLAYER_TWO_COLOR, GameState().playerTwoColorHex),
        ),
    )
    /** État courant de la partie, exposé en lecture seule aux observateurs Compose. */
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    /** Modifie le score de [player] selon [delta]. */
    fun onScoreChange(player: Player, delta: Int) =
        _state.update { it.withScoreChange(player, delta) }

    /** Remet les scores des joueurs à zéro sans réinitialiser le chronomètre. */
    fun onResetScores() = _state.update { it.withScoresReset() }

    /** Bascule entre les modes un joueur et deux joueurs. */
    fun onTogglePlayers() = _state.update { it.copy(twoPlayers = !it.twoPlayers) }

    /** Démarre le chronomètre s’il est arrêté, ou le met en pause s’il tourne. */
    fun onToggleTimer() {
        if (_state.value.isTimerRunning) stopTimer() else startTimer()
    }

    /** Arrête le chronomètre et remet le temps écoulé à zéro. */
    fun onResetTimer() {
        tickerJob?.cancel()
        _state.update { it.withTimerReset() }
    }

    /** Met à jour la limite du chronomètre en minutes. */
    fun onUpdateTimerLimit(minutes: Int) =
        _state.update { it.withTimerLimitMinutes(minutes) }

    /** Met à jour et persiste la couleur du joueur 1. */
    fun onUpdatePlayerOneColor(colorHex: Long) {
        preferences.edit().putLong(KEY_PLAYER_ONE_COLOR, colorHex).apply()
        _state.update { it.withPlayerOneColor(colorHex) }
    }

    /** Met à jour et persiste la couleur du joueur 2. */
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
