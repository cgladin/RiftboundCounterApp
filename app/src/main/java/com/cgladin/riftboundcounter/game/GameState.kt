package com.cgladin.riftboundcounter.game

/** Identifies one of the two players whose score can be changed. */
enum class Player { ONE, TWO }

/**
 * Immutable game state containing scores, the timer, and visual preferences.
 *
 * Scores are clamped to [MAX_SCORE] when changed with [withScoreChange].
 */
data class GameState(
    val playerOneScore: Int = 0,
    val playerTwoScore: Int = 0,
    val twoPlayers: Boolean = false,
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val timerLimitSeconds: Int = DEFAULT_TIMER_LIMIT_SECONDS,
    val playerOneColorHex: Long = 0xFFEF7D00L,
    val playerTwoColorHex: Long = 0xFF003A51L,
) {
    /** Seconds remaining before the limit; becomes negative during overtime. */
    val remainingSeconds: Int get() = timerLimitSeconds - elapsedSeconds

    /** Whether the elapsed time has exceeded the configured limit. */
    val isOvertime: Boolean get() = remainingSeconds < 0

    /** Configured timer duration, expressed as whole minutes. */
    val timerLimitMinutes: Int get() = timerLimitSeconds / 60

    /** Returns the score for the specified [player]. */
    fun scoreOf(player: Player): Int = when (player) {
        Player.ONE -> playerOneScore
        Player.TWO -> playerTwoScore
    }

    /**
     * Returns a new state with the player's score adjusted and clamped between zero and [MAX_SCORE].
     *
     * @param player player whose score should change.
     * @param delta points to add (or remove if negative).
     */
    fun withScoreChange(player: Player, delta: Int): GameState {
        val newScore = (scoreOf(player).toLong() + delta)
            .coerceIn(0L, MAX_SCORE.toLong())
            .toInt()
        return when (player) {
            Player.ONE -> copy(playerOneScore = newScore)
            Player.TWO -> copy(playerTwoScore = newScore)
        }
    }

    /** Returns a new state with both scores reset to zero, without changing the timer. */
    fun withScoresReset(): GameState = copy(playerOneScore = 0, playerTwoScore = 0)

    /** Returns a new state with one additional second elapsed. */
    fun withTick(): GameState = copy(elapsedSeconds = elapsedSeconds + 1)

    /** Resets elapsed time to zero and stops the timer. */
    fun withTimerReset(): GameState = copy(elapsedSeconds = 0, isTimerRunning = false)

    /**
     * Returns a new state with the timer duration updated.
     *
     * Values below one minute are raised to one minute, and values that are too large are capped
     * so they remain representable in seconds.
     *
     * @param minutes desired duration in minutes.
     */
    fun withTimerLimitMinutes(minutes: Int): GameState {
        val seconds = (minutes.toLong().coerceAtLeast(1L) * 60)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        return copy(timerLimitSeconds = seconds)
    }

    /** Returns a new state with player one's color updated. */
    fun withPlayerOneColor(colorHex: Long): GameState = copy(playerOneColorHex = colorHex)

    /** Returns a new state with player two's color updated. */
    fun withPlayerTwoColor(colorHex: Long): GameState = copy(playerTwoColorHex = colorHex)

    companion object {
        /** Maximum player score, which triggers a victory. */
        const val MAX_SCORE = 8
        /** Initial timer duration, in seconds. */
        const val DEFAULT_TIMER_LIMIT_SECONDS = 60 * 60
        /** Compatibility alias for the initial timer duration, in seconds. */
        const val TIMER_LIMIT_SECONDS = DEFAULT_TIMER_LIMIT_SECONDS
    }
}

/**
 * Formats the time as `mm:ss` and prefixes overtime with `+`.
 *
 * @param remainingSeconds seconds remaining; a negative value represents overtime.
 * @return formatted time, for example `12:34` or `+00:12`.
 */
fun formatTimer(remainingSeconds: Int): String {
    val absolute = kotlin.math.abs(remainingSeconds.toLong())
    val sign = if (remainingSeconds < 0) "+" else ""
    val minutes = (absolute / 60).toString().padStart(2, '0')
    val seconds = (absolute % 60).toString().padStart(2, '0')
    return "$sign$minutes:$seconds"
}
