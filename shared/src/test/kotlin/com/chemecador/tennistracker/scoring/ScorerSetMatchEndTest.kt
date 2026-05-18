package com.chemecador.tennistracker.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScorerSetMatchEndTest {

    @Test fun `set won 6-0`() {
        var s = Scorer.newMatch(tennisBo1())
        repeat(6) { s = s.play("AAAA") }
        assertEquals(Side.A, s.winner)
        val last = s.completedSets.last()
        assertEquals(6, last.gamesA)
        assertEquals(0, last.gamesB)
        assertNull(last.tieBreak)
    }

    @Test fun `set won 6-4`() {
        var s = Scorer.newMatch(tennisBo1())
        // 4 games for A, 4 for B (alternating), then 2 more for A → 6-4
        s = s.play("AAAABBBB".repeat(4))  // 4-4
        assertEquals(4 to 4, s.currentSetGames)
        s = s.play("AAAA")  // 5-4
        s = s.play("AAAA")  // 6-4
        assertEquals(Side.A, s.winner)
        val last = s.completedSets.last()
        assertEquals(6, last.gamesA)
        assertEquals(4, last.gamesB)
    }

    @Test fun `set won 7-5`() {
        var s = Scorer.newMatch(tennisBo1())
        // Reach 5-5
        s = s.play("AAAABBBB".repeat(5))
        assertEquals(5 to 5, s.currentSetGames)
        s = s.play("AAAA")  // 6-5
        assertNull(s.winner)
        s = s.play("AAAA")  // 7-5
        assertEquals(Side.A, s.winner)
        val last = s.completedSets.last()
        assertEquals(7, last.gamesA)
        assertEquals(5, last.gamesB)
    }

    @Test fun `set goes 6-5 not won`() {
        var s = Scorer.newMatch(tennisBo1())
        s = s.play("AAAABBBB".repeat(5))  // 5-5
        s = s.play("AAAA")  // 6-5
        assertNull(s.winner)
        assertEquals(6 to 5, s.currentSetGames)
    }

    @Test fun `bo3 match ends after 2 sets`() {
        var s = Scorer.newMatch(tennisBo3())
        // A wins 6-0, 6-0
        repeat(6) { s = s.play("AAAA") }
        assertNull(s.winner)
        assertEquals(1, s.completedSets.size)
        repeat(6) { s = s.play("AAAA") }
        assertEquals(Side.A, s.winner)
        assertEquals(2, s.completedSets.size)
    }

    @Test fun `bo3 goes to 3 sets`() {
        var s = Scorer.newMatch(tennisBo3())
        // A wins set 1
        repeat(6) { s = s.play("AAAA") }
        // B wins set 2
        repeat(6) { s = s.play("BBBB") }
        assertNull(s.winner)
        assertEquals(2, s.completedSets.size)
        // A wins set 3
        repeat(6) { s = s.play("AAAA") }
        assertEquals(Side.A, s.winner)
        assertEquals(3, s.completedSets.size)
    }

    @Test fun `bo5 match ends after 3 sets to one side`() {
        var s = Scorer.newMatch(tennisBo5())
        repeat(3) { repeat(6) { s = s.play("AAAA") } }
        assertEquals(Side.A, s.winner)
        assertEquals(3, s.completedSets.size)
    }

    @Test fun `bo5 can go 5 sets`() {
        var s = Scorer.newMatch(tennisBo5())
        // A, B, A, B, A
        repeat(6) { s = s.play("AAAA") }
        repeat(6) { s = s.play("BBBB") }
        repeat(6) { s = s.play("AAAA") }
        repeat(6) { s = s.play("BBBB") }
        assertEquals(2, s.setsWonA)
        assertEquals(2, s.setsWonB)
        assertNull(s.winner)
        repeat(6) { s = s.play("AAAA") }
        assertEquals(Side.A, s.winner)
        assertEquals(5, s.completedSets.size)
    }

    @Test fun `setsToWin computed correctly`() {
        assertEquals(1, tennisBo1().setsToWin)
        assertEquals(2, tennisBo3().setsToWin)
        assertEquals(3, tennisBo5().setsToWin)
    }

    @Test fun `endMatchEarly sets winner and preserves completed sets`() {
        var s = Scorer.newMatch(tennisBo3())
        // A wins set 1: 6-0
        repeat(6) { s = s.play("AAAA") }
        assertEquals(1, s.completedSets.size)
        // Mid second set: 3-2 for A
        s = s.play("AAAABBBBAAAABBBBAAAA")
        assertEquals(3 to 2, s.currentSetGames)
        assertNull(s.winner)

        val ended = Scorer.endMatchEarly(s, Side.B)

        assertEquals(Side.B, ended.winner)
        assertEquals(1, ended.completedSets.size)
        assertEquals(3 to 2, ended.currentSetGames)
    }

    @Test fun `endMatchEarly is a no-op when match already has a winner`() {
        var s = Scorer.newMatch(tennisBo1())
        repeat(6) { s = s.play("AAAA") }
        assertEquals(Side.A, s.winner)

        val unchanged = Scorer.endMatchEarly(s, Side.B)

        assertEquals(Side.A, unchanged.winner)
        assertEquals(s, unchanged)
    }
}
