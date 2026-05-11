package com.chemecador.tennistracker.scoring

internal fun tennisBo3(
    finalSetMode: FinalSetMode = FinalSetMode.TIEBREAK_7,
): MatchConfig = MatchConfig(
    sport = Sport.TENNIS,
    bestOfSets = 3,
    finalSetMode = finalSetMode,
    goldenPoint = false,
    playerNameA = "A",
    playerNameB = "B",
)

internal fun tennisBo1(
    finalSetMode: FinalSetMode = FinalSetMode.TIEBREAK_7,
): MatchConfig = tennisBo3(finalSetMode).copy(bestOfSets = 1)

internal fun tennisBo5(
    finalSetMode: FinalSetMode = FinalSetMode.TIEBREAK_7,
): MatchConfig = tennisBo3(finalSetMode).copy(bestOfSets = 5)

internal fun padelBo3(
    goldenPoint: Boolean = true,
    finalSetMode: FinalSetMode = FinalSetMode.SUPER_TIEBREAK_10,
): MatchConfig = MatchConfig(
    sport = Sport.PADEL,
    bestOfSets = 3,
    finalSetMode = finalSetMode,
    goldenPoint = goldenPoint,
    playerNameA = "A",
    playerNameB = "B",
)

/** Apply a sequence of points where 'A' = Side.A wins, 'B' = Side.B wins. */
internal fun MatchState.play(seq: String): MatchState =
    seq.fold(this) { s, c ->
        require(c == 'A' || c == 'B') { "invalid char $c in sequence" }
        Scorer.winPoint(s, if (c == 'A') Side.A else Side.B)
    }
