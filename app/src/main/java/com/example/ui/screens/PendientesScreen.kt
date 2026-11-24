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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ClientAccountSummary
import com.example.data.LedgerData
import com.example.data.PaymentModality
import com.example.ui.components.ClientInitialsAvatar
import com.example.ui.components.DietaryNoteBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ModalityPill
import com.example.ui.theme.WhatsAppGreen
import com.example.util.formatCurrency
import com.example.util.formatLunchesCountText
import com.example.util.shareStatementOnWhatsApp

@Composable
fun PendientesScreen(
    ledger: LedgerData,
    onRegisterPayment: (ClientAccountSummary) -> Unit,
    onOpenClientDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedModality by remember { mutableStateOf<PaymentModality?>(null) }

    val pendingAccounts = remember(ledger.accountSummaries, searchQuery, selectedModality) {
        val q = searchQuery.trim().lowercase()
        ledger.accountSummaries
            .filter { it.pendingBalance > 0L }
            .filter { selectedModality == null || it.client.modalityEnum == selectedModality }
            .filter {
                q.isEmpty() ||
                    it.client.fullName.lowercase().contains(q) ||
                    it.client.notes.lowercase().contains(q)
            }
            .sortedByDescending { it.pendingBalance }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "title") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.pending_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Consulta cuánto te debe cada cliente y registra pagos o abonos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Total Receivables Hero Card
        item(key = "hero") {
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
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.pending_hero_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                        Text(
                            text = formatCurrency(ledger.totalReceivables),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val clientsLabel = if (ledger.totalClientsWithDebt == 1) {
                                "1 cliente con saldo"
                            } else {
                                "${ledger.totalClientsWithDebt} clientes con saldo"
                            }
                            val lunchesLabel = if (ledger.totalPendingLunches == 1) {
                                "1 almuerzo pendiente"
                            } else {
                                "${ledger.totalPendingLunches} almuerzos pendientes"
                            }
                            Text(
                                text = "$clientsLabel · $lunchesLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.94f)
                            )
                        }
                    }
                }
            }
        }

        // Search + Modality Filters
        item(key = "filters") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar cliente con saldo…") },
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
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "all") {
                        FilterChip(
                            selected = selectedModality == null,
                            onClick = { selectedModality = null },
                            label = { Text("Todos") },
                            modifier = Modifier.height(42.dp)
                        )
                    }
                    items(PaymentModality.entries, key = { it.name }) { modality ->
                        FilterChip(
                            selected = selectedModality == modality,
                            onClick = {
                                selectedModality = if (selectedModality == modality) null else modality
                            },
                            label = { Text(modality.label) },
                            modifier = Modifier.height(42.dp)
                        )
                    }
                }
            }
        }

        if (pendingAccounts.isEmpty()) {
            item(key = "empty_pending") {
                val hasActiveFilters = searchQuery.isNotBlank() || selectedModality != null
                EmptyStateCard(
                    icon = Icons.Rounded.CheckCircle,
                    title = if (hasActiveFilters) {
                        "Sin resultados con este filtro"
                    } else {
                        stringResource(R.string.pending_empty_title)
                    },
                    subtitle = if (hasActiveFilters) {
                        "Prueba quitando el filtro de modalidad o cambiando el nombre buscado."
                    } else {
                        stringResource(R.string.pending_empty_subtitle)
                    }
                )
            }
        } else {
            items(pendingAccounts, key = { it.client.id }) { summary ->
                PendingAccountCard(
                    summary = summary,
                    onOpenDetail = { onOpenClientDetail(summary.client.id) },
                    onRegisterPayment = { onRegisterPayment(summary) },
                    onWhatsAppClick = { shareStatementOnWhatsApp(context, summary) }
                )
            }
        }
    }
}

@Composable
private fun PendingAccountCard(
    summary: ClientAccountSummary,
    onOpenDetail: () -> Unit,
    onRegisterPayment: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onOpenDetail)
            .testTag("pending_card_${summary.client.id}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    ClientInitialsAvatar(
                        fullName = summary.client.fullName,
                        active = summary.client.activeFlag
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = summary.client.fullName,
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
                            ModalityPill(modality = summary.client.modalityEnum)
                            Text(
                                text = "${formatCurrency(summary.client.agreedPrice)}/alm",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Debt Amount & Pending Lunches Highlight Strip
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saldo pendiente",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatLunchesCountText(
                                summary.pendingLunchesCount,
                                summary.hasPartialLunchBalance
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                        )
                    }
                    Text(
                        text = formatCurrency(summary.pendingBalance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (summary.client.notes.isNotBlank()) {
                DietaryNoteBanner(notes = summary.client.notes, maxLines = 1)
            }

            // Primary Action Buttons: Register Payment + WhatsApp Reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRegisterPayment,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .weight(1.25f)
                        .height(50.dp)
                        .testTag("pay_button_${summary.client.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.pending_action_pay),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onWhatsAppClick,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, WhatsAppGreen),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(50.dp)
                        .testTag("whatsapp_button_${summary.client.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.cd_whatsapp_send),
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.pending_action_whatsapp),
                        style = MaterialTheme.typography.labelLarge,
                        color = WhatsAppGreen
                    )
                }
            }
        }
    }
}
