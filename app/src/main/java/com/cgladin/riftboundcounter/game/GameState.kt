package com.cgladin.riftboundcounter.game

enum class Player { ONE, TWO }

data class GameState(
    val playerOneScore: Int = 0,
    val playerTwoScore: Int = 0,
    val twoPlayers: Boolean = false,
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val timerLimitSeconds: Int = DEFAULT_TIMER_LIMIT_SECONDS,
    val playerOneColorHex: Long = 0xFFA21C60L,
    val playerTwoColorHex: Long = 0xFF3DBE8BL,
) {
    val remainingSeconds: Int get() = timerLimitSeconds - elapsedSeconds
    val isOvertime: Boolean get() = remainingSeconds < 0
    val timerLimitMinutes: Int get() = timerLimitSeconds / 60

    fun scoreOf(player: Player): Int = when (player) {
        Player.ONE -> playerOneScore
        Player.TWO -> playerTwoScore
    }

    fun withScoreChange(player: Player, delta: Int): GameState {
        val newScore = (scoreOf(player).toLong() + delta)
            .coerceIn(0L, MAX_SCORE.toLong())
            .toInt()
        return when (player) {
            Player.ONE -> copy(playerOneScore = newScore)
            Player.TWO -> copy(playerTwoScore = newScore)
        }
    }

    fun withScoresReset(): GameState = copy(playerOneScore = 0, playerTwoScore = 0)

    fun withTick(): GameState = copy(elapsedSeconds = elapsedSeconds + 1)

    fun withTimerReset(): GameState = copy(elapsedSeconds = 0, isTimerRunning = false)

    fun withTimerLimitMinutes(minutes: Int): GameState {
        val seconds = (minutes.toLong().coerceAtLeast(1L) * 60)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        return copy(timerLimitSeconds = seconds)
    }

    fun withPlayerOneColor(colorHex: Long): GameState = copy(playerOneColorHex = colorHex)

    fun withPlayerTwoColor(colorHex: Long): GameState = copy(playerTwoColorHex = colorHex)

    companion object {
        const val MAX_SCORE = 8
        const val DEFAULT_TIMER_LIMIT_SECONDS = 60 * 60
        const val TIMER_LIMIT_SECONDS = DEFAULT_TIMER_LIMIT_SECONDS
    }
}

/** Formate le temps restant en mm:ss, préfixé par "+" une fois la limite dépassée. */
fun formatTimer(remainingSeconds: Int): String {
    val absolute = kotlin.math.abs(remainingSeconds.toLong())
    val sign = if (remainingSeconds < 0) "+" else ""
    return "%s%02d:%02d".format(sign, absolute / 60, absolute % 60)
}
