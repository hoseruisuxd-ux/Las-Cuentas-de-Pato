package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Client
import com.example.data.ClientAccountSummary
import com.example.data.PaymentMethod
import com.example.data.PaymentRecord
import com.example.data.PaymentType
import com.example.util.formatCurrency
import com.example.util.formatFriendlyDate
import com.example.util.todayEpochDay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentSheet(
    summary: ClientAccountSummary,
    editingPayment: PaymentRecord?,
    isSaving: Boolean,
    onSave: (
        client: Client,
        dateEpochDay: Long,
        amount: Long,
        method: PaymentMethod,
        paymentType: PaymentType,
        note: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val effectivePendingBeforeThisPayment = remember(summary.pendingBalance, editingPayment?.id) {
        if (editingPayment != null) {
            summary.pendingBalance + editingPayment.amount
        } else {
            summary.pendingBalance
        }
    }

    var paymentType by remember(editingPayment?.id) {
        mutableStateOf(
            editingPayment?.typeEnum ?: if (effectivePendingBeforeThisPayment > 0L) {
                PaymentType.TOTAL
            } else {
                PaymentType.ABONO
            }
        )
    }
    var amountText by remember(editingPayment?.id, effectivePendingBeforeThisPayment) {
        mutableStateOf(
            when {
                editingPayment != null -> editingPayment.amount.toString()
                effectivePendingBeforeThisPayment > 0L -> effectivePendingBeforeThisPayment.toString()
                else -> summary.client.agreedPrice.coerceAtLeast(1000L).toString()
            }
        )
    }
    var method by remember(editingPayment?.id) {
        mutableStateOf(editingPayment?.methodEnum ?: PaymentMethod.EFECTIVO)
    }
    var dateEpochDay by remember(editingPayment?.id) {
        mutableLongStateOf(editingPayment?.dateEpochDay ?: todayEpochDay())
    }
    var note by remember(editingPayment?.id) {
        mutableStateOf(editingPayment?.note ?: "")
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val parsedAmount = amountText.toLongOrNull() ?: 0L
    val resultingDebt = effectivePendingBeforeThisPayment - parsedAmount

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
                text = if (editingPayment == null) "Registrar pago o abono" else "Editar pago",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Client + Current Balance Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        ClientInitialsAvatar(fullName = summary.client.fullName)
                        Column {
                            Text(
                                text = summary.client.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (effectivePendingBeforeThisPayment > 0L) {
                                    "Deuda actual: ${formatCurrency(effectivePendingBeforeThisPayment)}"
                                } else {
                                    "Cuenta actualmente al día"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Full Payment vs Partial Abono toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(PaymentType.TOTAL, PaymentType.ABONO)
                options.forEachIndexed { index, typeOption ->
                    SegmentedButton(
                        selected = paymentType == typeOption,
                        onClick = {
                            paymentType = typeOption
                            if (typeOption == PaymentType.TOTAL && effectivePendingBeforeThisPayment > 0L) {
                                amountText = effectivePendingBeforeThisPayment.toString()
                            } else if (typeOption == PaymentType.ABONO && parsedAmount == effectivePendingBeforeThisPayment) {
                                val suggested = summary.client.agreedPrice
                                    .coerceAtMost(effectivePendingBeforeThisPayment)
                                    .coerceAtLeast(1000L)
                                amountText = suggested.toString()
                            }
                            validationError = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = typeOption.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Amount input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(8)
                    amountText = filtered
                    val v = filtered.toLongOrNull() ?: 0L
                    if (effectivePendingBeforeThisPayment > 0L) {
                        paymentType = if (v >= effectivePendingBeforeThisPayment) {
                            PaymentType.TOTAL
                        } else {
                            PaymentType.ABONO
                        }
                    }
                    validationError = null
                },
                label = { Text("Monto recibido *") },
                prefix = { Text("$ ") },
                leadingIcon = { Icon(Icons.Rounded.Payments, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_amount_input")
            )

            // Quick amount suggestion chips for partial payments
            if (effectivePendingBeforeThisPayment > 0L) {
                val suggestions = remember(effectivePendingBeforeThisPayment, summary.client.agreedPrice) {
                    buildList {
                        add("Todo (${formatCurrency(effectivePendingBeforeThisPayment)})" to effectivePendingBeforeThisPayment)
                        val oneLunch = summary.client.agreedPrice
                        if (oneLunch in 1 until effectivePendingBeforeThisPayment) {
                            add("1 almuerzo (${formatCurrency(oneLunch)})" to oneLunch)
                        }
                        val half = (effectivePendingBeforeThisPayment / 2000L) * 1000L
                        if (half in 1000L until effectivePendingBeforeThisPayment && half != oneLunch) {
                            add("Mitad (${formatCurrency(half)})" to half)
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { (label, value) ->
                        AssistChip(
                            onClick = {
                                amountText = value.toString()
                                paymentType = if (value >= effectivePendingBeforeThisPayment) {
                                    PaymentType.TOTAL
                                } else {
                                    PaymentType.ABONO
                                }
                                validationError = null
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            // Payment Method Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Método de pago",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.entries.forEach { option ->
                        FilterChip(
                            selected = method == option,
                            onClick = { method = option },
                            leadingIcon = {
                                Icon(
                                    imageVector = paymentMethodIcon(option),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            modifier = Modifier.height(44.dp)
                        )
                    }
                }
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
                                text = "Fecha del pago",
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

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 250) note = it },
                label = { Text("Nota o referencia (opcional)") },
                placeholder = { Text("Ej. Transferencia Nequi confirmada") },
                leadingIcon = { Icon(Icons.Rounded.Notes, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Live Resulting Balance Preview
            if (parsedAmount > 0L) {
                val settled = resultingDebt <= 0L
                val bgColor = if (settled) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                }
                val fgColor = if (settled) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
                val previewText = when {
                    resultingDebt == 0L -> "¡La cuenta quedará completamente al día!"
                    resultingDebt < 0L -> "Cuenta al día + ${formatCurrency(-resultingDebt)} de saldo a favor"
                    else -> "Saldo pendiente después del abono: ${formatCurrency(resultingDebt)}"
                }
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = fgColor
                        )
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = fgColor
                        )
                    }
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
                    if (parsedAmount <= 0L) {
                        validationError = "Ingresa un monto mayor a $0."
                    } else {
                        onSave(
                            summary.client,
                            dateEpochDay,
                            parsedAmount,
                            method,
                            paymentType,
                            note.trim()
                        )
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_payment_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = if (editingPayment == null) {
                        "Confirmar pago (${formatCurrency(parsedAmount)})"
                    } else {
                        "Guardar cambios"
                    },
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
