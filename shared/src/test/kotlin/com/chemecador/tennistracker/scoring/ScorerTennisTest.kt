package com.chemecador.tennistracker.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScorerTennisTest {

    @Test fun `new match starts at 0-0 with A serving`() {
        val s = Scorer.newMatch(tennisBo3())
        assertEquals(GamePhase.Normal(GameScore(0, 0)), s.phase)
        assertEquals(Side.A, s.server)
        assertEquals(0 to 0, s.currentSetGames)
        assertTrue(s.completedSets.isEmpty())
        assertNull(s.winner)
    }

    @Test fun `15 30 40 progression`() {
        val s = Scorer.newMatch(tennisBo3()).play("AAA")
        assertEquals(GamePhase.Normal(GameScore(3, 0)), s.phase)
    }

    @Test fun `love game closes`() {
        val s = Scorer.newMatch(tennisBo3()).play("AAAA")
        assertEquals(GamePhase.Normal(GameScore(0, 0)), s.phase)
        assertEquals(1 to 0, s.currentSetGames)
        assertEquals(Side.B, s.server)  // server alternates after game
    }

    @Test fun `deuce reached at 3-3`() {
        val s = Scorer.newMatch(tennisBo3()).play("AAABBB")
        assertEquals(GamePhase.Deuce, s.phase)
    }

    @Test fun `advantage A then deuce on B point`() {
        val s = Scorer.newMatch(tennisBo3()).play("AAABBB").play("A")
        assertEquals(GamePhase.Advantage(Side.A), s.phase)
        val s2 = s.play("B")
        assertEquals(GamePhase.Deuce, s2.phase)
    }

    @Test fun `advantage A then game A`() {
        val s = Scorer.newMatch(tennisBo3()).play("AAABBB").play("AA")
        assertEquals(GamePhase.Normal(GameScore(0, 0)), s.phase)
        assertEquals(1 to 0, s.currentSetGames)
    }

    @Test fun `long deuce ends correctly`() {
        // 0-0 deuce many times then B closes
        val s = Scorer.newMatch(tennisBo3()).play("AAABBB")
            .play("ABABABAB")  // alternating: adv-A, deuce, adv-A, deuce, adv-A, deuce, adv-A, deuce
            .play("BB")        // adv-B, game-B
        assertEquals(GamePhase.Normal(GameScore(0, 0)), s.phase)
        assertEquals(0 to 1, s.currentSetGames)
    }

    @Test fun `score 30-15 stays Normal`() {
        val s = Scorer.newMatch(tennisBo3()).play("AABA")
        assertEquals(GamePhase.Normal(GameScore(3, 1)), s.phase)
    }

    @Test fun `winning point after match over is no-op`() {
        // bo1 super short
        val cfg = tennisBo1()
        var s = Scorer.newMatch(cfg)
        // Win 6 games for A in a row
        repeat(6) { s = s.play("AAAA") }
        assertEquals(Side.A, s.winner)
        val before = s
        s = Scorer.winPoint(s, Side.B)
        assertEquals(before, s)
    }
}
