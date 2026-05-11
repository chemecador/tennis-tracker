package com.chemecador.tennistracker.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScorerTieBreakTest {

    /** Reaches 6-6 in the first set with both players holding all serves. */
    private fun reachSixAll(): MatchState {
        // 12 hold-serve games: each "AAAA" or "BBBB" wins a game for that side
        val seq = "AAAABBBB".repeat(6)
        return Scorer.newMatch(tennisBo1()).play(seq)
    }

    @Test fun `enters tie break at 6-6`() {
        val s = reachSixAll()
        assertEquals(6 to 6, s.currentSetGames)
        val phase = s.phase
        assertTrue("expected TieBreak, got $phase", phase is GamePhase.TieBreak)
        phase as GamePhase.TieBreak
        assertEquals(7, phase.score.target)
        assertEquals(0, phase.pointsPlayed)
    }

    @Test fun `tie break starter is receiver of last game`() {
        // Game 12 server = B (alternating from A on odd games). Starter = B.other = A.
        val s = reachSixAll()
        val phase = s.phase as GamePhase.TieBreak
        assertEquals(Side.A, phase.starter)
        assertEquals(Side.A, s.server)
    }

    @Test fun `tie break server rotation 1-2-2-2`() {
        var s = reachSixAll()
        val starter = (s.phase as GamePhase.TieBreak).starter
        // Point 1: starter serves
        assertEquals(starter, s.server)
        s = s.play("A")
        // Points 2 and 3: other serves
        assertEquals(starter.other, s.server)
        s = s.play("A")
        assertEquals(starter.other, s.server)
        s = s.play("A")
        // Points 4 and 5: starter serves
        assertEquals(starter, s.server)
        s = s.play("A")
        assertEquals(starter, s.server)
        s = s.play("A")
        // Points 6 and 7: other serves
        assertEquals(starter.other, s.server)
    }

    @Test fun `tie break won 7-5 closes set 7-6`() {
        // Sequence A,B,A,B,A,B,A,B,A,B,A,A → final TB tally A=7, B=5
        val s = reachSixAll().play("ABABABABABAA")
        val last = s.completedSets.last()
        assertEquals(Side.A, last.winner)
        assertEquals(7, last.gamesA)
        assertEquals(6, last.gamesB)
        assertNotNull(last.tieBreak)
        assertEquals(7, last.tieBreak!!.a)
        assertEquals(5, last.tieBreak.b)
    }

    @Test fun `tie break needs diff of 2`() {
        val s = reachSixAll().play("ABABABABABAB")  // 6-6 in TB
        val phase = s.phase as GamePhase.TieBreak
        assertEquals(6, phase.score.a)
        assertEquals(6, phase.score.b)
        // Continue: A wins 7-6 (not enough diff), then 8-6 closes
        val s2 = s.play("AA")
        val last = s2.completedSets.last()
        assertNotNull(last.tieBreak)
        assertEquals(8, last.tieBreak!!.a)
        assertEquals(6, last.tieBreak.b)
    }

    @Test fun `super tie-break replaces deciding set in bo3`() {
        // Padel bo3 super-TB, no golden, A wins set 1, B wins set 2, then TB-10 in set 3
        val cfg = padelBo3(goldenPoint = false, finalSetMode = FinalSetMode.SUPER_TIEBREAK_10)
        var s = Scorer.newMatch(cfg)
        // set 1: A wins 6-0 → 24 A points (6 games × 4 points)
        repeat(6) { s = s.play("AAAA") }
        assertEquals(1, s.completedSets.size)
        // set 2: B wins 6-0
        repeat(6) { s = s.play("BBBB") }
        assertEquals(2, s.completedSets.size)
        // Now should be in super-TB
        val phase = s.phase
        assertTrue("expected TieBreak in deciding set, got $phase", phase is GamePhase.TieBreak)
        phase as GamePhase.TieBreak
        assertEquals(10, phase.score.target)
        assertEquals(0, phase.pointsPlayed)
        assertEquals(0 to 0, s.currentSetGames)
    }

    @Test fun `super tie-break ends match`() {
        val cfg = padelBo3(goldenPoint = false, finalSetMode = FinalSetMode.SUPER_TIEBREAK_10)
        var s = Scorer.newMatch(cfg)
        repeat(6) { s = s.play("AAAA") }
        repeat(6) { s = s.play("BBBB") }
        // super-TB: A wins 10-0
        s = s.play("AAAAAAAAAA")
        assertEquals(Side.A, s.winner)
        val deciding = s.completedSets.last()
        assertNotNull(deciding.tieBreak)
        assertEquals(10, deciding.tieBreak!!.a)
        assertEquals(0, deciding.tieBreak.b)
    }

    @Test fun `advantage final set has no tie break`() {
        val cfg = tennisBo1(finalSetMode = FinalSetMode.ADVANTAGE)
        var s = Scorer.newMatch(cfg)
        // Reach 6-6 in this single set (which IS the deciding set in bo1)
        s = s.play("AAAABBBB".repeat(6))
        assertEquals(6 to 6, s.currentSetGames)
        // Should NOT enter TB; stay in Normal
        assertTrue(s.phase is GamePhase.Normal)
        assertNull(s.winner)
        // Continue: A holds, B holds → 7-7
        s = s.play("AAAABBBB")
        assertEquals(7 to 7, s.currentSetGames)
        // A breaks B (wins one of B's serves), then holds → 9-7
        s = s.play("AAAA")  // 8-7
        s = s.play("AAAA")  // 9-7
        assertEquals(Side.A, s.winner)
    }

    @Test fun `super tie break in deciding set serves correctly`() {
        val cfg = padelBo3(goldenPoint = false, finalSetMode = FinalSetMode.SUPER_TIEBREAK_10)
        var s = Scorer.newMatch(cfg)
        repeat(6) { s = s.play("AAAA") }
        repeat(6) { s = s.play("BBBB") }
        // Set 2 ended; set 3 server = (set 2 server's other)
        // Set 1 starter: A. Set 1 ends after 6 A-games. Set 1 last game server: A (game 1=A, ..., game 11=A, set ends at 6-0 after game 6: server of game 6 = B).
        // Actually with 6-0 sweep: games 1..6 = A,B,A,B,A,B. Game 6 server = B. After set: nextServer = B.other = A. So set 2 starts with A serving.
        // But A loses every game in set 2: games 7..12 = A,B,A,B,A,B (alternating server). Game 12 server = B. After set 2: nextServer = B.other = A.
        // Super-TB starter = A.
        val phase = s.phase as GamePhase.TieBreak
        assertEquals(Side.A, phase.starter)
        assertEquals(Side.A, s.server)
    }
}
