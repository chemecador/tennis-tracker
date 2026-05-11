package com.chemecador.tennistracker.scoring

enum class Side {
    A, B;

    val other: Side get() = if (this == A) B else A
}

enum class Sport { TENNIS, PADEL }

enum class FinalSetMode {
    ADVANTAGE,
    TIEBREAK_7,
    SUPER_TIEBREAK_10,
}

data class MatchConfig(
    val sport: Sport,
    val bestOfSets: Int,
    val finalSetMode: FinalSetMode,
    val goldenPoint: Boolean,
    val playerNameA: String,
    val playerNameB: String,
) {
    init {
        require(bestOfSets in listOf(1, 3, 5)) { "bestOfSets must be 1, 3 or 5" }
    }

    val setsToWin: Int get() = bestOfSets / 2 + 1
}

data class GameScore(val a: Int, val b: Int)

data class TieBreakScore(val a: Int, val b: Int, val target: Int)

data class SetScore(
    val gamesA: Int,
    val gamesB: Int,
    val tieBreak: TieBreakScore?,
) {
    val winner: Side? get() = when {
        gamesA > gamesB -> Side.A
        gamesB > gamesA -> Side.B
        else -> null
    }
}

sealed interface GamePhase {
    data class Normal(val score: GameScore) : GamePhase
    data object Deuce : GamePhase
    data class Advantage(val side: Side) : GamePhase
    data class TieBreak(val score: TieBreakScore, val pointsPlayed: Int, val starter: Side) : GamePhase
}

data class MatchState(
    val config: MatchConfig,
    val completedSets: List<SetScore>,
    val currentSetGames: Pair<Int, Int>,
    val phase: GamePhase,
    val server: Side,
    val winner: Side?,
    val history: List<MatchState> = emptyList(),
) {
    val setsWonA: Int get() = completedSets.count { it.winner == Side.A }
    val setsWonB: Int get() = completedSets.count { it.winner == Side.B }
}
