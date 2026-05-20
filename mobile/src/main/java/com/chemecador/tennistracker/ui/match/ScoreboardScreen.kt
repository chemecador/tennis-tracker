package com.chemecador.tennistracker.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chemecador.tennistracker.core.match.MatchSessionViewModel
import com.chemecador.tennistracker.scoring.GamePhase
import com.chemecador.tennistracker.scoring.MatchState
import com.chemecador.tennistracker.scoring.Side

@Composable
fun ScoreboardScreen(
    viewModel: MatchSessionViewModel,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return

    KeepScreenOn()

    var showEndDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SideZone(
            side = Side.A,
            state = current,
            onTap = { viewModel.onPoint(Side.A) },
            background = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .statusBarsPadding(),
        )
        CenterStrip(
            state = current,
            onUndo = viewModel::onUndo,
            onEndMatch = { showEndDialog = true },
            onExit = onExit,
        )
        SideZone(
            side = Side.B,
            state = current,
            onTap = { viewModel.onPoint(Side.B) },
            background = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }

    if (showEndDialog && current.winner == null) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("¿Terminar partido?") },
            text = { Text("Elige el ganador.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.endMatchEarly(Side.A)
                        showEndDialog = false
                    },
                ) { Text(current.config.playerNameA) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.endMatchEarly(Side.B)
                        showEndDialog = false
                    },
                ) { Text(current.config.playerNameB) }
            },
        )
    }
}

@Composable
private fun SideZone(
    side: Side,
    state: MatchState,
    onTap: () -> Unit,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val name = if (side == Side.A) state.config.playerNameA else state.config.playerNameB
    val isServing = state.server == side
    val pointsLabel = pointsLabel(state.phase, side)

    Box(
        modifier = modifier
            .background(background)
            .clickable { onTap() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isServing) "• $name" else name,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = pointsLabel,
                color = textColor,
                fontSize = 112.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CenterStrip(
    state: MatchState,
    onUndo: () -> Unit,
    onEndMatch: () -> Unit,
    onExit: () -> Unit,
) {
    val canUndo = state.history.isNotEmpty()
    val undoColor = if (canUndo) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val canEnd = state.winner == null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onUndo, enabled = canUndo) {
            Text(
                text = "↶ Deshacer",
                color = undoColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = setsLine(state),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onEndMatch, enabled = canEnd) {
            Text(
                text = "Terminar",
                color = if (canEnd) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                fontWeight = FontWeight.SemiBold,
            )
        }
        TextButton(onClick = onExit) {
            Text("Salir")
        }
    }
}

private fun pointsLabel(phase: GamePhase, side: Side): String = when (phase) {
    is GamePhase.Normal -> tennisPoints(if (side == Side.A) phase.score.a else phase.score.b)
    GamePhase.Deuce -> "40"
    is GamePhase.Advantage -> if (phase.side == side) "Ad" else "—"
    is GamePhase.TieBreak -> if (side == Side.A) phase.score.a.toString() else phase.score.b.toString()
}

private fun tennisPoints(p: Int): String = when (p) {
    0 -> "0"
    1 -> "15"
    2 -> "30"
    3 -> "40"
    else -> p.toString()
}

private fun setsLine(state: MatchState): String {
    val parts = state.completedSets.map { set ->
        val tb = set.tieBreak
        if (tb != null && set.gamesA + set.gamesB <= 1) {
            "${tb.a}-${tb.b}"
        } else if (tb != null) {
            "${set.gamesA}-${set.gamesB}(${minOf(tb.a, tb.b)})"
        } else {
            "${set.gamesA}-${set.gamesB}"
        }
    } + run {
        val (a, b) = state.currentSetGames
        if (state.winner == null) listOf("$a-$b") else emptyList()
    }
    return parts.joinToString("  ")
}
