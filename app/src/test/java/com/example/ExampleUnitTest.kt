package com.example

import com.example.data.Client
import com.example.data.ClientStatus
import com.example.data.LedgerCalculator
import com.example.data.LunchEntry
import com.example.data.PaymentMethod
import com.example.data.PaymentModality
import com.example.data.PaymentRecord
import com.example.data.PaymentType
import com.example.util.buildWhatsAppStatement
import com.example.util.normalizeWhatsAppPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun ledgerCalculator_computesFifoDebtAndPartialPaymentsAccurately() {
        val carlos = Client(
            id = "c1",
            userId = "u1",
            fullName = "Carlos Mendoza",
            phone = "300 123 4567",
            paymentModality = PaymentModality.SEMANAL.name,
            agreedPrice = 15000L,
            notes = "Sin cebolla",
            status = ClientStatus.ACTIVO.name
        )

        val entries = listOf(
            LunchEntry(
                id = "e1",
                userId = "u1",
                clientId = "c1",
                clientNameSnapshot = "Carlos Mendoza",
                dateEpochDay = 20780L,
                quantity = 2L,
                unitPrice = 15000L,
                totalAmount = 30000L
            ),
            LunchEntry(
                id = "e2",
                userId = "u1",
                clientId = "c1",
                clientNameSnapshot = "Carlos Mendoza",
                dateEpochDay = 20781L,
                quantity = 1L,
                unitPrice = 15000L,
                totalAmount = 15000L
            )
        )

        // Carlos pays $20.000 partial abono against $45.000 total -> $25.000 pending
        val payments = listOf(
            PaymentRecord(
                id = "p1",
                userId = "u1",
                clientId = "c1",
                clientNameSnapshot = "Carlos Mendoza",
                dateEpochDay = 20781L,
                amount = 20000L,
                method = PaymentMethod.NEQUI.name,
                paymentType = PaymentType.ABONO.name
            )
        )

        val ledger = LedgerCalculator.buildLedgerData(listOf(carlos), entries, payments)
        val summary = ledger.summariesByClientId["c1"]!!

        assertEquals(45000L, summary.totalCharged)
        assertEquals(20000L, summary.totalPaid)
        assertEquals(25000L, summary.pendingBalance)
        assertEquals(0L, summary.creditBalance)
        assertEquals(1, summary.pendingLunchesCount)
        assertTrue(summary.hasPartialLunchBalance)
        assertEquals(25000L, ledger.totalReceivables)
        assertEquals(1, ledger.totalClientsWithDebt)

        val daily = LedgerCalculator.computeDailySummary(ledger, 20781L)
        assertEquals(1L, daily.lunchesCount)
        assertEquals(15000L, daily.billedAmount)
        assertEquals(20000L, daily.collectedAmount)
    }

    @Test
    fun ledgerCalculator_handlesExactFullPaymentSettlement() {
        val maria = Client(
            id = "c2",
            userId = "u1",
            fullName = "María López",
            agreedPrice = 14000L
        )
        val entries = listOf(
            LunchEntry(
                id = "e1",
                clientId = "c2",
                clientNameSnapshot = "María López",
                dateEpochDay = 20780L,
                quantity = 2L,
                unitPrice = 14000L,
                totalAmount = 28000L
            )
        )
        val payments = listOf(
            PaymentRecord(
                id = "p1",
                clientId = "c2",
                clientNameSnapshot = "María López",
                dateEpochDay = 20780L,
                amount = 28000L,
                method = PaymentMethod.EFECTIVO.name,
                paymentType = PaymentType.TOTAL.name
            )
        )

        val ledger = LedgerCalculator.buildLedgerData(listOf(maria), entries, payments)
        val summary = ledger.summariesByClientId["c2"]!!

        assertEquals(0L, summary.pendingBalance)
        assertEquals(0, summary.pendingLunchesCount)
        assertFalse(summary.hasPartialLunchBalance)
        assertEquals(0L, ledger.totalReceivables)
    }

    @Test
    fun whatsAppHelper_normalizesColombianPhoneAndFormatsStatement() {
        assertEquals("573001234567", normalizeWhatsAppPhone("300 123-4567"))
        assertEquals("573109876543", normalizeWhatsAppPhone("+57 310 987 6543"))

        val client = Client(id = "c1", fullName = "Carlos Mendoza", agreedPrice = 15000L)
        val entry = LunchEntry(
            id = "e1",
            clientId = "c1",
            clientNameSnapshot = "Carlos Mendoza",
            dateEpochDay = 20780L,
            quantity = 2L,
            unitPrice = 15000L,
            totalAmount = 30000L
        )
        val ledger = LedgerCalculator.buildLedgerData(listOf(client), listOf(entry), emptyList())
        val statement = buildWhatsAppStatement(ledger.summariesByClientId["c1"]!!)

        assertTrue(statement.contains("Hola Carlos"))
        assertTrue(statement.contains("2 almuerzos"))
        assertTrue(statement.contains("30.000"))
    }
}
