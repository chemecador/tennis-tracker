package com.chemecador.tennistracker.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            text = "Sets",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.completedSets.forEachIndexed { idx, set ->
            Text(
                text = "${idx + 1}.  ${formatSet(set)}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
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

private fun formatSet(set: SetScore): String {
    val tb = set.tieBreak
    return when {
        tb != null && set.gamesA + set.gamesB <= 1 -> "${tb.a}-${tb.b}  (super TB)"
        tb != null -> "${set.gamesA}-${set.gamesB}  (${tb.a}-${tb.b})"
        else -> "${set.gamesA}-${set.gamesB}"
    }
}
