package com.chemecador.tennistracker.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chemecador.tennistracker.data.profile.UserProfile
import com.chemecador.tennistracker.scoring.FinalSetMode
import com.chemecador.tennistracker.scoring.MatchConfig

sealed interface OpponentSelection {
    data object None : OpponentSelection
    data class Registered(val profile: UserProfile) : OpponentSelection
    data class Guest(val name: String) : OpponentSelection
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupMatchScreen(
    myUid: String,
    myProfile: UserProfile?,
    onStart: (MatchConfig) -> Unit,
    onOpenProfile: () -> Unit,
) {
    var bestOfSets by remember { mutableStateOf(3) }
    var finalSetMode by remember { mutableStateOf(FinalSetMode.TIEBREAK_7) }
    var goldenPoint by remember { mutableStateOf(false) }
    var advancedOptions by remember { mutableStateOf(false) }
    var guestNameA by remember { mutableStateOf("") }
    var guestNameB by remember { mutableStateOf("") }
    var opponent by remember { mutableStateOf<OpponentSelection>(OpponentSelection.None) }

    val isAnonymous = myProfile == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo partido") },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Perfil",
                        )
                    }
                },
            )
        },
    ) { padding ->
        SetupContent(
            contentPadding = padding,
            myUid = myUid,
            myProfile = myProfile,
            guestNameA = guestNameA,
            guestNameB = guestNameB,
            opponent = opponent,
            bestOfSets = bestOfSets,
            finalSetMode = finalSetMode,
            goldenPoint = goldenPoint,
            advancedOptions = advancedOptions,
            onGuestNameAChange = { guestNameA = it },
            onGuestNameBChange = { guestNameB = it },
            onOpponentChange = { opponent = it },
            onBestOfChange = { bestOfSets = it },
            onFinalSetModeChange = { finalSetMode = it },
            onGoldenPointChange = { goldenPoint = it },
            onAdvancedOptionsChange = { advancedOptions = it },
            onStart = {
                val playerA = if (isAnonymous) {
                    guestNameA.ifBlank { "Invitado 1" }
                } else {
                    myProfile.username
                }
                val playerB = if (isAnonymous) {
                    guestNameB.ifBlank { "Invitado 2" }
                } else {
                    when (val sel = opponent) {
                        OpponentSelection.None -> "Rival"
                        is OpponentSelection.Registered -> sel.profile.username
                        is OpponentSelection.Guest -> sel.name.ifBlank { "Invitado" }
                    }
                }
                onStart(
                    MatchConfig(
                        bestOfSets = bestOfSets,
                        finalSetMode = finalSetMode,
                        goldenPoint = goldenPoint,
                        playerNameA = playerA,
                        playerNameB = playerB,
                    )
                )
            },
        )
    }
}

