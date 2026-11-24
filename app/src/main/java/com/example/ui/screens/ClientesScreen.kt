package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Client
import com.example.data.ClientAccountSummary
import com.example.data.LedgerData
import com.example.ui.components.ClientInitialsAvatar
import com.example.ui.components.DietaryNoteBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ModalityPill
import com.example.ui.components.StatusPill
import com.example.util.formatCurrency

private enum class ClientFilterMode(val label: String) {
    ACTIVOS("Activos"),
    INACTIVOS("Inactivos"),
    TODOS("Todos")
}

@Composable
fun ClientesScreen(
    ledger: LedgerData,
    onAddClient: () -> Unit,
    onEditClient: (Client) -> Unit,
    onToggleClientStatus: (Client) -> Unit,
    onQuickAddLunch: (Client) -> Unit,
    onOpenClientDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(ClientFilterMode.ACTIVOS) }

    val activeCount = remember(ledger.clients) { ledger.clients.count { it.activeFlag } }
    val inactiveCount = remember(ledger.clients) { ledger.clients.count { !it.activeFlag } }

    val filteredSummaries = remember(ledger.accountSummaries, searchQuery, filterMode) {
        val q = searchQuery.trim().lowercase()
        ledger.accountSummaries
            .filter { summary ->
                when (filterMode) {
                    ClientFilterMode.ACTIVOS -> summary.client.activeFlag
                    ClientFilterMode.INACTIVOS -> !summary.client.activeFlag
                    ClientFilterMode.TODOS -> true
                }
            }
            .filter { summary ->
                q.isEmpty() ||
                    summary.client.fullName.lowercase().contains(q) ||
                    summary.client.phone.lowercase().contains(q) ||
                    summary.client.address.lowercase().contains(q) ||
                    summary.client.notes.lowercase().contains(q)
            }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.clients_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$activeCount activos · $inactiveCount inactivos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "search_and_filter") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.clients_search_hint)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clients_search_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ClientFilterMode.entries, key = { it.name }) { mode ->
                        val count = when (mode) {
                            ClientFilterMode.ACTIVOS -> activeCount
                            ClientFilterMode.INACTIVOS -> inactiveCount
                            ClientFilterMode.TODOS -> ledger.clients.size
                        }
                        FilterChip(
                            selected = filterMode == mode,
                            onClick = { filterMode = mode },
                            label = { Text("${mode.label} ($count)") },
                            modifier = Modifier.height(42.dp)
                        )
                    }
                }
            }
        }

        if (filteredSummaries.isEmpty()) {
            item(key = "empty_clients") {
                val isGlobalEmpty = ledger.clients.isEmpty()
                EmptyStateCard(
                    icon = Icons.Rounded.People,
                    title = if (isGlobalEmpty) {
                        stringResource(R.string.clients_empty_title)
                    } else {
                        "No se encontraron clientes"
                    },
                    subtitle = if (isGlobalEmpty) {
                        stringResource(R.string.clients_empty_subtitle)
                    } else {
                        "Prueba cambiando el filtro o registra un nuevo cliente."
                    },
                    actionLabel = stringResource(R.string.clients_add),
                    onActionClick = onAddClient
                )
            }
        } else {
            items(filteredSummaries, key = { it.client.id }) { summary ->
                ClientDirectoryCard(
                    summary = summary,
                    onOpenDetail = { onOpenClientDetail(summary.client.id) },
                    onEdit = { onEditClient(summary.client) },
                    onToggleStatus = { onToggleClientStatus(summary.client) },
                    onAddLunch = { onQuickAddLunch(summary.client) }
                )
            }
        }
    }
}

@Composable
private fun ClientDirectoryCard(
    summary: ClientAccountSummary,
    onOpenDetail: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onAddLunch: () -> Unit
) {
    val client = summary.client
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenDetail)
            .testTag("client_card_${client.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ClientInitialsAvatar(fullName = client.fullName, active = client.activeFlag)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = client.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModalityPill(modality = client.modalityEnum)
                            Text(
                                text = formatCurrency(client.agreedPrice),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Balance status pill
                when {
                    summary.pendingBalance > 0L -> StatusPill(
                        text = "Debe ${formatCurrency(summary.pendingBalance)}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    summary.creditBalance > 0L -> StatusPill(
                        text = "A favor ${formatCurrency(summary.creditBalance)}",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    else -> StatusPill(
                        text = "Al día",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (client.phone.isNotBlank() || client.address.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (client.phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = client.phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (client.address.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = client.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (client.notes.isNotBlank()) {
                DietaryNoteBanner(notes = client.notes, maxLines = 2)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleStatus) {
                    Text(
                        text = if (client.activeFlag) "Desactivar" else "Activar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (client.activeFlag) {
                        TextButton(onClick = onAddLunch) {
                            Icon(
                                imageVector = Icons.Rounded.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Almuerzo", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
