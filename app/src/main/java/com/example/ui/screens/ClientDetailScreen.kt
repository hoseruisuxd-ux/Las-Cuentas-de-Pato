package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Client
import com.example.data.ClientAccountSummary
import com.example.data.LunchEntry
import com.example.data.PaymentRecord
import com.example.ui.components.ClientInitialsAvatar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DietaryNoteBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ModalityPill
import com.example.ui.theme.WhatsAppGreen
import com.example.util.formatCurrency
import com.example.util.formatLunchesCountText
import com.example.util.shareStatementOnWhatsApp

private enum class DetailHistoryTab(val label: String) {
    ALMUERZOS("Almuerzos"),
    PAGOS("Pagos y abonos")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    summary: ClientAccountSummary,
    onBack: () -> Unit,
    onEditClient: (Client) -> Unit,
    onAddLunch: (Client) -> Unit,
    onRegisterPayment: (ClientAccountSummary) -> Unit,
    onEditLunch: (LunchEntry) -> Unit,
    onDeleteLunch: (LunchEntry) -> Unit,
    onEditPayment: (PaymentRecord) -> Unit,
    onDeletePayment: (PaymentRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val client = summary.client
    var selectedTab by remember { mutableStateOf(DetailHistoryTab.ALMUERZOS) }
    var lunchPendingDelete by remember { mutableStateOf<LunchEntry?>(null) }
    var paymentPendingDelete by remember { mutableStateOf<PaymentRecord?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cuenta de ${client.fullName.substringBefore(" ")}",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClient(client) }) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile + Balance Summary Card
            item(key = "profile_card") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ClientInitialsAvatar(fullName = client.fullName, active = client.activeFlag)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = client.fullName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ModalityPill(modality = client.modalityEnum)
                                    Text(
                                        text = "${formatCurrency(client.agreedPrice)} por almuerzo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (client.phone.isNotBlank() || client.address.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (client.phone.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Phone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = client.phone,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                                if (client.address.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = client.address,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }

                        if (client.notes.isNotBlank()) {
                            DietaryNoteBanner(notes = "Preferencias: ${client.notes}", maxLines = 4)
                        }

                        // Balance box
                        val isOwed = summary.pendingBalance > 0L
                        val balanceBg = if (isOwed) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                        val balanceFg = if (isOwed) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                        Surface(
                            color = balanceBg,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = when {
                                        isOwed -> "Saldo pendiente por pagar"
                                        summary.creditBalance > 0L -> "Saldo a favor del cliente"
                                        else -> "Estado de cuenta"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = balanceFg
                                )
                                Text(
                                    text = when {
                                        isOwed -> formatCurrency(summary.pendingBalance)
                                        summary.creditBalance > 0L -> formatCurrency(summary.creditBalance)
                                        else -> "Al día ($0)"
                                    },
                                    style = MaterialTheme.typography.displaySmall,
                                    color = balanceFg
                                )
                                if (isOwed) {
                                    Text(
                                        text = formatLunchesCountText(
                                            summary.pendingLunchesCount,
                                            summary.hasPartialLunchBalance
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = balanceFg.copy(alpha = 0.9f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Total consumido: ${formatCurrency(summary.totalCharged)} · Total pagado: ${formatCurrency(summary.totalPaid)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = balanceFg.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Primary Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onAddLunch(client) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("detail_add_lunch_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Almuerzo")
                            }
                            Button(
                                onClick = { onRegisterPayment(summary) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("detail_pay_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pago / Abono")
                            }
                        }

                        OutlinedButton(
                            onClick = { shareStatementOnWhatsApp(context, summary) },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.2.dp, WhatsAppGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("detail_whatsapp_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.cd_whatsapp_send),
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enviar resumen de cuenta por WhatsApp",
                                color = WhatsAppGreen,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // History Tabs
            item(key = "history_tabs") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DetailHistoryTab.entries.forEachIndexed { index, tab ->
                        val count = if (tab == DetailHistoryTab.ALMUERZOS) {
                            summary.entries.size
                        } else {
                            summary.payments.size
                        }
                        SegmentedButton(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = DetailHistoryTab.entries.size
                            ),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("${tab.label} ($count)")
                        }
                    }
                }
            }

            if (selectedTab == DetailHistoryTab.ALMUERZOS) {
                if (summary.entries.isEmpty()) {
                    item(key = "empty_client_lunches") {
                        EmptyStateCard(
                            icon = Icons.Rounded.Restaurant,
                            title = "Sin almuerzos registrados",
                            subtitle = "Anota el primer almuerzo de ${client.fullName} y se sumará automáticamente a su cuenta."
                        )
                    }
                } else {
                    items(summary.entries, key = { it.id }) { entry ->
                        LunchHistoryRowCard(
                            entry = entry,
                            onEdit = { onEditLunch(entry) },
                            onDelete = { lunchPendingDelete = entry }
                        )
                    }
                }
            } else {
                if (summary.payments.isEmpty()) {
                    item(key = "empty_client_payments") {
                        EmptyStateCard(
                            icon = Icons.Rounded.ReceiptLong,
                            title = "Sin pagos registrados",
                            subtitle = "Los pagos totales y abonos parciales de ${client.fullName} aparecerán aquí."
                        )
                    }
                } else {
                    items(summary.payments, key = { it.id }) { payment ->
                        PaymentHistoryRowCard(
                            payment = payment,
                            onEdit = { onEditPayment(payment) },
                            onDelete = { paymentPendingDelete = payment }
                        )
                    }
                }
            }
        }
    }

    lunchPendingDelete?.let { entry ->
        ConfirmDeleteDialog(
            title = "¿Eliminar este almuerzo?",
            message = "Se eliminarán ${entry.quantity} almuerzo(s) por ${formatCurrency(entry.totalAmount)} y el saldo se recalculará al instante.",
            onConfirm = {
                onDeleteLunch(entry)
                lunchPendingDelete = null
            },
            onDismiss = { lunchPendingDelete = null }
        )
    }

    paymentPendingDelete?.let { payment ->
        ConfirmDeleteDialog(
            title = "¿Eliminar este pago?",
            message = "Se eliminará el pago de ${formatCurrency(payment.amount)} (${payment.methodEnum.label}) y el saldo se recalculará al instante.",
            onConfirm = {
                onDeletePayment(payment)
                paymentPendingDelete = null
            },
            onDismiss = { paymentPendingDelete = null }
        )
    }
}
