package com.chemecador.tennistracker.scoring

import org.junit.Assert.assertEquals
import org.junit.Test

class ScorerPadelTest {

    @Test fun `golden point closes game on deuce - A`() {
        val s = Scorer.newMatch(padelBo3(goldenPoint = true)).play("AAABBB")
        assertEquals(GamePhase.Deuce, s.phase)
        val s2 = s.play("A")
        assertEquals(GamePhase.Normal(GameScore(0, 0)), s2.phase)
        assertEquals(1 to 0, s2.currentSetGames)
    }

    @Test fun `golden point closes game on deuce - B`() {
        val s = Scorer.newMatch(padelBo3(goldenPoint = true)).play("AAABBB").play("B")
        assertEquals(GamePhase.Normal(GameScore(0, 0)), s.phase)
        assertEquals(0 to 1, s.currentSetGames)
    }

    @Test fun `padel without golden point still uses advantage`() {
        val s = Scorer.newMatch(padelBo3(goldenPoint = false)).play("AAABBB").play("A")
        assertEquals(GamePhase.Advantage(Side.A), s.phase)
    }

    @Test fun `padel love game with golden point flag does not affect normal flow`() {
        val s = Scorer.newMatch(padelBo3(goldenPoint = true)).play("AAAA")
        assertEquals(GamePhase.Normal(GameScore(0, 0)), s.phase)
        assertEquals(1 to 0, s.currentSetGames)
    }
}
