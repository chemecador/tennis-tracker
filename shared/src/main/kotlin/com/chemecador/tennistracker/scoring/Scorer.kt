package com.chemecador.tennistracker.scoring

import kotlin.math.abs

object Scorer {

    fun newMatch(config: MatchConfig): MatchState = MatchState(
        config = config,
        completedSets = emptyList(),
        currentSetGames = 0 to 0,
        phase = GamePhase.Normal(GameScore(0, 0)),
        server = Side.A,
        winner = null,
        history = emptyList(),
    )

    fun winPoint(state: MatchState, winner: Side): MatchState {
        if (state.winner != null) return state
        val next = applyPoint(state, winner)
        return next.copy(history = state.history + state.copy(history = emptyList()))
    }

    fun undo(state: MatchState): MatchState =
        state.history.lastOrNull()?.copy(history = state.history.dropLast(1)) ?: state

    fun endMatchEarly(state: MatchState, winner: Side): MatchState {
        if (state.winner != null) return state
        val snapshot = state.copy(history = emptyList())
        return state.copy(
            winner = winner,
            history = state.history + snapshot,
        )
    }

    private fun applyPoint(state: MatchState, w: Side): MatchState =
        when (val phase = state.phase) {
            is GamePhase.Normal -> applyNormalPoint(state, phase, w)
            is GamePhase.Deuce -> applyDeucePoint(state, w)
            is GamePhase.Advantage -> applyAdvantagePoint(state, phase, w)
            is GamePhase.TieBreak -> applyTieBreakPoint(state, phase, w)
        }

    private fun applyNormalPoint(state: MatchState, phase: GamePhase.Normal, w: Side): MatchState {
        val newA = phase.score.a + if (w == Side.A) 1 else 0
        val newB = phase.score.b + if (w == Side.B) 1 else 0
        return when {
            newA == 4 -> finishGame(state, Side.A)
            newB == 4 -> finishGame(state, Side.B)
            newA == 3 && newB == 3 -> state.copy(phase = GamePhase.Deuce)
            else -> state.copy(phase = GamePhase.Normal(GameScore(newA, newB)))
        }
    }

    private fun applyDeucePoint(state: MatchState, w: Side): MatchState =
        if (state.config.goldenPoint) {
            finishGame(state, w)
        } else {
            state.copy(phase = GamePhase.Advantage(w))
        }

    private fun applyAdvantagePoint(state: MatchState, phase: GamePhase.Advantage, w: Side): MatchState =
        if (w == phase.side) {
            finishGame(state, w)
        } else {
            state.copy(phase = GamePhase.Deuce)
        }

    private fun applyTieBreakPoint(state: MatchState, phase: GamePhase.TieBreak, w: Side): MatchState {
        val newA = phase.score.a + if (w == Side.A) 1 else 0
        val newB = phase.score.b + if (w == Side.B) 1 else 0
        val target = phase.score.target
        if ((newA >= target || newB >= target) && abs(newA - newB) >= 2) {
            val tbWinner = if (newA > newB) Side.A else Side.B
            return finishSetViaTieBreak(state, phase, tbWinner, TieBreakScore(newA, newB, target))
        }
        val newPointsPlayed = phase.pointsPlayed + 1
        val newServer = Rules.tieBreakServer(phase.starter, newPointsPlayed)
        return state.copy(
            phase = GamePhase.TieBreak(
                score = TieBreakScore(newA, newB, target),
                pointsPlayed = newPointsPlayed,
                starter = phase.starter,
            ),
            server = newServer,
        )
    }

    private fun finishGame(state: MatchState, gameWinner: Side): MatchState {
        val (gA, gB) = state.currentSetGames
        val newGA = gA + if (gameWinner == Side.A) 1 else 0
        val newGB = gB + if (gameWinner == Side.B) 1 else 0

        if ((newGA >= 6 || newGB >= 6) && abs(newGA - newGB) >= 2) {
            return finishSet(state, SetScore(newGA, newGB, null))
        }

        if (newGA == 6 && newGB == 6) {
            val midState = state.copy(currentSetGames = newGA to newGB)
            if (Rules.decidingSetUsesAdvantage(midState)) {
                return state.copy(
                    currentSetGames = newGA to newGB,
                    phase = GamePhase.Normal(GameScore(0, 0)),
                    server = state.server.other,
                )
            }
            val tbStarter = state.server.other
            val target = Rules.tieBreakTarget(midState)
            return state.copy(
                currentSetGames = newGA to newGB,
                phase = GamePhase.TieBreak(
                    score = TieBreakScore(0, 0, target),
                    pointsPlayed = 0,
                    starter = tbStarter,
                ),
                server = tbStarter,
            )
        }

        return state.copy(
            currentSetGames = newGA to newGB,
            phase = GamePhase.Normal(GameScore(0, 0)),
            server = state.server.other,
        )
    }

    private fun finishSet(state: MatchState, setScore: SetScore): MatchState {
        val newCompleted = state.completedSets + setScore
        val newSetsA = newCompleted.count { it.winner == Side.A }
        val newSetsB = newCompleted.count { it.winner == Side.B }
        val toWin = state.config.setsToWin

        if (newSetsA >= toWin || newSetsB >= toWin) {
            return state.copy(
                completedSets = newCompleted,
                currentSetGames = 0 to 0,
                phase = GamePhase.Normal(GameScore(0, 0)),
                winner = if (newSetsA >= toWin) Side.A else Side.B,
            )
        }

        val nextServer = state.server.other
        val enteringDecidingSet = newSetsA == toWin - 1 && newSetsB == toWin - 1
        if (enteringDecidingSet && state.config.finalSetMode == FinalSetMode.SUPER_TIEBREAK_10) {
            return state.copy(
                completedSets = newCompleted,
                currentSetGames = 0 to 0,
                phase = GamePhase.TieBreak(
                    score = TieBreakScore(0, 0, 10),
                    pointsPlayed = 0,
                    starter = nextServer,
                ),
                server = nextServer,
            )
        }

        return state.copy(
            completedSets = newCompleted,
            currentSetGames = 0 to 0,
            phase = GamePhase.Normal(GameScore(0, 0)),
            server = nextServer,
        )
    }

    private fun finishSetViaTieBreak(
        state: MatchState,
        phase: GamePhase.TieBreak,
        tbWinner: Side,
        tbScore: TieBreakScore,
    ): MatchState {
        val (gA, gB) = state.currentSetGames
        val newGA = gA + if (tbWinner == Side.A) 1 else 0
        val newGB = gB + if (tbWinner == Side.B) 1 else 0
        val setScore = SetScore(newGA, newGB, tbScore)
        val newCompleted = state.completedSets + setScore
        val newSetsA = newCompleted.count { it.winner == Side.A }
        val newSetsB = newCompleted.count { it.winner == Side.B }
        val toWin = state.config.setsToWin

        if (newSetsA >= toWin || newSetsB >= toWin) {
            return state.copy(
                completedSets = newCompleted,
                currentSetGames = 0 to 0,
                phase = GamePhase.Normal(GameScore(0, 0)),
                winner = if (newSetsA >= toWin) Side.A else Side.B,
            )
        }

        val nextServer = phase.starter.other
        return state.copy(
            completedSets = newCompleted,
            currentSetGames = 0 to 0,
            phase = GamePhase.Normal(GameScore(0, 0)),
            server = nextServer,
        )
    }
}
