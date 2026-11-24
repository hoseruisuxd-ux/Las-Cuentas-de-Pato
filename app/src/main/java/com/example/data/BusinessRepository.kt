package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BusinessRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) {
            "Debes iniciar sesión con Google antes de acceder a tus cuentas."
        }
        if (expectedUserId != null) {
            check(uid == expectedUserId) {
                "La sesión activa cambió; operación cancelada por seguridad."
            }
        }
        return uid
    }

    private fun clientsCollection(uid: String) =
        db.collection("users").document(uid).collection("clients")

    private fun lunchEntriesCollection(uid: String) =
        db.collection("users").document(uid).collection("lunch_entries")

    private fun paymentsCollection(uid: String) =
        db.collection("users").document(uid).collection("payments")

    // --- Real-time Snapshot Streams ---

    fun observeClients(userId: String): Flow<List<Client>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/clients"
        emitAll(
            clientsCollection(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Client::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, uid, path)
                    throw error
                }
        )
    }

    fun observeLunchEntries(userId: String): Flow<List<LunchEntry>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/lunch_entries"
        emitAll(
            lunchEntriesCollection(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LunchEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, uid, path)
                    throw error
                }
        )
    }

    fun observePayments(userId: String): Flow<List<PaymentRecord>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/payments"
        emitAll(
            paymentsCollection(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PaymentRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, uid, path)
                    throw error
                }
        )
    }

    private fun logStreamErrorIfAuthenticated(error: Throwable, uid: String, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
    }

    // --- Client Mutations ---

    suspend fun createClient(
        fullName: String,
        phone: String,
        address: String,
        modality: PaymentModality,
        agreedPrice: Long,
        notes: String,
        status: ClientStatus
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/clients"
        return try {
            val uid = requireUserId()
            val cleanName = fullName.trim()
            val cleanPhone = phone.trim()
            val cleanAddress = address.trim()
            val cleanNotes = notes.trim()
            require(cleanName.length in 1..100) { "El nombre debe tener entre 1 y 100 caracteres." }
            require(cleanPhone.length <= 30) { "El teléfono no puede superar 30 caracteres." }
            require(cleanAddress.length <= 200) { "La dirección no puede superar 200 caracteres." }
            require(cleanNotes.length <= 300) { "Las notas no pueden superar 300 caracteres." }
            require(agreedPrice in 1L..10_000_000L) { "El precio por almuerzo debe ser mayor a $0." }

            val docRef = clientsCollection(uid).document()
            docPath = docRef.path
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "fullName" to cleanName,
                "phone" to cleanPhone.takeIf { it.isNotEmpty() },
                "address" to cleanAddress.takeIf { it.isNotEmpty() },
                "paymentModality" to modality.name,
                "agreedPrice" to agreedPrice,
                "notes" to cleanNotes.takeIf { it.isNotEmpty() },
                "status" to status.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateClient(
        clientId: String,
        fullName: String,
        phone: String,
        address: String,
        modality: PaymentModality,
        agreedPrice: Long,
        notes: String,
        status: ClientStatus
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/clients/$clientId"
        return try {
            val uid = requireUserId()
            require(clientId.isNotBlank()) { "ID de cliente inválido." }
            val cleanName = fullName.trim()
            val cleanPhone = phone.trim()
            val cleanAddress = address.trim()
            val cleanNotes = notes.trim()
            require(cleanName.length in 1..100) { "El nombre debe tener entre 1 y 100 caracteres." }
            require(cleanPhone.length <= 30) { "El teléfono no puede superar 30 caracteres." }
            require(cleanAddress.length <= 200) { "La dirección no puede superar 200 caracteres." }
            require(cleanNotes.length <= 300) { "Las notas no pueden superar 300 caracteres." }
            require(agreedPrice in 1L..10_000_000L) { "El precio por almuerzo debe ser mayor a $0." }

            val docRef = clientsCollection(uid).document(clientId)
            docPath = docRef.path
            val updates = mapOf<String, Any>(
                "fullName" to cleanName,
                "phone" to cleanPhone,
                "address" to cleanAddress,
                "paymentModality" to modality.name,
                "agreedPrice" to agreedPrice,
                "notes" to cleanNotes,
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleClientStatus(clientId: String, newStatus: ClientStatus): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/clients/$clientId"
        return try {
            val uid = requireUserId()
            require(clientId.isNotBlank()) { "ID de cliente inválido." }
            val docRef = clientsCollection(uid).document(clientId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteClient(clientId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/clients/$clientId"
        return try {
            val uid = requireUserId()
            require(clientId.isNotBlank()) { "ID de cliente inválido." }
            val docRef = clientsCollection(uid).document(clientId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Lunch Entry Mutations ---

    suspend fun createLunchEntry(
        client: Client,
        dateEpochDay: Long,
        quantity: Long,
        unitPrice: Long,
        note: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/lunch_entries"
        return try {
            val uid = requireUserId()
            require(client.id.isNotBlank()) { "Debes seleccionar un cliente." }
            require(dateEpochDay in 18_000L..50_000L) { "Fecha fuera de rango válido." }
            require(quantity in 1L..500L) { "La cantidad debe ser al menos 1 almuerzo." }
            require(unitPrice in 1L..10_000_000L) { "El precio debe ser mayor a $0." }
            val cleanNote = note.trim()
            require(cleanNote.length <= 250) { "La nota no puede superar 250 caracteres." }

            val totalAmount = quantity * unitPrice
            val docRef = lunchEntriesCollection(uid).document()
            docPath = docRef.path
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "clientId" to client.id,
                "clientNameSnapshot" to client.fullName.trim().take(100),
                "dateEpochDay" to dateEpochDay,
                "quantity" to quantity,
                "unitPrice" to unitPrice,
                "totalAmount" to totalAmount,
                "note" to cleanNote.takeIf { it.isNotEmpty() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateLunchEntry(
        entryId: String,
        clientNameSnapshot: String,
        dateEpochDay: Long,
        quantity: Long,
        unitPrice: Long,
        note: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/lunch_entries/$entryId"
        return try {
            val uid = requireUserId()
            require(entryId.isNotBlank()) { "ID de registro inválido." }
            require(dateEpochDay in 18_000L..50_000L) { "Fecha fuera de rango válido." }
            require(quantity in 1L..500L) { "La cantidad debe ser al menos 1 almuerzo." }
            require(unitPrice in 1L..10_000_000L) { "El precio debe ser mayor a $0." }
            val cleanNote = note.trim()
            require(cleanNote.length <= 250) { "La nota no puede superar 250 caracteres." }

            val totalAmount = quantity * unitPrice
            val docRef = lunchEntriesCollection(uid).document(entryId)
            docPath = docRef.path
            val updates = mapOf<String, Any>(
                "clientNameSnapshot" to clientNameSnapshot.trim().take(100),
                "dateEpochDay" to dateEpochDay,
                "quantity" to quantity,
                "unitPrice" to unitPrice,
                "totalAmount" to totalAmount,
                "note" to cleanNote,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteLunchEntry(entryId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/lunch_entries/$entryId"
        return try {
            val uid = requireUserId()
            require(entryId.isNotBlank()) { "ID de registro inválido." }
            val docRef = lunchEntriesCollection(uid).document(entryId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Payment Mutations ---

    suspend fun createPayment(
        client: Client,
        dateEpochDay: Long,
        amount: Long,
        method: PaymentMethod,
        paymentType: PaymentType,
        note: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/payments"
        return try {
            val uid = requireUserId()
            require(client.id.isNotBlank()) { "Debes seleccionar un cliente." }
            require(dateEpochDay in 18_000L..50_000L) { "Fecha fuera de rango válido." }
            require(amount in 1L..50_000_000L) { "El monto del pago debe ser mayor a $0." }
            val cleanNote = note.trim()
            require(cleanNote.length <= 250) { "La nota no puede superar 250 caracteres." }

            val docRef = paymentsCollection(uid).document()
            docPath = docRef.path
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "clientId" to client.id,
                "clientNameSnapshot" to client.fullName.trim().take(100),
                "dateEpochDay" to dateEpochDay,
                "amount" to amount,
                "method" to method.name,
                "paymentType" to paymentType.name,
                "note" to cleanNote.takeIf { it.isNotEmpty() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePayment(
        paymentId: String,
        clientNameSnapshot: String,
        dateEpochDay: Long,
        amount: Long,
        method: PaymentMethod,
        paymentType: PaymentType,
        note: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/payments/$paymentId"
        return try {
            val uid = requireUserId()
            require(paymentId.isNotBlank()) { "ID de pago inválido." }
            require(dateEpochDay in 18_000L..50_000L) { "Fecha fuera de rango válido." }
            require(amount in 1L..50_000_000L) { "El monto del pago debe ser mayor a $0." }
            val cleanNote = note.trim()
            require(cleanNote.length <= 250) { "La nota no puede superar 250 caracteres." }

            val docRef = paymentsCollection(uid).document(paymentId)
            docPath = docRef.path
            val updates = mapOf<String, Any>(
                "clientNameSnapshot" to clientNameSnapshot.trim().take(100),
                "dateEpochDay" to dateEpochDay,
                "amount" to amount,
                "method" to method.name,
                "paymentType" to paymentType.name,
                "note" to cleanNote,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePayment(paymentId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/payments/$paymentId"
        return try {
            val uid = requireUserId()
            require(paymentId.isNotBlank()) { "ID de pago inválido." }
            val docRef = paymentsCollection(uid).document(paymentId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
