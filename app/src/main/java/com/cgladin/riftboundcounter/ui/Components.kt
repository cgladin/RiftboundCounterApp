package com.cgladin.riftboundcounter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cgladin.riftboundcounter.R
import com.cgladin.riftboundcounter.game.GameState
import com.cgladin.riftboundcounter.game.formatTimer
import kotlin.random.Random

internal object Palette {
    val Background = Color(0xFF14121A)
    val OnBackground = Color.White
    val PlayerOne = Color(0xFF3DBE8B)
    val PlayerTwo = Color(0xFFA21C60)
    val Overtime = Color(0xFFE53935)
    val OnPlayer = Color(0xFF1B1B1B)
    val ButtonOverlay = Color(0x33000000)

    fun onPlayer(color: Color): Color =
        if (color.luminance() < 0.179f) Color.White else OnPlayer

    val ColorPresets = listOf(
        0xFF3DBE8BL, // Vert
        0xFFA21C60L, // Rose
        0xFF3D8BEBL, // Bleu
        0xFFE53935L, // Rouge
        0xFFFF9800L, // Orange
        0xFF009688L, // Teal
        0xFF8E24AAL, // Violet
        0xFFFFC107L, // Jaune
    )
}

@Composable
fun TimerBar(
    remainingSeconds: Int,
    isOvertime: Boolean,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            formatTimer(remainingSeconds),
            color = if (isOvertime) Palette.Overtime else Palette.OnBackground,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 16.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onToggle) {
                Text(stringResource(if (isRunning) R.string.timer_pause else R.string.timer_start))
            }
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            ) { Text(stringResource(R.string.timer_reset)) }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = Palette.OnBackground,
                )
            }
        }
    }
}

@Composable
fun PlayerPanel(
    score: Int,
    color: Color,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val decreaseDescription = stringResource(R.string.decrease_score)
    val increaseDescription = stringResource(R.string.increase_score)
    val isVictory = score >= GameState.MAX_SCORE
    val onPlayerColor = Palette.onPlayer(color)

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(color),
    ) {
        if (isVictory) {
            ConfettiEffect()
        }

        Row(Modifier.fillMaxSize()) {
            // Côté gauche : diminue le score en appuyant n'importe où sur la moitié gauche
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        enabled = score > 0,
                        onClick = { onChange(-1) },
                    )
                    .semantics { contentDescription = decreaseDescription },
                contentAlignment = Alignment.CenterStart,
            ) {
                RoundSymbol(
                    label = "−",
                    enabled = score > 0,
                    contentColor = onPlayerColor,
                    modifier = Modifier.padding(start = 24.dp),
                )
            }

            // Côté droit : augmente le score en appuyant n'importe où sur la moitié droite
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        enabled = score < GameState.MAX_SCORE,
                        onClick = { onChange(1) },
                    )
                    .semantics { contentDescription = increaseDescription },
                contentAlignment = Alignment.CenterEnd,
            ) {
                RoundSymbol(
                    label = "+",
                    enabled = score < GameState.MAX_SCORE,
                    contentColor = onPlayerColor,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        }

        // Score centré par-dessus et badge de victoire
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isVictory) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.ButtonOverlay)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(
                        stringResource(R.string.victory).uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = onPlayerColor,
                        letterSpacing = 2.sp,
                    )
                }
            }
            Text(
                "$score",
                fontSize = if (isVictory) 100.sp else 120.sp,
                fontWeight = FontWeight.Bold,
                color = onPlayerColor,
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val yOffset: Float,
    val size: Float,
    val color: Color,
    val speedY: Float,
    val speedX: Float,
    val swaySpeed: Float,
    val rotationSpeed: Float,
    val isCircle: Boolean,
)

private val ConfettiColors = listOf(
    Color(0xFFFFD700), // Gold
    Color(0xFFFF4081), // Pink
    Color(0xFF00E676), // Green
    Color(0xFF00B0FF), // Blue
    Color(0xFFFF9100), // Orange
    Color(0xFFE040FB), // Purple
    Color(0xFF00E5FF), // Cyan
    Color(0xFFFFFFFF), // White
)

