package com.cgladin.riftboundcounter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
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

/** Collecte l’état du ViewModel et le connecte à l’écran du compteur. */
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

/**
 * Affiche le chronomètre, les panneaux de score et les commandes de la partie.
 *
 * @param state état courant de la partie.
 * @param onScoreChange callback d’ajustement du score d’un joueur.
 * @param onTogglePlayers callback de bascule entre un et deux joueurs.
 * @param onResetScores callback de remise à zéro des scores.
 * @param onToggleTimer callback de démarrage ou de pause du chronomètre.
 * @param onResetTimer callback de remise à zéro du chronomètre.
 * @param onUpdateTimerLimit callback de modification de la durée du chronomètre, en minutes.
 * @param onUpdatePlayerOneColor callback de modification de la couleur du joueur 1.
 * @param onUpdatePlayerTwoColor callback de modification de la couleur du joueur 2.
 */
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
    var diceMode by rememberSaveable { mutableStateOf(false) }
    var diceOne by rememberSaveable { mutableStateOf<Int?>(null) }
    var diceTwo by rememberSaveable { mutableStateOf<Int?>(null) }
    // Le mode dé n'a de sens qu'à deux joueurs.
    if (!state.twoPlayers && diceMode) diceMode = false

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
        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.twoPlayers) {
                    val panelModifier = Modifier
                        .weight(1f)
                        .rotate(180f)
                    if (diceMode) {
                        DicePanel(
                            result = diceTwo,
                            color = Color(state.playerTwoColorHex),
                            onResult = { diceTwo = it },
                            modifier = panelModifier,
                        )
                    } else {
                        PlayerPanel(
                            score = state.playerTwoScore,
                            color = Color(state.playerTwoColorHex),
                            onChange = { onScoreChange(Player.TWO, it) },
                            modifier = panelModifier,
                        )
                    }
                }
                if (diceMode) {
                    DicePanel(
                        result = diceOne,
                        color = Color(state.playerOneColorHex),
                        onResult = { diceOne = it },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    PlayerPanel(
                        score = state.playerOneScore,
                        color = Color(state.playerOneColorHex),
                        onChange = { onScoreChange(Player.ONE, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (state.twoPlayers) {
                Button(
                    onClick = {
                        diceMode = !diceMode
                        diceOne = null
                        diceTwo = null
                    },
                    modifier = Modifier.align(Alignment.Center),
                ) { Text(stringResource(R.string.dice_mode)) }
            }
        }

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
