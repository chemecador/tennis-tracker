package com.chemecador.tennistracker.core.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.tennistracker.core.data.match.MatchRecord
import com.chemecador.tennistracker.core.data.match.MatchRepository
import com.chemecador.tennistracker.core.data.match.OpponentRef
import com.chemecador.tennistracker.core.data.match.toRecord
import com.chemecador.tennistracker.scoring.MatchConfig
import com.chemecador.tennistracker.scoring.MatchState
import com.chemecador.tennistracker.scoring.Scorer
import com.chemecador.tennistracker.scoring.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MatchSessionViewModel(
    private val matchRepository: MatchRepository,
    private val currentUid: () -> String?,
) : ViewModel() {

    private val _state = MutableStateFlow<MatchState?>(null)
    val state = _state.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = _saveState.asStateFlow()

    private var matchId: String? = null
    private var opponent: OpponentRef? = null
    private var sport: String = DEFAULT_SPORT
    private var startedAt: Long = 0L
    private var lastSavedMatchId: String? = null

    fun start(
        config: MatchConfig,
        opponent: OpponentRef,
        sport: String = DEFAULT_SPORT,
    ) {
        matchId = UUID.randomUUID().toString()
        this.opponent = opponent
        this.sport = sport
        startedAt = System.currentTimeMillis()
        lastSavedMatchId = null
        _saveState.value = SaveState.Idle
        _state.value = Scorer.newMatch(config)
    }

    fun onPoint(side: Side) {
        _state.update { current -> current?.let { Scorer.winPoint(it, side) } }
        maybePersist()
    }

    fun onUndo() {
        _state.update { current -> current?.let(Scorer::undo) }
    }

    fun endMatchEarly(winner: Side) {
        _state.update { current -> current?.let { Scorer.endMatchEarly(it, winner) } }
        maybePersist()
    }

    fun reset() {
        _state.value = null
        matchId = null
        opponent = null
        sport = DEFAULT_SPORT
        startedAt = 0L
        lastSavedMatchId = null
        _saveState.value = SaveState.Idle
    }

    fun retrySave() {
        if (_saveState.value !is SaveState.Error) return
        val record = buildCurrentRecord() ?: return
        persist(record)
    }

    private fun maybePersist() {
        val currentMatchId = matchId ?: return
        if (lastSavedMatchId == currentMatchId) return
        val record = buildCurrentRecord() ?: return
        lastSavedMatchId = currentMatchId
        persist(record)
    }

    private fun buildCurrentRecord(): MatchRecord? {
        val currentMatchId = matchId ?: return null
        val currentState = _state.value ?: return null
        if (currentState.winner == null) return null
        val currentOpponent = opponent ?: return null
        val myUid = currentUid() ?: return null
        return currentState.toRecord(
            matchId = currentMatchId,
            sport = sport,
            myUid = myUid,
            opponent = currentOpponent,
            startedAt = startedAt,
            finishedAt = System.currentTimeMillis(),
        )
    }

    private fun persist(record: MatchRecord) {
        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            matchRepository.saveCompletedMatch(record)
                .onSuccess { id -> _saveState.value = SaveState.Saved(id) }
                .onFailure { e ->
                    _saveState.value = SaveState.Error(e.message ?: "Unknown error")
                }
        }
    }

    sealed interface SaveState {
        data object Idle : SaveState
        data object Saving : SaveState
        data class Saved(val matchId: String) : SaveState
        data class Error(val message: String) : SaveState
    }

    private companion object {
        const val DEFAULT_SPORT = "tennis"
    }
}
