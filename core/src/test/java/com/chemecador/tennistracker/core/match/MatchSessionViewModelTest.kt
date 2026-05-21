package com.chemecador.tennistracker.core.match

import com.chemecador.tennistracker.core.data.match.MatchRecord
import com.chemecador.tennistracker.core.data.match.MatchRepository
import com.chemecador.tennistracker.core.data.match.OpponentRef
import com.chemecador.tennistracker.scoring.FinalSetMode
import com.chemecador.tennistracker.scoring.MatchConfig
import com.chemecador.tennistracker.scoring.Side
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchSessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm(repo: FakeMatchRepository = FakeMatchRepository()) =
        MatchSessionViewModel(
            matchRepository = repo,
            currentUid = { "uid-me" },
        ) to repo

    private val bo1 = MatchConfig(
        bestOfSets = 1,
        finalSetMode = FinalSetMode.TIEBREAK_7,
        goldenPoint = false,
        playerNameA = "me",
        playerNameB = "rival",
    )

    private fun MatchSessionViewModel.playToWin(side: Side) {
        repeat(24) { onPoint(side) }
    }

    @Test
    fun `auto-saves once when a match ends naturally`() = runTest {
        val (vm, repo) = buildVm()
        vm.start(bo1, OpponentRef.Registered("uid-rival"))

        vm.playToWin(Side.A)
        advanceUntilIdle()

        assertEquals(1, repo.calls)
        val saved = repo.lastSaved
        assertTrue(saved != null)
        assertEquals("uid-me", saved!!.createdBy)
        assertEquals(listOf("uid-me", "uid-rival"), saved.players)
        assertTrue(vm.saveState.value is MatchSessionViewModel.SaveState.Saved)
    }

    @Test
    fun `does not save twice when undo and replay close the match again`() = runTest {
        val (vm, repo) = buildVm()
        vm.start(bo1, OpponentRef.Guest("rival"))

        vm.playToWin(Side.A)
        advanceUntilIdle()
        assertEquals(1, repo.calls)

        vm.onUndo()
        vm.onPoint(Side.A) // re-cierra
        advanceUntilIdle()

        assertEquals("Should remain at 1 save", 1, repo.calls)
    }

    @Test
    fun `endMatchEarly also triggers save`() = runTest {
        val (vm, repo) = buildVm()
        vm.start(bo1, OpponentRef.Guest("rival"))
        vm.endMatchEarly(Side.B)
        advanceUntilIdle()

        assertEquals(1, repo.calls)
        assertEquals("guest:rival", repo.lastSaved?.winner)
    }

    @Test
    fun `retrySave re-invokes repository after a failure`() = runTest {
        val repo = FakeMatchRepository(failFirst = true)
        val (vm, _) = buildVm(repo)
        vm.start(bo1, OpponentRef.Guest("rival"))
        vm.playToWin(Side.A)
        advanceUntilIdle()

        assertTrue(vm.saveState.value is MatchSessionViewModel.SaveState.Error)
        assertEquals(1, repo.calls)

        vm.retrySave()
        advanceUntilIdle()

        assertEquals(2, repo.calls)
        assertTrue(vm.saveState.value is MatchSessionViewModel.SaveState.Saved)
    }

    @Test
    fun `does not save without a uid`() = runTest {
        val repo = FakeMatchRepository()
        val vm = MatchSessionViewModel(
            matchRepository = repo,
            currentUid = { null },
        )
        vm.start(bo1, OpponentRef.Guest("rival"))
        vm.playToWin(Side.A)
        advanceUntilIdle()

        assertEquals(0, repo.calls)
    }

    private class FakeMatchRepository(
        private val failFirst: Boolean = false,
    ) : MatchRepository {
        var calls: Int = 0
            private set
        var lastSaved: MatchRecord? = null
            private set

        override suspend fun saveCompletedMatch(record: MatchRecord): Result<String> {
            calls += 1
            lastSaved = record
            return if (failFirst && calls == 1) {
                Result.failure(RuntimeException("boom"))
            } else {
                Result.success(record.matchId)
            }
        }
    }
}
