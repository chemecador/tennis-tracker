package com.chemecador.tennistracker.wear.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.chemecador.tennistracker.core.match.MatchSessionViewModel.SaveState
import com.chemecador.tennistracker.scoring.MatchState
import com.chemecador.tennistracker.scoring.SetScore
import com.chemecador.tennistracker.scoring.Side
import com.chemecador.tennistracker.scoring.TieBreakScore

@Composable
fun MatchSummaryScreen(
    state: MatchState?,
    saveState: SaveState,
    onRetrySave: () -> Unit,
    onNewMatch: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state == null) {
                item { Text("Sin partido") }
                return@ScalingLazyColumn
            }

            val winnerName = when (state.winner) {
                Side.A -> state.config.playerNameA
                Side.B -> state.config.playerNameB
                null -> "—"
            }

            item {
                Text(
                    text = "Ganador",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text(
                    text = winnerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            item { Spacer(Modifier.height(2.dp)) }

            item {
                Text(
                    text = "Resultado",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
            val (gamesA, gamesB) = matchScore(state)
            item {
                ScoreRow(
                    name = state.config.playerNameA,
                    sets = gamesA,
                    isWinner = state.winner == Side.A,
                )
            }
            item {
                ScoreRow(
                    name = state.config.playerNameB,
                    sets = gamesB,
                    isWinner = state.winner == Side.B,
                )
            }

            if (state.completedSets.any { it.tieBreak != null }) {
                item { Spacer(Modifier.height(2.dp)) }
                item {
                    Text(
                        text = "Tie-breaks",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
                state.completedSets.forEachIndexed { idx, set ->
                    val tb = set.tieBreak
                    if (tb != null) {
                        item {
                            Text(
                                text = "S${idx + 1}: ${formatTieBreak(set, tb)}",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            when (saveState) {
                SaveState.Idle -> Unit
                SaveState.Saving -> item {
                    Text(
                        text = "Guardando…",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }

                is SaveState.Saved -> item {
                    Text(
                        text = "Guardado",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                is SaveState.Error -> {
                    item {
                        Text(
                            text = "Error al guardar",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    item {
                        Button(
                            onClick = onRetrySave,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onNewMatch,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Nuevo partido")
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(name: String, sets: List<Int>, isWinner: Boolean) {
    val weight = if (isWinner) FontWeight.Bold else FontWeight.Normal
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = weight,
            modifier = Modifier.width(72.dp),
        )
        sets.forEach { games ->
            Text(
                text = games.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = weight,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(22.dp),
            )
        }
    }
}

private fun formatTieBreak(set: SetScore, tb: TieBreakScore): String =
    if (set.gamesA + set.gamesB <= 1) "${tb.a}-${tb.b} (super TB)" else "${tb.a}-${tb.b}"

private fun matchScore(state: MatchState): Pair<List<Int>, List<Int>> {
    val gamesA = state.completedSets.map { it.gamesA }.toMutableList()
    val gamesB = state.completedSets.map { it.gamesB }.toMutableList()
    val (curA, curB) = state.currentSetGames
    if (curA > 0 || curB > 0 || gamesA.isEmpty()) {
        gamesA += curA
        gamesB += curB
    }
    return gamesA to gamesB
}
