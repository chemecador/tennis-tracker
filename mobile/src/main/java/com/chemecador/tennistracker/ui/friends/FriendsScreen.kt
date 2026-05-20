package com.chemecador.tennistracker.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    myUid: String,
    onBack: () -> Unit,
) {
    val viewModel: FriendsViewModel = viewModel(key = "friends-$myUid") {
        FriendsViewModel(myUid = myUid)
    }
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val addResult by viewModel.addResult.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<FriendItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amigos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.resetAddResult()
                showAddDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir amigo")
            }
        },
    ) { padding ->
        if (ui.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (ui.incoming.isEmpty() && ui.outgoing.isEmpty() && ui.accepted.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (ui.incoming.isNotEmpty()) {
                    item { SectionHeader("Solicitudes recibidas") }
                    items(ui.incoming, key = { it.friendship.id }) { item ->
                        IncomingCard(
                            item = item,
                            onAccept = { viewModel.accept(item.friendship.id) },
                            onReject = { viewModel.delete(item.friendship.id) },
                        )
                    }
                }
                if (ui.accepted.isNotEmpty()) {
                    item { SectionHeader("Mis amigos") }
                    items(ui.accepted, key = { it.friendship.id }) { item ->
                        AcceptedCard(
                            item = item,
                            onRemove = { pendingDelete = item },
                        )
                    }
                }
                if (ui.outgoing.isNotEmpty()) {
                    item { SectionHeader("Pendientes enviadas") }
                    items(ui.outgoing, key = { it.friendship.id }) { item ->
                        OutgoingCard(
                            item = item,
                            onCancel = { viewModel.delete(item.friendship.id) },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFriendDialog(
            result = addResult,
            onSearch = viewModel::searchUsername,
            onSend = viewModel::sendRequest,
            onDismiss = {
                showAddDialog = false
                viewModel.resetAddResult()
            },
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar amigo") },
            text = {
                Text("¿Seguro que quieres eliminar a @${item.other?.username ?: "este usuario"}?")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item.friendship.id)
                    pendingDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Todavía no tienes amigos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Pulsa el botón \"+\" para buscar a alguien por su username.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun FriendRow(
    item: FriendItem,
    trailing: @Composable () -> Unit,
) {
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
            AvatarSmall(initial = item.other?.username?.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.other?.displayName ?: "(perfil no disponible)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.other?.let { "@${it.username}" } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailing()
        }
    }
}

@Composable
private fun AvatarSmall(initial: String) {
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

@Composable
private fun IncomingCard(
    item: FriendItem,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    FriendRow(item) {
        IconButton(onClick = onAccept) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Aceptar",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onReject) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Rechazar",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AcceptedCard(
    item: FriendItem,
    onRemove: () -> Unit,
) {
    FriendRow(item) {
        Column(horizontalAlignment = Alignment.End) {
            item.other?.let { profile ->
                Text(
                    text = "${profile.eloTennis} / ${profile.eloPadel}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "tenis / pádel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun OutgoingCard(
    item: FriendItem,
    onCancel: () -> Unit,
) {
    FriendRow(item) {
        TextButton(onClick = onCancel) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun AddFriendDialog(
    result: AddFriendResult,
    onSearch: (String) -> Unit,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buscar amigo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AddFriendStatus(result = result)
            }
        },
        confirmButton = {
            when (result) {
                is AddFriendResult.Found -> TextButton(onClick = { onSend(result.profile.uid) }) {
                    Text("Enviar solicitud")
                }

                AddFriendResult.Sent -> TextButton(onClick = onDismiss) { Text("Cerrar") }

                else -> TextButton(
                    onClick = { onSearch(input) },
                    enabled = input.isNotBlank() &&
                        result !is AddFriendResult.Searching &&
                        result !is AddFriendResult.Sending,
                ) {
                    Text("Buscar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun AddFriendStatus(result: AddFriendResult) {
    when (result) {
        AddFriendResult.Idle -> Unit
        AddFriendResult.Searching -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(8.dp))
            Text("Buscando...")
        }

        AddFriendResult.Sending -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(8.dp))
            Text("Enviando...")
        }

        AddFriendResult.NotFound -> Text(
            text = "No existe ningún usuario con ese username.",
            color = MaterialTheme.colorScheme.error,
        )

        AddFriendResult.SelfNotAllowed -> Text(
            text = "Ese eres tú.",
            color = MaterialTheme.colorScheme.error,
        )

        is AddFriendResult.Found -> Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarSmall(initial = result.profile.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        text = result.profile.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "@${result.profile.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        AddFriendResult.Sent -> Text(
            text = "Solicitud enviada.",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )

        AddFriendResult.AlreadyExists -> Text(
            text = "Ya hay una solicitud o amistad con este usuario.",
            color = MaterialTheme.colorScheme.error,
        )

        is AddFriendResult.Error -> Text(
            text = result.message,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
