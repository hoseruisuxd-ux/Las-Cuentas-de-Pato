package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LedgerData
import com.example.data.LunchEntry
import com.example.data.PaymentMethod
import com.example.data.PaymentRecord
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.paymentMethodIcon
import com.example.util.formatCurrency
import com.example.util.formatShortDate
import com.example.util.todayEpochDay

private enum class SummaryPeriod(val label: String, val daysBack: Long?) {
    HOY("Hoy", 0L),
    SEMANA("Últimos 7 días", 6L),
    MES("Últimos 30 días", 29L),
    TODO("Todo", null)
}

private enum class ActivityTab(val label: String) {
    PAGOS("Pagos"),
    ALMUERZOS("Almuerzos")
}

@Composable
fun ResumenScreen(
    ledger: LedgerData,
    userDisplayName: String?,
    userEmail: String?,
    onEditLunch: (LunchEntry) -> Unit,
    onDeleteLunch: (LunchEntry) -> Unit,
    onEditPayment: (PaymentRecord) -> Unit,
    onDeletePayment: (PaymentRecord) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { todayEpochDay() }
    var period by remember { mutableStateOf(SummaryPeriod.SEMANA) }
    var activityTab by remember { mutableStateOf(ActivityTab.PAGOS) }
    var paymentPendingDelete by remember { mutableStateOf<PaymentRecord?>(null) }
    var lunchPendingDelete by remember { mutableStateOf<LunchEntry?>(null) }

    val minEpochDay = remember(period, today) {
        period.daysBack?.let { today - it }
    }

    val filteredPayments = remember(ledger.payments, minEpochDay) {
        if (minEpochDay == null) ledger.payments
        else ledger.payments.filter { it.dateEpochDay >= minEpochDay }
    }
    val filteredLunches = remember(ledger.lunchEntries, minEpochDay) {
        if (minEpochDay == null) ledger.lunchEntries
        else ledger.lunchEntries.filter { it.dateEpochDay >= minEpochDay }
    }

    val totalCollected = remember(filteredPayments) { filteredPayments.sumOf { it.amount } }
    val totalBilled = remember(filteredLunches) { filteredLunches.sumOf { it.totalAmount } }
    val totalLunchesQty = remember(filteredLunches) { filteredLunches.sumOf { it.quantity } }

    val methodBreakdown = remember(filteredPayments) {
        PaymentMethod.entries.mapNotNull { method ->
            val sum = filteredPayments.filter { it.methodEnum == method }.sumOf { it.amount }
            if (sum > 0L) method to sum else null
        }.sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "title") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.summary_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Ingresos recibidos por método de pago e historial completo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Period Filter Chips
        item(key = "period_chips") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SummaryPeriod.entries, key = { it.name }) { option ->
                    FilterChip(
                        selected = period == option,
                        onClick = { period = option },
                        label = { Text(option.label) },
                        modifier = Modifier.height(42.dp)
                    )
                }
            }
        }

        // Financial Overview Card
        item(key = "overview") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ingresos cobrados",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(totalCollected),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Total facturado",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(totalBilled),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (totalLunchesQty == 1L) "1 almuerzo" else "$totalLunchesQty almuerzos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (methodBreakdown.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = stringResource(R.string.summary_by_method),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        methodBreakdown.forEach { (method, sum) ->
                            val ratio = if (totalCollected > 0L) {
                                (sum.toFloat() / totalCollected.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = paymentMethodIcon(method),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = method.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = formatCurrency(sum),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Toggle between Payments and Lunches History
        item(key = "activity_toggle") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ActivityTab.entries.forEachIndexed { index, tab ->
                    val count = if (tab == ActivityTab.PAGOS) filteredPayments.size else filteredLunches.size
                    SegmentedButton(
                        selected = activityTab == tab,
                        onClick = { activityTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ActivityTab.entries.size
                        ),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text("${tab.label} ($count)")
                    }
                }
            }
        }

        if (activityTab == ActivityTab.PAGOS) {
            if (filteredPayments.isEmpty()) {
                item(key = "empty_payments") {
                    EmptyStateCard(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "Sin pagos en este período",
                        subtitle = "Cuando registres pagos totales o abonos desde Pendientes aparecerán aquí con su método de pago."
                    )
                }
            } else {
                items(filteredPayments, key = { it.id }) { payment ->
                    PaymentHistoryRowCard(
                        payment = payment,
                        onEdit = { onEditPayment(payment) },
                        onDelete = { paymentPendingDelete = payment }
                    )
                }
            }
        } else {
            if (filteredLunches.isEmpty()) {
                item(key = "empty_lunches") {
                    EmptyStateCard(
                        icon = Icons.Rounded.Restaurant,
                        title = "Sin almuerzos en este período",
                        subtitle = "Los almuerzos diarios que anotes aparecerán organizados en este historial."
                    )
                }
            } else {
                items(filteredLunches, key = { it.id }) { entry ->
                    LunchHistoryRowCard(
                        entry = entry,
                        onEdit = { onEditLunch(entry) },
                        onDelete = { lunchPendingDelete = entry }
                    )
                }
            }
        }

        // Account & Sign-Out Card
        item(key = "account_section") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.summary_account_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = userDisplayName?.takeIf { it.isNotBlank() } ?: "Cuenta de Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.auth_sign_out))
                    }
                }
            }
        }
    }

    paymentPendingDelete?.let { payment ->
        ConfirmDeleteDialog(
            title = "¿Eliminar este pago?",
            message = "Se eliminará el pago de ${formatCurrency(payment.amount)} (${payment.methodEnum.label}) de ${payment.clientNameSnapshot} y su saldo pendiente se actualizará automáticamente.",
            onConfirm = {
                onDeletePayment(payment)
                paymentPendingDelete = null
            },
            onDismiss = { paymentPendingDelete = null }
        )
    }

    lunchPendingDelete?.let { entry ->
        ConfirmDeleteDialog(
            title = "¿Eliminar este almuerzo?",
            message = "Se eliminarán ${entry.quantity} almuerzo(s) de ${entry.clientNameSnapshot} por ${formatCurrency(entry.totalAmount)}.",
            onConfirm = {
                onDeleteLunch(entry)
                lunchPendingDelete = null
            },
            onDismiss = { lunchPendingDelete = null }
        )
    }
}

@Composable
fun PaymentHistoryRowCard(
    payment: PaymentRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = paymentMethodIcon(payment.methodEnum),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = payment.clientNameSnapshot,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatShortDate(payment.dateEpochDay)} · ${payment.methodEnum.label} · ${payment.typeEnum.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (payment.note.isNotBlank()) {
                        Text(
                            text = payment.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${formatCurrency(payment.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LunchHistoryRowCard(
    entry: LunchEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.clientNameSnapshot,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val qty = if (entry.quantity == 1L) "1 almuerzo" else "${entry.quantity} almuerzos"
                Text(
                    text = "${formatShortDate(entry.dateEpochDay)} · $qty (${formatCurrency(entry.unitPrice)} c/u)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(entry.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
