package com.chemecador.tennistracker.core.data.match

data class MatchRecord(
    val matchId: String = "",
    val sport: String = "",
    val format: MatchFormatRecord = MatchFormatRecord(),
    val players: List<String> = emptyList(),
    val playerNames: List<String> = emptyList(),
    val winner: String = "",
    val score: MatchScoreRecord = MatchScoreRecord(),
    val startedAt: Long = 0L,
    val finishedAt: Long = 0L,
    val status: String = "",
    val createdBy: String = "",
)

data class MatchFormatRecord(
    val bestOfSets: Int = 0,
    val finalSetMode: String = "",
    val goldenPoint: Boolean = false,
)

data class MatchScoreRecord(
    val winner: String = "",
    val sets: List<SetScoreRecord> = emptyList(),
)

data class SetScoreRecord(
    val gamesA: Int = 0,
    val gamesB: Int = 0,
    val tieBreak: TieBreakScoreRecord? = null,
)

data class TieBreakScoreRecord(
    val a: Int = 0,
    val b: Int = 0,
    val target: Int = 0,
)
