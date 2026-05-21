package com.chemecador.tennistracker.core.data.match

import com.chemecador.tennistracker.scoring.FinalSetMode
import com.chemecador.tennistracker.scoring.MatchConfig
import com.chemecador.tennistracker.scoring.Scorer
import com.chemecador.tennistracker.scoring.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchRecordMapperTest {

    @Test
    fun `maps a finished bo1 match against a registered opponent`() {
        val config = MatchConfig(
            bestOfSets = 1,
            finalSetMode = FinalSetMode.TIEBREAK_7,
            goldenPoint = false,
            playerNameA = "me",
            playerNameB = "rival",
        )
        var state = Scorer.newMatch(config)
        // Play 6-0 (24 puntos seguidos para A).
        repeat(24) { state = Scorer.winPoint(state, Side.A) }

        val record = state.toRecord(
            matchId = "match-1",
            sport = "tennis",
            myUid = "uid-me",
            opponent = OpponentRef.Registered("uid-rival"),
            startedAt = 100L,
            finishedAt = 200L,
        )

        assertEquals("match-1", record.matchId)
        assertEquals("tennis", record.sport)
        assertEquals(1, record.format.bestOfSets)
        assertEquals("TIEBREAK_7", record.format.finalSetMode)
        assertEquals(false, record.format.goldenPoint)
        assertEquals(listOf("uid-me", "uid-rival"), record.players)
        assertEquals(listOf("me", "rival"), record.playerNames)
        assertEquals("uid-me", record.winner)
        assertEquals("A", record.score.winner)
        assertEquals(1, record.score.sets.size)
        assertEquals(6, record.score.sets[0].gamesA)
        assertEquals(0, record.score.sets[0].gamesB)
        assertNull(record.score.sets[0].tieBreak)
        assertEquals(100L, record.startedAt)
        assertEquals(200L, record.finishedAt)
        assertEquals("confirmed", record.status)
        assertEquals("uid-me", record.createdBy)
    }

    @Test
    fun `maps a finished match against a guest opponent`() {
        val config = MatchConfig(
            bestOfSets = 1,
            finalSetMode = FinalSetMode.TIEBREAK_7,
            goldenPoint = false,
            playerNameA = "me",
            playerNameB = "Invitado",
        )
        var state = Scorer.newMatch(config)
        repeat(24) { state = Scorer.winPoint(state, Side.B) }

        val record = state.toRecord(
            matchId = "m2",
            sport = "tennis",
            myUid = "uid-me",
            opponent = OpponentRef.Guest("Invitado"),
            startedAt = 0L,
            finishedAt = 1L,
        )

        assertEquals(listOf("uid-me", "guest:Invitado"), record.players)
        assertEquals("guest:Invitado", record.winner)
        assertEquals("B", record.score.winner)
    }

    @Test
    fun `serialises tiebreak when present`() {
        val config = MatchConfig(
            bestOfSets = 1,
            finalSetMode = FinalSetMode.TIEBREAK_7,
            goldenPoint = false,
            playerNameA = "A",
            playerNameB = "B",
        )
        var state = Scorer.newMatch(config)
        // 5 games A + 5 games B = 5-5, luego 1 game A + 1 game B = 6-6 → tiebreak.
        repeat(5) { repeat(4) { state = Scorer.winPoint(state, Side.A) } }
        repeat(5) { repeat(4) { state = Scorer.winPoint(state, Side.B) } }
        repeat(4) { state = Scorer.winPoint(state, Side.A) } // 6-5
        repeat(4) { state = Scorer.winPoint(state, Side.B) } // 6-6 → TB
        // Ganar TB para A: 7 puntos.
        repeat(7) { state = Scorer.winPoint(state, Side.A) }

        assertTrue("Match should be finished: ${state.winner}", state.winner != null)
        val record = state.toRecord(
            matchId = "m3",
            sport = "tennis",
            myUid = "uid-me",
            opponent = OpponentRef.Guest("B"),
            startedAt = 0L,
            finishedAt = 1L,
        )
        val set = record.score.sets.single()
        assertEquals(7, set.gamesA)
        assertEquals(6, set.gamesB)
        val tb = set.tieBreak
        assertTrue("tieBreak should be present", tb != null)
        assertEquals(7, tb!!.a)
    }
}
