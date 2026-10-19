package com.cgladin.riftboundcounter.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    @Test
    fun scoreIsClampedBetweenZeroAndMax() {
        val low = GameState().withScoreChange(Player.ONE, -1)
        assertEquals(0, low.playerOneScore)

        val high = GameState(playerOneScore = GameState.MAX_SCORE).withScoreChange(Player.ONE, 1)
        assertEquals(GameState.MAX_SCORE, high.playerOneScore)
    }

    @Test
    fun scoreChangeDoesNotOverflowForExtremeDeltas() {
        assertEquals(
            GameState.MAX_SCORE,
            GameState().withScoreChange(Player.ONE, Int.MAX_VALUE).playerOneScore,
        )
        assertEquals(
            0,
            GameState(playerOneScore = GameState.MAX_SCORE)
                .withScoreChange(Player.ONE, Int.MIN_VALUE)
                .playerOneScore,
        )
    }

    @Test
    fun scoreChangeOnlyAffectsTargetedPlayer() {
        val state = GameState().withScoreChange(Player.TWO, 3)
        assertEquals(0, state.playerOneScore)
        assertEquals(3, state.playerTwoScore)
    }

    @Test
    fun resetScoresKeepsTimer() {
        val state = GameState(playerOneScore = 4, playerTwoScore = 2, elapsedSeconds = 30)
            .withScoresReset()
        assertEquals(0, state.playerOneScore)
        assertEquals(0, state.playerTwoScore)
        assertEquals(30, state.elapsedSeconds)
    }

    @Test
    fun timerTurnsOvertimeAfterOneHour() {
        assertFalse(GameState(elapsedSeconds = GameState.TIMER_LIMIT_SECONDS).isOvertime)
        assertTrue(GameState(elapsedSeconds = GameState.TIMER_LIMIT_SECONDS + 1).isOvertime)
    }

    @Test
    fun timerResetStopsAndClears() {
        val state = GameState(elapsedSeconds = 99, isTimerRunning = true).withTimerReset()
        assertEquals(0, state.elapsedSeconds)
        assertFalse(state.isTimerRunning)
    }

    @Test
    fun formatTimerHandlesRemainingAndOvertime() {
        assertEquals("60:00", formatTimer(3600))
        assertEquals("00:00", formatTimer(0))
        assertEquals("+00:12", formatTimer(-12))
        assertEquals("+01:05", formatTimer(-65))
        assertEquals("+35791394:08", formatTimer(Int.MIN_VALUE))
    }

    @Test
    fun timerLimitCanBeUpdatedInMinutes() {
        val state = GameState().withTimerLimitMinutes(45)
        assertEquals(45, state.timerLimitMinutes)
        assertEquals(2700, state.remainingSeconds)
    }

    @Test
    fun timerLimitClampsInvalidAndUnrepresentableValues() {
        assertEquals(60, GameState().withTimerLimitMinutes(0).timerLimitSeconds)
        assertEquals(
            Int.MAX_VALUE,
            GameState().withTimerLimitMinutes(Int.MAX_VALUE).timerLimitSeconds,
        )
    }

    @Test
    fun playerColorsCanBeUpdated() {
        val newColorP1 = 0xFF3D8BEBL
        val newColorP2 = 0xFFFF9800L
        val state = GameState()
            .withPlayerOneColor(newColorP1)
            .withPlayerTwoColor(newColorP2)

        assertEquals(newColorP1, state.playerOneColorHex)
        assertEquals(newColorP2, state.playerTwoColorHex)
    }
}
