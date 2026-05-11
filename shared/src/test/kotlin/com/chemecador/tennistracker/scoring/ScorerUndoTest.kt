package com.chemecador.tennistracker.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ScorerUndoTest {

    @Test fun `undo on initial state is no-op`() {
        val s = Scorer.newMatch(tennisBo3())
        assertSame(s, Scorer.undo(s))
    }

    @Test fun `undo single point`() {
        val s0 = Scorer.newMatch(tennisBo3())
        val s1 = Scorer.winPoint(s0, Side.A)
        assertEquals(GamePhase.Normal(GameScore(1, 0)), s1.phase)
        val s0Restored = Scorer.undo(s1)
        assertEquals(s0.phase, s0Restored.phase)
        assertEquals(s0.currentSetGames, s0Restored.currentSetGames)
        assertEquals(s0.server, s0Restored.server)
    }

    @Test fun `undo from advantage back to deuce`() {
        val s = Scorer.newMatch(tennisBo3()).play("AAABBB")  // deuce
        val sAdv = Scorer.winPoint(s, Side.A)
        assertEquals(GamePhase.Advantage(Side.A), sAdv.phase)
        val sBack = Scorer.undo(sAdv)
        assertEquals(GamePhase.Deuce, sBack.phase)
    }

    @Test fun `undo from game-won back to advantage`() {
        val s = Scorer.newMatch(tennisBo3()).play("AAABBB").play("A")  // adv-A
        val sGame = Scorer.winPoint(s, Side.A)
        assertEquals(1 to 0, sGame.currentSetGames)
        val sBack = Scorer.undo(sGame)
        assertEquals(GamePhase.Advantage(Side.A), sBack.phase)
        assertEquals(0 to 0, sBack.currentSetGames)
    }

    @Test fun `undo restores server after game change`() {
        val s = Scorer.newMatch(tennisBo3()).play("AAAA")  // game won, server now B
        assertEquals(Side.B, s.server)
        val sBack = Scorer.undo(s)
        assertEquals(Side.A, sBack.server)
    }

    @Test fun `undo mid tie-break`() {
        var s = Scorer.newMatch(tennisBo1()).play("AAAABBBB".repeat(6))
        assertTrue(s.phase is GamePhase.TieBreak)
        s = s.play("AAA")  // 3-0 in TB, pointsPlayed=3
        val phase = s.phase as GamePhase.TieBreak
        assertEquals(3, phase.score.a)
        val sBack = Scorer.undo(s)
        val backPhase = sBack.phase as GamePhase.TieBreak
        assertEquals(2, backPhase.score.a)
        assertEquals(2, backPhase.pointsPlayed)
    }

    @Test fun `undo after match end revives match`() {
        var s = Scorer.newMatch(tennisBo1())
        repeat(5) { s = s.play("AAAA") }  // 5-0
        s = s.play("AAA")  // 5-0, 40-0 in current game
        val sFinal = Scorer.winPoint(s, Side.A)  // 6-0, match won
        assertEquals(Side.A, sFinal.winner)
        val sBack = Scorer.undo(sFinal)
        assertNull(sBack.winner)
        assertEquals(5 to 0, sBack.currentSetGames)
    }

    @Test fun `multiple undos rewind point by point`() {
        val s0 = Scorer.newMatch(tennisBo3())
        val s1 = Scorer.winPoint(s0, Side.A)
        val s2 = Scorer.winPoint(s1, Side.A)
        val s3 = Scorer.winPoint(s2, Side.A)
        val s4 = Scorer.winPoint(s3, Side.A)  // game won

        val u1 = Scorer.undo(s4)
        assertEquals(GamePhase.Normal(GameScore(3, 0)), u1.phase)
        assertEquals(0 to 0, u1.currentSetGames)

        val u2 = Scorer.undo(u1)
        assertEquals(GamePhase.Normal(GameScore(2, 0)), u2.phase)

        val u3 = Scorer.undo(u2)
        assertEquals(GamePhase.Normal(GameScore(1, 0)), u3.phase)

        val u4 = Scorer.undo(u3)
        assertEquals(GamePhase.Normal(GameScore(0, 0)), u4.phase)
        assertTrue(u4.history.isEmpty())

        val u5 = Scorer.undo(u4)
        assertEquals(u4.phase, u5.phase)
    }
}
