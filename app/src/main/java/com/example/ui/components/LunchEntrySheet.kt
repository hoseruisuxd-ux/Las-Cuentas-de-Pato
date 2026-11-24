package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Client
import com.example.data.LunchEntry
import com.example.util.formatCurrency
import com.example.util.formatFriendlyDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunchEntrySheet(
    clients: List<Client>,
    initialClient: Client?,
    editingEntry: LunchEntry?,
    initialDateEpochDay: Long,
    isSaving: Boolean,
    onSave: (
        client: Client,
        dateEpochDay: Long,
        quantity: Long,
        unitPrice: Long,
        note: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeClients = remember(clients) {
        val active = clients.filter { it.activeFlag }
        if (active.isNotEmpty()) active else clients
    }

    val initialSelected = remember(editingEntry?.id, initialClient?.id, clients) {
        when {
            editingEntry != null -> clients.firstOrNull { it.id == editingEntry.clientId }
                ?: Client(
                    id = editingEntry.clientId,
                    fullName = editingEntry.clientNameSnapshot,
                    agreedPrice = editingEntry.unitPrice
                )
            initialClient != null -> initialClient
            else -> activeClients.firstOrNull()
        }
    }

    var selectedClient by remember(editingEntry?.id, initialClient?.id) {
        mutableStateOf(initialSelected)
    }
    var clientQuery by remember { mutableStateOf("") }
    var dateEpochDay by remember(editingEntry?.id, initialDateEpochDay) {
        mutableLongStateOf(editingEntry?.dateEpochDay ?: initialDateEpochDay)
    }
    var quantity by remember(editingEntry?.id) {
        mutableLongStateOf(editingEntry?.quantity ?: 1L)
    }
    var unitPriceText by remember(editingEntry?.id, initialSelected?.id) {
        mutableStateOf(
            (editingEntry?.unitPrice ?: initialSelected?.agreedPrice ?: 15000L).toString()
        )
    }
    var note by remember(editingEntry?.id) {
        mutableStateOf(editingEntry?.note ?: "")
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val parsedPrice = unitPriceText.toLongOrNull() ?: 0L
    val computedTotal = quantity * parsedPrice

    val filteredClients = remember(clients, clientQuery) {
        val q = clientQuery.trim().lowercase()
        val base = if (q.isEmpty()) activeClients else clients.filter {
            it.fullName.lowercase().contains(q) || it.notes.lowercase().contains(q)
        }
        base
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (editingEntry == null) "Registrar almuerzo" else "Corregir almuerzo",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Client Selector (locked when editing an existing entry to match immutable clientId)
            if (editingEntry != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClientInitialsAvatar(fullName = selectedClient?.fullName ?: editingEntry.clientNameSnapshot)
                        Column {
                            Text(
                                text = "Cliente",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedClient?.fullName ?: editingEntry.clientNameSnapshot,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Selecciona el cliente",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (clients.size > 5) {
                        OutlinedTextField(
                            value = clientQuery,
                            onValueChange = { clientQuery = it },
                            placeholder = { Text("Buscar cliente…") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(filteredClients, key = { it.id }) { client ->
                            val isSelected = selectedClient?.id == client.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedClient = client
                                    unitPriceText = client.agreedPrice.toString()
                                    validationError = null
                                },
                                label = {
                                    Text(
                                        text = client.fullName,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                modifier = Modifier.height(44.dp)
                            )
                        }
                    }
                }
            }

            // Highlight client's dietary preferences right where Pato needs them!
            val clientNotes = selectedClient?.notes.orEmpty()
            if (clientNotes.isNotBlank()) {
                DietaryNoteBanner(notes = "Preferencia: $clientNotes", maxLines = 3)
            }

            // Date selector
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Fecha del almuerzo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatFriendlyDate(dateEpochDay),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = "Cambiar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quantity tactile stepper
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Cantidad de almuerzos",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = if (quantity == 1L) "1 almuerzo" else "$quantity almuerzos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (quantity > 1L) quantity -= 1L },
                            enabled = quantity > 1L,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("qty_decrease_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = stringResource(R.string.cd_decrease_qty)
                            )
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FilledTonalIconButton(
                            onClick = { if (quantity < 500L) quantity += 1L },
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("qty_increase_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.cd_increase_qty)
                            )
                        }
                    }
                }
            }

            // Unit price (auto-filled from client, editable if needed)
            OutlinedTextField(
                value = unitPriceText,
                onValueChange = { input ->
                    unitPriceText = input.filter { it.isDigit() }.take(8)
                    validationError = null
                },
                label = { Text("Precio por almuerzo") },
                prefix = { Text("$ ") },
                leadingIcon = { Icon(Icons.Rounded.Payments, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lunch_price_input")
            )

            // Optional daily note
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 250) note = it },
                label = { Text("Nota de hoy (opcional)") },
                placeholder = { Text("Ej. Con jugo extra o sin sopa") },
                leadingIcon = { Icon(Icons.Rounded.Notes, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Live Auto-Calculated Total Banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total del registro",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$quantity × ${formatCurrency(parsedPrice)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Text(
                        text = formatCurrency(computedTotal),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    val client = selectedClient
                    when {
                        client == null || client.id.isBlank() -> {
                            validationError = "Selecciona un cliente para registrar el almuerzo."
                        }
                        parsedPrice <= 0L -> {
                            validationError = "Ingresa un precio válido mayor a $0."
                        }
                        else -> {
                            onSave(client, dateEpochDay, quantity, parsedPrice, note.trim())
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_lunch_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = if (editingEntry == null) "Guardar almuerzo" else "Guardar corrección",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    if (showDatePicker) {
        WarmDatePickerDialog(
            initialEpochDay = dateEpochDay,
            onDateSelected = { dateEpochDay = it },
            onDismiss = { showDatePicker = false }
        )
    }
}
