package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PaymentModality(val label: String) {
    DIARIO("Diario"),
    SEMANAL("Semanal"),
    QUINCENAL("Quincenal"),
    MENSUAL("Mensual");

    companion object {
        fun fromRaw(raw: String?): PaymentModality =
            entries.firstOrNull { it.name == raw } ?: SEMANAL
    }
}

enum class ClientStatus(val label: String) {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    companion object {
        fun fromRaw(raw: String?): ClientStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVO
    }
}

enum class PaymentMethod(val label: String) {
    EFECTIVO("Efectivo"),
    NEQUI("Nequi"),
    TRANSFERENCIA("Transferencia"),
    DAVIPLATA("Daviplata"),
    OTRO("Otro");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: EFECTIVO
    }
}

enum class PaymentType(val label: String) {
    TOTAL("Pago total"),
    ABONO("Abono parcial");

    companion object {
        fun fromRaw(raw: String?): PaymentType =
            entries.firstOrNull { it.name == raw } ?: ABONO
    }
}

@Keep
@IgnoreExtraProperties
data class Client(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val address: String = "",
    val paymentModality: String = PaymentModality.SEMANAL.name,
    val agreedPrice: Long = 0L,
    val notes: String = "",
    val status: String = ClientStatus.ACTIVO.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val modalityEnum: PaymentModality get() = PaymentModality.fromRaw(paymentModality)
    val statusEnum: ClientStatus get() = ClientStatus.fromRaw(status)
    val activeFlag: Boolean get() = statusEnum == ClientStatus.ACTIVO
}

@Keep
@IgnoreExtraProperties
data class LunchEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val clientId: String = "",
    val clientNameSnapshot: String = "",
    val dateEpochDay: Long = 0L,
    val quantity: Long = 1L,
    val unitPrice: Long = 0L,
    val totalAmount: Long = 0L,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val clientId: String = "",
    val clientNameSnapshot: String = "",
    val dateEpochDay: Long = 0L,
    val amount: Long = 0L,
    val method: String = PaymentMethod.EFECTIVO.name,
    val paymentType: String = PaymentType.ABONO.name,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val methodEnum: PaymentMethod get() = PaymentMethod.fromRaw(method)
    val typeEnum: PaymentType get() = PaymentType.fromRaw(paymentType)
}

data class UnpaidLunchItem(
    val entry: LunchEntry,
    val remainingAmount: Long,
    val unpaidUnitsApprox: Int
)

data class ClientAccountSummary(
    val client: Client,
    val entries: List<LunchEntry>,
    val payments: List<PaymentRecord>,
    val totalCharged: Long,
    val totalPaid: Long,
    val pendingBalance: Long,
    val creditBalance: Long,
    val totalLunchesCount: Long,
    val pendingLunchesCount: Int,
    val hasPartialLunchBalance: Boolean,
    val unpaidItems: List<UnpaidLunchItem>,
    val lastActivityEpochDay: Long?
)

data class DailySummary(
    val dateEpochDay: Long,
    val lunchesCount: Long,
    val billedAmount: Long,
    val collectedAmount: Long,
    val totalReceivables: Long,
    val clientsWithDebtCount: Int,
    val totalPendingLunches: Int
)

data class LedgerData(
    val clients: List<Client>,
    val lunchEntries: List<LunchEntry>,
    val payments: List<PaymentRecord>,
    val accountSummaries: List<ClientAccountSummary>,
    val summariesByClientId: Map<String, ClientAccountSummary>,
    val totalReceivables: Long,
    val totalClientsWithDebt: Int,
    val totalPendingLunches: Int
)

object LedgerCalculator {

    fun buildLedgerData(
        clients: List<Client>,
        lunchEntries: List<LunchEntry>,
        payments: List<PaymentRecord>
    ): LedgerData {
        val sortedClients = clients.sortedWith(
            compareByDescending<Client> { it.activeFlag }
                .thenBy { it.fullName.lowercase() }
        )
        val entriesByClient = lunchEntries.groupBy { it.clientId }
        val paymentsByClient = payments.groupBy { it.clientId }

        val summaries = sortedClients.map { client ->
            computeClientSummary(
                client = client,
                clientEntries = entriesByClient[client.id].orEmpty(),
                clientPayments = paymentsByClient[client.id].orEmpty()
            )
        }

        val totalReceivables = summaries.sumOf { it.pendingBalance }
        val clientsWithDebt = summaries.count { it.pendingBalance > 0L }
        val totalPendingLunches = summaries.sumOf { it.pendingLunchesCount }

        return LedgerData(
            clients = sortedClients,
            lunchEntries = lunchEntries.sortedWith(
                compareByDescending<LunchEntry> { it.dateEpochDay }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            ),
            payments = payments.sortedWith(
                compareByDescending<PaymentRecord> { it.dateEpochDay }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            ),
            accountSummaries = summaries,
            summariesByClientId = summaries.associateBy { it.client.id },
            totalReceivables = totalReceivables,
            totalClientsWithDebt = clientsWithDebt,
            totalPendingLunches = totalPendingLunches
        )
    }

