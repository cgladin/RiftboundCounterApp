package com.cgladin.riftboundcounter.game

/** Identifie l’un des deux joueurs dont le score peut être modifié. */
enum class Player { ONE, TWO }

/**
 * État immuable d’une partie, comprenant les scores, le chronomètre et les préférences visuelles.
 *
 * Les scores sont bornés par [MAX_SCORE] lorsqu’ils sont modifiés avec [withScoreChange].
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
    /** Nombre de secondes avant la limite ; devient négatif pendant le temps supplémentaire. */
    val remainingSeconds: Int get() = timerLimitSeconds - elapsedSeconds

    /** Indique si le temps écoulé a dépassé la limite définie. */
    val isOvertime: Boolean get() = remainingSeconds < 0

    /** Durée configurée du chronomètre, exprimée en minutes entières. */
    val timerLimitMinutes: Int get() = timerLimitSeconds / 60

    /** Retourne le score du [player] indiqué. */
    fun scoreOf(player: Player): Int = when (player) {
        Player.ONE -> playerOneScore
        Player.TWO -> playerTwoScore
    }

    /**
     * Retourne un nouvel état avec le score du joueur ajusté et borné entre zéro et [MAX_SCORE].
     *
     * @param player joueur dont le score doit changer.
     * @param delta nombre de points à ajouter (ou à retirer si la valeur est négative).
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

    /** Retourne un nouvel état avec les deux scores à zéro, sans modifier le chronomètre. */
    fun withScoresReset(): GameState = copy(playerOneScore = 0, playerTwoScore = 0)

    /** Retourne un nouvel état avec une seconde supplémentaire écoulée. */
    fun withTick(): GameState = copy(elapsedSeconds = elapsedSeconds + 1)

    /** Remet le temps écoulé à zéro et arrête le chronomètre. */
    fun withTimerReset(): GameState = copy(elapsedSeconds = 0, isTimerRunning = false)

    /**
     * Retourne un nouvel état avec la durée du chronomètre mise à jour.
     *
     * Les valeurs inférieures à une minute sont ramenées à une minute et les valeurs trop grandes
     * sont plafonnées pour rester représentables en secondes.
     *
     * @param minutes durée souhaitée en minutes.
     */
    fun withTimerLimitMinutes(minutes: Int): GameState {
        val seconds = (minutes.toLong().coerceAtLeast(1L) * 60)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        return copy(timerLimitSeconds = seconds)
    }

    /** Retourne un nouvel état avec la couleur du joueur 1 mise à jour. */
    fun withPlayerOneColor(colorHex: Long): GameState = copy(playerOneColorHex = colorHex)

    /** Retourne un nouvel état avec la couleur du joueur 2 mise à jour. */
    fun withPlayerTwoColor(colorHex: Long): GameState = copy(playerTwoColorHex = colorHex)

    companion object {
        /** Score maximal d’un joueur, qui déclenche la victoire. */
        const val MAX_SCORE = 8
        /** Durée initiale du chronomètre, en secondes. */
        const val DEFAULT_TIMER_LIMIT_SECONDS = 60 * 60
        /** Alias de compatibilité pour la durée initiale du chronomètre, en secondes. */
        const val TIMER_LIMIT_SECONDS = DEFAULT_TIMER_LIMIT_SECONDS
    }
}

/**
 * Formate le temps en `mm:ss` et préfixe le temps supplémentaire par `+`.
 *
 * @param remainingSeconds secondes restantes ; une valeur négative représente le temps supplémentaire.
 * @return temps formaté, par exemple `12:34` ou `+00:12`.
 */
fun formatTimer(remainingSeconds: Int): String {
    val absolute = kotlin.math.abs(remainingSeconds.toLong())
    val sign = if (remainingSeconds < 0) "+" else ""
    val minutes = (absolute / 60).toString().padStart(2, '0')
    val seconds = (absolute % 60).toString().padStart(2, '0')
    return "$sign$minutes:$seconds"
}