@Composable
private fun SetupContent(
    contentPadding: PaddingValues,
    myUid: String,
    myProfile: UserProfile?,
    guestNameA: String,
    guestNameB: String,
    opponent: OpponentSelection,
    bestOfSets: Int,
    finalSetMode: FinalSetMode,
    goldenPoint: Boolean,
    advancedOptions: Boolean,
    onGuestNameAChange: (String) -> Unit,
    onGuestNameBChange: (String) -> Unit,
    onOpponentChange: (OpponentSelection) -> Unit,
    onBestOfChange: (Int) -> Unit,
    onFinalSetModeChange: (FinalSetMode) -> Unit,
    onGoldenPointChange: (Boolean) -> Unit,
    onAdvancedOptionsChange: (Boolean) -> Unit,
    onStart: () -> Unit,
) {
    val scroll = rememberScrollState()
    var showOpponentPicker by remember { mutableStateOf(false) }
    val isAnonymous = myProfile == null
    val canStart = isAnonymous || opponent !is OpponentSelection.None

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionLabel("Jugadores")

        if (myProfile != null) {
            PlayerCard(
                label = "Tú",
                primary = myProfile.displayName,
                secondary = "@${myProfile.username}",
            )
        } else {
            OutlinedTextField(
                value = guestNameA,
                onValueChange = onGuestNameAChange,
                label = { Text("Tu nombre (invitado)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (myProfile != null) {
            OpponentPickerCard(
                opponent = opponent,
                onClick = { showOpponentPicker = true },
            )
        } else {
            OutlinedTextField(
                value = guestNameB,
                onValueChange = onGuestNameBChange,
                label = { Text("Rival (invitado)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Opciones avanzadas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Switch(checked = advancedOptions, onCheckedChange = onAdvancedOptionsChange)
        }

        if (advancedOptions) {
            SectionLabel("Sets a jugar")
            ChipRow(
                options = listOf(1, 3, 5),
                selected = bestOfSets,
                label = { "Al mejor de $it" },
                onSelect = onBestOfChange,
            )

            SectionLabel("Set decisivo")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FinalSetMode.entries.forEach { mode ->
                    FilterChip(
                        selected = finalSetMode == mode,
                        onClick = { onFinalSetModeChange(mode) },
                        label = { Text(mode.displayName()) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SectionLabel("Punto de oro")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(if (goldenPoint) "Activado" else "Desactivado")
                Switch(checked = goldenPoint, onCheckedChange = onGoldenPointChange)
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onStart,
            enabled = canStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Empezar")
        }
    }

    if (showOpponentPicker && myProfile != null) {
        OpponentPickerSheet(
            myUid = myUid,
            current = opponent,
            onSelect = {
                onOpponentChange(it)
                showOpponentPicker = false
            },
            onDismiss = { showOpponentPicker = false },
        )
    }
}

@Composable
private fun PlayerCard(label: String, primary: String, secondary: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(initial = primary.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!secondary.isNullOrBlank()) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OpponentPickerCard(
    opponent: OpponentSelection,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val initial: String
            val label: String
            val primary: String
            val secondary: String?
            when (opponent) {
                OpponentSelection.None -> {
                    initial = "?"
                    label = "Rival"
                    primary = "Selecciona un rival"
                    secondary = "Amigo, búsqueda o invitado"
                }
                is OpponentSelection.Registered -> {
                    initial = opponent.profile.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    label = "Rival"
                    primary = opponent.profile.displayName
                    secondary = "@${opponent.profile.username}"
                }
                is OpponentSelection.Guest -> {
                    initial = opponent.name.firstOrNull()?.uppercaseChar()?.toString() ?: "I"
                    label = "Rival"
                    primary = opponent.name.ifBlank { "Invitado" }
                    secondary = "Invitado"
                }
            }
            Avatar(initial = initial)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (secondary.isNotBlank()) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Cambiar rival",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Avatar(initial: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpponentPickerSheet(
    myUid: String,
    current: OpponentSelection,
    onSelect: (OpponentSelection) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel: SetupMatchViewModel = viewModel(key = "setup-match-$myUid") {
        SetupMatchViewModel(myUid = myUid)
    }
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()

    val initialTab = if (current is OpponentSelection.Guest) PickerTab.GUEST else PickerTab.FRIEND
    var tab by remember { mutableStateOf(initialTab) }
    var searchInput by remember { mutableStateOf("") }
    var guestInput by remember {
        mutableStateOf(if (current is OpponentSelection.Guest) current.name else "")
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetSearch()
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Elegir rival",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = tab == PickerTab.FRIEND,
                    onClick = { tab = PickerTab.FRIEND },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Amigo / Buscar") }
                SegmentedButton(
                    selected = tab == PickerTab.GUEST,
                    onClick = { tab = PickerTab.GUEST },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Invitado") }
            }

            when (tab) {
                PickerTab.FRIEND -> FriendPickerContent(
                    friends = friends,
                    searchInput = searchInput,
                    searchResult = searchResult,
                    onSearchInputChange = {
                        searchInput = it
                        if (it.isBlank()) viewModel.resetSearch()
                    },
                    onSearch = { viewModel.search(searchInput) },
                    onPick = { profile ->
                        viewModel.resetSearch()
                        onSelect(OpponentSelection.Registered(profile))
                    },
                )

                PickerTab.GUEST -> GuestPickerContent(
                    name = guestInput,
                    onNameChange = { guestInput = it },
                    onConfirm = {
                        onSelect(OpponentSelection.Guest(guestInput.trim().ifBlank { "Invitado" }))
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private enum class PickerTab { FRIEND, GUEST }

@Composable
private fun FriendPickerContent(
    friends: List<UserProfile>,
    searchInput: String,
    searchResult: OpponentSearchResult,
    onSearchInputChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (UserProfile) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = onSearchInputChange,
                label = { Text("Buscar por @username") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onSearch,
                enabled = searchInput.isNotBlank() && searchResult !is OpponentSearchResult.Searching,
            ) {
                Text("Buscar")
            }
        }

        SearchStatus(result = searchResult, onPick = onPick)

        if (friends.isEmpty()) {
            Text(
                text = "Aún no tienes amigos. Búscalos por @username o márcalos como invitado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "Tus amigos",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(friends, key = { it.uid }) { friend ->
                    FriendOptionCard(profile = friend, onClick = { onPick(friend) })
                }
            }
        }
    }
}

@Composable
private fun SearchStatus(
    result: OpponentSearchResult,
    onPick: (UserProfile) -> Unit,
) {
    when (result) {
        OpponentSearchResult.Idle -> Unit
        OpponentSearchResult.Searching -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(8.dp))
            Text("Buscando...")
        }

        OpponentSearchResult.NotFound -> Text(
            text = "No existe ningún usuario con ese username.",
            color = MaterialTheme.colorScheme.error,
        )

        OpponentSearchResult.SelfNotAllowed -> Text(
            text = "No puedes jugar contra ti mismo.",
            color = MaterialTheme.colorScheme.error,
        )

        is OpponentSearchResult.Found -> FriendOptionCard(
            profile = result.profile,
            onClick = { onPick(result.profile) },
        )

        is OpponentSearchResult.Error -> Text(
            text = result.message,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun FriendOptionCard(profile: UserProfile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(initial = profile.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "@${profile.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GuestPickerContent(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nombre del invitado") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Usar este invitado")
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun FinalSetMode.displayName(): String = when (this) {
    FinalSetMode.TIEBREAK_7 -> "Tie-break a 7"
    FinalSetMode.SUPER_TIEBREAK_10 -> "Super tie-break a 10"
    FinalSetMode.ADVANTAGE -> "Ventaja (sin tie-break)"
}
