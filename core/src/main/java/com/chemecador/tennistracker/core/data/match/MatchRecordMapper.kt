package com.chemecador.tennistracker.core.data.match

import com.chemecador.tennistracker.scoring.MatchState
import com.chemecador.tennistracker.scoring.SetScore
import com.chemecador.tennistracker.scoring.Side
import com.chemecador.tennistracker.scoring.TieBreakScore

fun MatchState.toRecord(
    matchId: String,
    sport: String,
    myUid: String,
    opponent: OpponentRef,
    startedAt: Long,
    finishedAt: Long,
    status: String = "confirmed",
): MatchRecord {
    val winnerSide = requireNotNull(winner) { "Cannot persist a match without a winner" }
    val opponentToken = opponent.token
    val winnerToken = when (winnerSide) {
        Side.A -> myUid
        Side.B -> opponentToken
    }
    return MatchRecord(
        matchId = matchId,
        sport = sport,
        format = MatchFormatRecord(
            bestOfSets = config.bestOfSets,
            finalSetMode = config.finalSetMode.name,
            goldenPoint = config.goldenPoint,
        ),
        players = listOf(myUid, opponentToken),
        playerNames = listOf(config.playerNameA, config.playerNameB),
        winner = winnerToken,
        score = MatchScoreRecord(
            winner = winnerSide.name,
            sets = completedSets.map { it.toRecord() },
        ),
        startedAt = startedAt,
        finishedAt = finishedAt,
        status = status,
        createdBy = myUid,
    )
}

private fun SetScore.toRecord(): SetScoreRecord = SetScoreRecord(
    gamesA = gamesA,
    gamesB = gamesB,
    tieBreak = tieBreak?.toRecord(),
)

private fun TieBreakScore.toRecord(): TieBreakScoreRecord = TieBreakScoreRecord(
    a = a,
    b = b,
    target = target,
)
