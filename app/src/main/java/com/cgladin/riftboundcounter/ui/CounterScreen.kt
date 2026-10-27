package com.cgladin.riftboundcounter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

/** Bouton central de bascule points/dé, affiché avec un fondu et un zoom en mode deux joueurs. */
@Composable
private fun DiceToggleButton(
    visible: Boolean,
    diceMode: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(450)) + scaleIn(tween(450)),
        exit = fadeOut(tween(300)) + scaleOut(tween(300)),
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent, contentColor = Palette.OnPlayer),
        ) {
            Text(stringResource(if (diceMode) R.string.points_mode else R.string.dice_mode))
        }
    }
}

/** Carte à deux faces : [front] puis [back] après un retournement animé autour de l’axe vertical. */
@Composable
private fun FlipCard(
    flipped: Boolean,
    modifier: Modifier,
    front: @Composable (Modifier) -> Unit,
    back: @Composable (Modifier) -> Unit,
) {
    val angle by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(600),
        label = "cardFlip",
    )
    val showBack = angle > 90f
    Box(
        modifier.graphicsLayer {
            rotationY = angle
            cameraDistance = 12f * density
        },
    ) {
        if (showBack) {
            // Contre-rotation pour que la face arrière ne soit pas en miroir.
            back(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f })
        } else {
            front(Modifier.fillMaxSize())
        }
    }
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
            Column(Modifier.fillMaxSize()) {
                val secondPanelFraction by animateFloatAsState(
                    targetValue = if (state.twoPlayers) 1f else 0f,
                    animationSpec = tween(450),
                    label = "secondPanel",
                )
                if (secondPanelFraction > 0f) {
                    FlipCard(
                        flipped = diceMode,
                        modifier = Modifier
                            .weight(secondPanelFraction.coerceAtLeast(0.001f))
                            .fillMaxWidth()
                            .padding(bottom = 12.dp * secondPanelFraction)
                            .graphicsLayer { alpha = secondPanelFraction }
                            .rotate(180f),
                        front = { m ->
                            PlayerPanel(
                                score = state.playerTwoScore,
                                color = Color(state.playerTwoColorHex),
                                onChange = { onScoreChange(Player.TWO, it) },
                                modifier = m,
                            )
                        },
                        back = { m ->
                            DicePanel(
                                result = diceTwo,
                                color = Color(state.playerTwoColorHex),
                                onResult = { diceTwo = it },
                                modifier = m,
                            )
                        },
                    )
                }
                FlipCard(
                    flipped = diceMode,
                    modifier = Modifier.weight(1f),
                    front = { m ->
                        PlayerPanel(
                            score = state.playerOneScore,
                            color = Color(state.playerOneColorHex),
                            onChange = { onScoreChange(Player.ONE, it) },
                            modifier = m,
                        )
                    },
                    back = { m ->
                        DicePanel(
                            result = diceOne,
                            color = Color(state.playerOneColorHex),
                            onResult = { diceOne = it },
                            modifier = m,
                        )
                    },
                )
            }
            DiceToggleButton(
                visible = state.twoPlayers,
                diceMode = diceMode,
                modifier = Modifier.align(Alignment.Center),
                onClick = {
                    diceMode = !diceMode
                    diceOne = null
                    diceTwo = null
                },
            )
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
