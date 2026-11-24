package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Client
import com.example.data.LedgerCalculator
import com.example.data.LedgerData
import com.example.data.LunchEntry
import com.example.ui.components.ClientInitialsAvatar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DietaryNoteBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.WarmDatePickerDialog
import com.example.util.formatCurrency
import com.example.util.formatFriendlyDate
import com.example.util.todayEpochDay

@Composable
fun HoyScreen(
    ledger: LedgerData,
    selectedDateEpochDay: Long,
    onSelectDate: (Long) -> Unit,
    onShiftDate: (Long) -> Unit,
    onJumpToday: () -> Unit,
    onQuickAddLunch: (Client?) -> Unit,
    onEditLunch: (LunchEntry) -> Unit,
    onDeleteLunch: (LunchEntry) -> Unit,
    onOpenPendingTab: () -> Unit,
    onAddFirstClient: () -> Unit,
    onOpenClientDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { todayEpochDay() }
    val dailySummary = remember(ledger, selectedDateEpochDay) {
        LedgerCalculator.computeDailySummary(ledger, selectedDateEpochDay)
    }
    val dayEntries = remember(ledger.lunchEntries, selectedDateEpochDay) {
        ledger.lunchEntries.filter { it.dateEpochDay == selectedDateEpochDay }
    }
    val activeClients = remember(ledger.clients) {
        ledger.clients.filter { it.activeFlag }
    }
    val clientsById = remember(ledger.clients) {
        ledger.clients.associateBy { it.id }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var entryPendingDelete by remember { mutableStateOf<LunchEntry?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Brand & Date Header
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Control diario de cocina y almuerzos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedDateEpochDay != today) {
                        AssistChip(
                            onClick = onJumpToday,
                            label = { Text("Ir a hoy", fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Date Navigator Pill
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { onShiftDate(-1L) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.cd_previous_day)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = stringResource(R.string.cd_pick_date),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = formatFriendlyDate(selectedDateEpochDay),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { onShiftDate(1L) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.cd_next_day)
                            )
                        }
                    }
                }
            }
        }

        // Daily Summary Hero Card
        item(key = "daily_summary") {
            Surface(
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.86f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.today_summary_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = dailySummary.lunchesCount.toString(),
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text(
                                        text = if (dailySummary.lunchesCount == 1L) "almuerzo" else "almuerzos",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                                        modifier = Modifier.padding(bottom = 5.dp)
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenPendingTab() }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Por cobrar total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = formatCurrency(dailySummary.totalReceivables),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryMetricPill(
                                label = "Facturado el día",
                                value = formatCurrency(dailySummary.billedAmount),
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricPill(
                                label = "Cobrado el día",
                                value = formatCurrency(dailySummary.collectedAmount),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Quick-Tap Active Clients Section
        if (activeClients.isNotEmpty()) {
            item(key = "quick_register") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Toca un cliente para anotar rápido",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeClients, key = { it.id }) { client ->
                            val hasOrderedToday = dayEntries.any { it.clientId == client.id }
                            QuickClientChip(
                                client = client,
                                orderedOnDate = hasOrderedToday,
                                onClick = { onQuickAddLunch(client) }
                            )
                        }
                    }
                }
            }
        }

        // Day's Lunch Entries Header
        item(key = "entries_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.today_lunches_list_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (dayEntries.isNotEmpty()) {
                    Text(
                        text = "${dayEntries.size} reg.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Empty State or Entries List
        if (ledger.clients.isEmpty()) {
            item(key = "no_clients") {
                EmptyStateCard(
                    icon = Icons.Rounded.PersonAdd,
                    title = "Comienza agregando tus clientes",
                    subtitle = stringResource(R.string.today_no_clients_hint),
                    actionLabel = stringResource(R.string.today_go_to_clients),
                    onActionClick = onAddFirstClient
                )
            }
        } else if (dayEntries.isEmpty()) {
            item(key = "empty_day") {
                EmptyStateCard(
                    icon = Icons.Rounded.Restaurant,
                    title = stringResource(R.string.today_empty_title),
                    subtitle = stringResource(R.string.today_empty_subtitle),
                    actionLabel = stringResource(R.string.today_add_lunch),
                    onActionClick = { onQuickAddLunch(null) }
                )
            }
        } else {
            items(dayEntries, key = { it.id }) { entry ->
                val client = clientsById[entry.clientId]
                LunchEntryCard(
                    entry = entry,
                    clientNotes = client?.notes.orEmpty(),
                    onOpenClient = {
                        if (client != null) onOpenClientDetail(client.id)
                    },
                    onEdit = { onEditLunch(entry) },
                    onDelete = { entryPendingDelete = entry }
                )
            }
        }
    }

    if (showDatePicker) {
        WarmDatePickerDialog(
            initialEpochDay = selectedDateEpochDay,
            onDateSelected = onSelectDate,
            onDismiss = { showDatePicker = false }
        )
    }

    entryPendingDelete?.let { entry ->
        ConfirmDeleteDialog(
            title = "¿Eliminar este almuerzo?",
            message = "Se eliminarán ${entry.quantity} almuerzo(s) de ${entry.clientNameSnapshot} por ${formatCurrency(entry.totalAmount)} y su cuenta se recalculará automáticamente.",
            onConfirm = {
                onDeleteLunch(entry)
                entryPendingDelete = null
            },
            onDismiss = { entryPendingDelete = null }
        )
    }
}

@Composable
private fun SummaryMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun QuickClientChip(
    client: Client,
    orderedOnDate: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (orderedOnDate) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = if (orderedOnDate) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("quick_client_${client.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (orderedOnDate) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = if (orderedOnDate) MaterialTheme.colorScheme.onSecondary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = client.fullName.substringBefore(" "),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatCurrency(client.agreedPrice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LunchEntryCard(
    entry: LunchEntry,
    clientNotes: String,
    onOpenClient: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenClient)
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
                    ClientInitialsAvatar(fullName = entry.clientNameSnapshot)
                    Column {
                        Text(
                            text = entry.clientNameSnapshot,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val qtyLabel = if (entry.quantity == 1L) "1 almuerzo" else "${entry.quantity} almuerzos"
                        Text(
                            text = "$qtyLabel · ${formatCurrency(entry.unitPrice)} c/u",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = formatCurrency(entry.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (clientNotes.isNotBlank()) {
                DietaryNoteBanner(notes = clientNotes, maxLines = 2)
            }

            if (entry.note.isNotBlank()) {
                Text(
                    text = "Nota del día: ${entry.note}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