private val TimerPresetMinutes = listOf(15, 30, 45, 60)

private const val CONFETTI_DURATION_SECONDS = 3f

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 45,
) {
    val particles = remember(particleCount) {
        List(particleCount) {
            ConfettiParticle(
                x = Random.nextFloat(),
                yOffset = Random.nextFloat() * 1.5f,
                size = Random.nextFloat() * 12f + 8f,
                color = ConfettiColors.random(),
                speedY = Random.nextFloat() * 0.35f + 0.2f,
                speedX = Random.nextFloat() * 0.12f + 0.04f,
                swaySpeed = Random.nextFloat() * 3.5f + 1.2f,
                rotationSpeed = Random.nextFloat() * 360f + 180f,
                isCircle = Random.nextBoolean(),
            )
        }
    }

    var timeSeconds by remember { mutableFloatStateOf(0f) }
    var isAnimating by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        var lastTime = withFrameNanos { it }
        var elapsedSeconds = 0f
        while (elapsedSeconds < CONFETTI_DURATION_SECONDS) {
            withFrameNanos { frameTime ->
                val dt = (frameTime - lastTime) / 1_000_000_000f
                lastTime = frameTime
                elapsedSeconds += dt
                timeSeconds = elapsedSeconds
            }
        }
        isAnimating = false
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (isAnimating) {
            val w = size.width
            val h = size.height

            particles.forEach { p ->
                val currentY = ((p.yOffset + timeSeconds * p.speedY) % 1.3f) - 0.15f
                val pxY = currentY * h

                val currentX = p.x + kotlin.math.sin(timeSeconds * p.swaySpeed) * p.speedX
                val pxX = ((currentX % 1f + 1f) % 1f) * w

                val rotation = timeSeconds * p.rotationSpeed

                withTransform({
                    translate(left = pxX, top = pxY)
                    rotate(degrees = rotation)
                }) {
                    if (p.isCircle) {
                        drawCircle(
                            color = p.color,
                            radius = p.size / 2,
                        )
                    } else {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(-p.size / 2, -p.size / 4),
                            size = Size(p.size, p.size / 2),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundSymbol(
    label: String,
    enabled: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Palette.ButtonOverlay)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor.copy(alpha = if (enabled) 1f else 0.3f),
        )
    }
}

@Composable
fun SettingsDialog(
    timerLimitMinutes: Int,
    playerOneColorHex: Long,
    playerTwoColorHex: Long,
    onTimerLimitChange: (Int) -> Unit,
    onPlayerOneColorChange: (Long) -> Unit,
    onPlayerTwoColorChange: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.settings_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Section 1: Durée du timer
                Text(
                    stringResource(R.string.timer_duration),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(
                        onClick = { onTimerLimitChange((timerLimitMinutes - 5).coerceAtLeast(1)) },
                        enabled = timerLimitMinutes > 5,
                    ) {
                        Text("−", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "$timerLimitMinutes min",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = { onTimerLimitChange(timerLimitMinutes + 5) },
                    ) {
                        Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TimerPresetMinutes.forEach { minutes ->
                        val isSelected = timerLimitMinutes == minutes
                        Button(
                            onClick = { onTimerLimitChange(minutes) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(playerOneColorHex) else Color.DarkGray,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("${minutes}m", fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider()

                // Section 2: Couleur du Joueur 1
                Text(
                    stringResource(R.string.player_one_color),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
                ColorPickerRow(
                    selectedColorHex = playerOneColorHex,
                    onColorSelect = onPlayerOneColorChange,
                )

                HorizontalDivider()

                // Section 3: Couleur du Joueur 2
                Text(
                    stringResource(R.string.player_two_color),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
                ColorPickerRow(
                    selectedColorHex = playerTwoColorHex,
                    onColorSelect = onPlayerTwoColorChange,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun ColorPickerRow(
    selectedColorHex: Long,
    onColorSelect: (Long) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Palette.ColorPresets.forEach { colorHex ->
            val isSelected = colorHex == selectedColorHex
            val color = Color(colorHex)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, Color.White, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onColorSelect(colorHex) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Text("✓", color = Palette.OnPlayer, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}