    fun computeDailySummary(ledger: LedgerData, dateEpochDay: Long): DailySummary {
        val dayLunches = ledger.lunchEntries.filter { it.dateEpochDay == dateEpochDay }
        val dayPayments = ledger.payments.filter { it.dateEpochDay == dateEpochDay }
        return DailySummary(
            dateEpochDay = dateEpochDay,
            lunchesCount = dayLunches.sumOf { it.quantity },
            billedAmount = dayLunches.sumOf { it.totalAmount },
            collectedAmount = dayPayments.sumOf { it.amount },
            totalReceivables = ledger.totalReceivables,
            clientsWithDebtCount = ledger.totalClientsWithDebt,
            totalPendingLunches = ledger.totalPendingLunches
        )
    }

    private fun computeClientSummary(
        client: Client,
        clientEntries: List<LunchEntry>,
        clientPayments: List<PaymentRecord>
    ): ClientAccountSummary {
        val chronologicalEntries = clientEntries.sortedWith(
            compareBy<LunchEntry> { it.dateEpochDay }
                .thenBy { it.createdAt ?: Timestamp(0, 0) }
        )
        val totalCharged = chronologicalEntries.sumOf { it.totalAmount }
        val totalPaid = clientPayments.sumOf { it.amount }
        val diff = totalCharged - totalPaid
        val pendingBalance = maxOf(0L, diff)
        val creditBalance = maxOf(0L, -diff)

        // FIFO allocation of payments against chronological lunch entries
        var remainingCredit = totalPaid
        val unpaidItems = mutableListOf<UnpaidLunchItem>()
        var pendingLunchesFull = 0
        var hasPartial = false

        for (entry in chronologicalEntries) {
            if (remainingCredit >= entry.totalAmount) {
                remainingCredit -= entry.totalAmount
            } else {
                val entryRemaining = entry.totalAmount - remainingCredit
                remainingCredit = 0L
                val unitPrice = entry.unitPrice.coerceAtLeast(1L)
                val fullUnits = (entryRemaining / unitPrice).toInt().coerceAtMost(entry.quantity.toInt())
                val remainder = entryRemaining % unitPrice
                if (remainder > 0L) {
                    hasPartial = true
                }
                val displayUnits = if (fullUnits == 0 && entryRemaining > 0L) 1 else fullUnits
                pendingLunchesFull += fullUnits
                unpaidItems.add(
                    UnpaidLunchItem(
                        entry = entry,
                        remainingAmount = entryRemaining,
                        unpaidUnitsApprox = displayUnits
                    )
                )
            }
        }

        // If there's pending balance smaller than 1 full lunch, still show partial flag
        val effectivePendingLunches = if (pendingBalance > 0L && pendingLunchesFull == 0) {
            1
        } else {
            pendingLunchesFull
        }

        val lastLunch = clientEntries.maxOfOrNull { it.dateEpochDay }
        val lastPay = clientPayments.maxOfOrNull { it.dateEpochDay }
        val lastActivity = listOfNotNull(lastLunch, lastPay).maxOrNull()

        return ClientAccountSummary(
            client = client,
            entries = clientEntries.sortedWith(
                compareByDescending<LunchEntry> { it.dateEpochDay }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            ),
            payments = clientPayments.sortedWith(
                compareByDescending<PaymentRecord> { it.dateEpochDay }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            ),
            totalCharged = totalCharged,
            totalPaid = totalPaid,
            pendingBalance = pendingBalance,
            creditBalance = creditBalance,
            totalLunchesCount = clientEntries.sumOf { it.quantity },
            pendingLunchesCount = effectivePendingLunches,
            hasPartialLunchBalance = hasPartial,
            unpaidItems = unpaidItems,
            lastActivityEpochDay = lastActivity
        )
    }
}
