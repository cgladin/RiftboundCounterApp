package com.cgladin.riftboundcounter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgladin.riftboundcounter.R
import com.cgladin.riftboundcounter.game.GameState
import com.cgladin.riftboundcounter.game.Player

@Composable
fun CounterRoute(viewModel: CounterViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CounterScreen(
        state = state,
        onScoreChange = viewModel::onScoreChange,
        onTogglePlayers = viewModel::onTogglePlayers,
        onResetScores = viewModel::onResetScores,
        onToggleTimer = viewModel::onToggleTimer,
        onResetTimer = viewModel::onResetTimer,
        onUpdateTimerLimit = viewModel::onUpdateTimerLimit,
        onUpdatePlayerOneColor = viewModel::onUpdatePlayerOneColor,
        onUpdatePlayerTwoColor = viewModel::onUpdatePlayerTwoColor,
    )
}

@Composable
fun CounterScreen(
    state: GameState,
    onScoreChange: (Player, Int) -> Unit,
    onTogglePlayers: () -> Unit,
    onResetScores: () -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onUpdateTimerLimit: (Int) -> Unit,
    onUpdatePlayerOneColor: (Long) -> Unit,
    onUpdatePlayerTwoColor: (Long) -> Unit,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TimerBar(
            remainingSeconds = state.remainingSeconds,
            isOvertime = state.isOvertime,
            isRunning = state.isTimerRunning,
            onToggle = onToggleTimer,
            onReset = onResetTimer,
            onOpenSettings = { showSettings = true },
        )

        // Le joueur 2 est retourné à 180° pour faire face à l'adversaire.
        if (state.twoPlayers) {
            PlayerPanel(
                score = state.playerTwoScore,
                color = Color(state.playerTwoColorHex),
                onChange = { onScoreChange(Player.TWO, it) },
                modifier = Modifier
                    .weight(1f)
                    .rotate(180f),
            )
        }
        PlayerPanel(
            score = state.playerOneScore,
            color = Color(state.playerOneColorHex),
            onChange = { onScoreChange(Player.ONE, it) },
            modifier = Modifier.weight(1f),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onTogglePlayers) {
                Text(
                    stringResource(if (state.twoPlayers) R.string.one_player else R.string.two_players),
                    color = Palette.OnBackground,
                )
            }
            TextButton(onClick = onResetScores) {
                Text(stringResource(R.string.reset_scores), color = Palette.OnBackground)
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            timerLimitMinutes = state.timerLimitMinutes,
            playerOneColorHex = state.playerOneColorHex,
            playerTwoColorHex = state.playerTwoColorHex,
            onTimerLimitChange = onUpdateTimerLimit,
            onPlayerOneColorChange = onUpdatePlayerOneColor,
            onPlayerTwoColorChange = onUpdatePlayerTwoColor,
            onDismiss = { showSettings = false },
        )
    }
}
