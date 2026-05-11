package com.chemecador.tennistracker.scoring

internal object Rules {

    fun isDecidingSet(state: MatchState): Boolean {
        val toWin = state.config.setsToWin
        return state.setsWonA == toWin - 1 && state.setsWonB == toWin - 1
    }

    fun tieBreakTarget(state: MatchState): Int {
        val deciding = isDecidingSet(state)
        return when (state.config.finalSetMode) {
            FinalSetMode.SUPER_TIEBREAK_10 -> if (deciding) 10 else 7
            FinalSetMode.TIEBREAK_7 -> 7
            FinalSetMode.ADVANTAGE -> 7
        }
    }

    fun decidingSetUsesAdvantage(state: MatchState): Boolean =
        state.config.finalSetMode == FinalSetMode.ADVANTAGE && isDecidingSet(state)

    fun tieBreakServer(starter: Side, pointsPlayed: Int): Side {
        if (pointsPlayed == 0) return starter
        val groupsAfterFirst = (pointsPlayed - 1) / 2 + 1
        return if (groupsAfterFirst % 2 == 1) starter.other else starter
    }
}
