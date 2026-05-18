package com.chemecador.tennistracker.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chemecador.tennistracker.scoring.MatchState
import com.chemecador.tennistracker.scoring.SetScore
import com.chemecador.tennistracker.scoring.Side
import com.chemecador.tennistracker.scoring.TieBreakScore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSummaryScreen(state: MatchState?, onNewMatch: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Resumen del partido") }) },
    ) { padding ->
        if (state == null) {
            EmptyState(padding)
        } else {
            SummaryContent(state = state, padding = padding, onNewMatch = onNewMatch)
        }
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sin partido")
    }
}

@Composable
private fun SummaryContent(
    state: MatchState,
    padding: PaddingValues,
    onNewMatch: () -> Unit,
) {
    val winnerName = when (state.winner) {
        Side.A -> state.config.playerNameA
        Side.B -> state.config.playerNameB
        null -> "—"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Ganador",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = winnerName,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Resultado",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ScoreTable(state = state)

        if (state.completedSets.any { it.tieBreak != null }) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tie-breaks",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.completedSets.forEachIndexed { idx, set ->
                val tb = set.tieBreak ?: return@forEachIndexed
                Text(
                    text = "Set ${idx + 1}: ${formatTieBreak(set, tb)}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNewMatch,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Nuevo partido")
        }
    }
}

@Composable
private fun ScoreTable(state: MatchState) {
    val (gamesA, gamesB) = matchScore(state)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScoreRow(
            name = state.config.playerNameA,
            sets = gamesA,
            isWinner = state.winner == Side.A,
        )
        ScoreRow(
            name = state.config.playerNameB,
            sets = gamesB,
            isWinner = state.winner == Side.B,
        )
    }
}

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

@Composable
private fun ScoreRow(name: String, sets: List<Int>, isWinner: Boolean) {
    val weight = if (isWinner) FontWeight.Bold else FontWeight.Normal
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = weight,
            modifier = Modifier.width(140.dp),
        )
        sets.forEach { games ->
            Text(
                text = games.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = weight,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(36.dp),
            )
        }
    }
}

private fun formatTieBreak(set: SetScore, tb: TieBreakScore): String =
    if (set.gamesA + set.gamesB <= 1) "${tb.a}-${tb.b} (super TB)" else "${tb.a}-${tb.b}"
