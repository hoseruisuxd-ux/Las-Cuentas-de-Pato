package com.example.ui.components

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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.ClientStatus
import com.example.data.PaymentModality

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClientFormSheet(
    editingClient: Client?,
    isSaving: Boolean,
    onSave: (
        fullName: String,
        phone: String,
        address: String,
        modality: PaymentModality,
        agreedPrice: Long,
        notes: String,
        status: ClientStatus
    ) -> Unit,
    onDelete: ((Client) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var fullName by remember(editingClient?.id) { mutableStateOf(editingClient?.fullName ?: "") }
    var phone by remember(editingClient?.id) { mutableStateOf(editingClient?.phone ?: "") }
    var address by remember(editingClient?.id) { mutableStateOf(editingClient?.address ?: "") }
    var modality by remember(editingClient?.id) {
        mutableStateOf(editingClient?.modalityEnum ?: PaymentModality.SEMANAL)
    }
    var priceText by remember(editingClient?.id) {
        mutableStateOf(
            if (editingClient != null && editingClient.agreedPrice > 0L) {
                editingClient.agreedPrice.toString()
            } else {
                "15000"
            }
        )
    }
    var notes by remember(editingClient?.id) { mutableStateOf(editingClient?.notes ?: "") }
    var isActive by remember(editingClient?.id) {
        mutableStateOf(editingClient?.activeFlag ?: true)
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                text = if (editingClient == null) "Nuevo cliente" else "Editar cliente",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    if (it.length <= 100) fullName = it
                    validationError = null
                },
                label = { Text("Nombre completo *") },
                placeholder = { Text("Ej. Carlos Mendoza") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardCapitalization.Words.let {
                    KeyboardOptions(capitalization = it)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_name_input")
            )

            OutlinedTextField(
                value = priceText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(8)
                    priceText = filtered
                    validationError = null
                },
                label = { Text("Precio acordado por almuerzo *") },
                placeholder = { Text("Ej. 15000") },
                prefix = { Text("$ ") },
                leadingIcon = { Icon(Icons.Rounded.Payments, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_price_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Modalidad de pago",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentModality.entries.forEach { option ->
                        FilterChip(
                            selected = modality == option,
                            onClick = { modality = option },
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

            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 30) phone = it },
                label = { Text("Teléfono / WhatsApp (opcional)") },
                placeholder = { Text("Ej. 300 123 4567") },
                leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_phone_input")
            )

            OutlinedTextField(
                value = address,
                onValueChange = { if (it.length <= 200) address = it },
                label = { Text("Dirección o lugar de entrega (opcional)") },
                placeholder = { Text("Ej. Oficina 302, edificio central") },
                leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 300) notes = it },
                label = { Text("Notas o preferencias (opcional)") },
                placeholder = { Text("Ej. Sin cebolla, sopa aparte, entregar en portería") },
                leadingIcon = { Icon(Icons.Rounded.Notes, contentDescription = null) },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_notes_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isActive) "Cliente activo" else "Cliente inactivo",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isActive) {
                            "Aparece en el registro rápido diario"
                        } else {
                            "Se conserva su historial, pero no aparece en acceso rápido"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
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
                    val trimmedName = fullName.trim()
                    val parsedPrice = priceText.toLongOrNull() ?: 0L
                    when {
                        trimmedName.isEmpty() -> {
                            validationError = "Escribe el nombre del cliente."
                        }
                        parsedPrice <= 0L -> {
                            validationError = "Ingresa un precio por almuerzo mayor a $0."
                        }
                        else -> {
                            onSave(
                                trimmedName,
                                phone.trim(),
                                address.trim(),
                                modality,
                                parsedPrice,
                                notes.trim(),
                                if (isActive) ClientStatus.ACTIVO else ClientStatus.INACTIVO
                            )
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_client_button")
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
                    text = if (editingClient == null) "Guardar cliente" else "Guardar cambios",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (editingClient != null && onDelete != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Eliminar cliente",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showDeleteConfirm && editingClient != null && onDelete != null) {
        ConfirmDeleteDialog(
            title = "¿Eliminar a ${editingClient.fullName}?",
            message = "Si aún tiene cuentas o historial de almuerzos, te recomendamos marcarlo como Inactivo en lugar de eliminarlo.",
            confirmLabel = "Eliminar",
            onConfirm = {
                showDeleteConfirm = false
                onDelete(editingClient)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
