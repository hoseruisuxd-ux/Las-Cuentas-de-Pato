package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BusinessRepository
import com.example.data.Client
import com.example.data.ClientStatus
import com.example.data.LedgerCalculator
import com.example.data.LedgerData
import com.example.data.PaymentMethod
import com.example.data.PaymentModality
import com.example.data.PaymentType
import com.example.util.formatCurrency
import com.example.util.todayEpochDay
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class BusinessLedgerViewModel(
    private val repository: BusinessRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedDateEpochDay = MutableStateFlow(todayEpochDay())
    val selectedDateEpochDay: StateFlow<Long> = _selectedDateEpochDay.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val ledgerState: StateFlow<UiState<LedgerData>> = combine(
        repository.observeClients(currentUserId),
        repository.observeLunchEntries(currentUserId),
        repository.observePayments(currentUserId)
    ) { clients, entries, payments ->
        UiState.Success(
            LedgerCalculator.buildLedgerData(
                clients = clients,
                lunchEntries = entries,
                payments = payments
            )
        ) as UiState<LedgerData>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing business ledger", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "No pudimos sincronizar tus cuentas."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    fun selectDate(epochDay: Long) {
        _selectedDateEpochDay.value = epochDay
    }

    fun shiftSelectedDate(deltaDays: Long) {
        _selectedDateEpochDay.value = (_selectedDateEpochDay.value + deltaDays).coerceIn(18_000L, 50_000L)
    }

    fun jumpToToday() {
        _selectedDateEpochDay.value = todayEpochDay()
    }

    // --- Client Actions ---

    fun saveClient(
        editingClientId: String?,
        fullName: String,
        phone: String,
        address: String,
        modality: PaymentModality,
        agreedPrice: Long,
        notes: String,
        status: ClientStatus,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = if (editingClientId.isNullOrBlank()) {
                repository.createClient(
                    fullName = fullName,
                    phone = phone,
                    address = address,
                    modality = modality,
                    agreedPrice = agreedPrice,
                    notes = notes,
                    status = status
                )
            } else {
                repository.updateClient(
                    clientId = editingClientId,
                    fullName = fullName,
                    phone = phone,
                    address = address,
                    modality = modality,
                    agreedPrice = agreedPrice,
                    notes = notes,
                    status = status
                )
            }
            _isSaving.value = false
            result.onSuccess {
                val msg = if (editingClientId.isNullOrBlank()) {
                    "Cliente ${fullName.trim()} guardado"
                } else {
                    "Datos de ${fullName.trim()} actualizados"
                }
                _messages.tryEmit(msg)
                onComplete()
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "No se pudo guardar el cliente.")
            }
        }
    }

    fun toggleClientStatus(client: Client) {
        viewModelScope.launch {
            val nextStatus = if (client.activeFlag) ClientStatus.INACTIVO else ClientStatus.ACTIVO
            repository.toggleClientStatus(client.id, nextStatus)
                .onSuccess {
                    val label = if (nextStatus == ClientStatus.ACTIVO) "activado" else "marcado como inactivo"
                    _messages.tryEmit("${client.fullName} $label")
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "No se pudo cambiar el estado.")
                }
        }
    }

    fun deleteClient(client: Client, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.deleteClient(client.id)
            _isSaving.value = false
            result.onSuccess {
                _messages.tryEmit("Cliente ${client.fullName} eliminado")
                onComplete()
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "No se pudo eliminar el cliente.")
            }
        }
    }

    // --- Lunch Entry Actions ---

    fun saveLunchEntry(
        editingEntryId: String?,
        client: Client,
        dateEpochDay: Long,
        quantity: Long,
        unitPrice: Long,
        note: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = if (editingEntryId.isNullOrBlank()) {
                repository.createLunchEntry(
                    client = client,
                    dateEpochDay = dateEpochDay,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    note = note
                )
            } else {
                repository.updateLunchEntry(
                    entryId = editingEntryId,
                    clientNameSnapshot = client.fullName,
                    dateEpochDay = dateEpochDay,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    note = note
                )
            }
            _isSaving.value = false
            result.onSuccess {
                val qtyWord = if (quantity == 1L) "1 almuerzo" else "$quantity almuerzos"
                val totalText = formatCurrency(quantity * unitPrice)
                val msg = if (editingEntryId.isNullOrBlank()) {
                    "Anotado: $qtyWord para ${client.fullName} ($totalText)"
                } else {
                    "Registro corregido: $qtyWord ($totalText)"
                }
                _messages.tryEmit(msg)
                onComplete()
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "No se pudo guardar el almuerzo.")
            }
        }
    }

    fun deleteLunchEntry(entryId: String, clientName: String) {
        viewModelScope.launch {
            repository.deleteLunchEntry(entryId)
                .onSuccess {
                    _messages.tryEmit("Registro de almuerzo de $clientName eliminado")
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "No se pudo eliminar el registro.")
                }
        }
    }

    // --- Payment Actions ---

    fun savePayment(
        editingPaymentId: String?,
        client: Client,
        dateEpochDay: Long,
        amount: Long,
        method: PaymentMethod,
        paymentType: PaymentType,
        note: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = if (editingPaymentId.isNullOrBlank()) {
                repository.createPayment(
                    client = client,
                    dateEpochDay = dateEpochDay,
                    amount = amount,
                    method = method,
                    paymentType = paymentType,
                    note = note
                )
            } else {
                repository.updatePayment(
                    paymentId = editingPaymentId,
                    clientNameSnapshot = client.fullName,
                    dateEpochDay = dateEpochDay,
                    amount = amount,
                    method = method,
                    paymentType = paymentType,
                    note = note
                )
            }
            _isSaving.value = false
            result.onSuccess {
                val label = if (paymentType == PaymentType.TOTAL) "Pago total" else "Abono"
                _messages.tryEmit("$label de ${formatCurrency(amount)} registrado (${method.label})")
                onComplete()
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "No se pudo registrar el pago.")
            }
        }
    }

    fun deletePayment(paymentId: String, clientName: String) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
                .onSuccess {
                    _messages.tryEmit("Pago de $clientName eliminado")
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "No se pudo eliminar el pago.")
                }
        }
    }

    private companion object {
        const val TAG = "BusinessLedgerVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
