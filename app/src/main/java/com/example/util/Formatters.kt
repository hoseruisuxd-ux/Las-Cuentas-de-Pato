package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.ClientAccountSummary
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private const val MILLIS_PER_DAY = 86_400_000L

private val spanishMonthsShort = arrayOf(
    "ene", "feb", "mar", "abr", "may", "jun",
    "jul", "ago", "sep", "oct", "nov", "dic"
)

private val spanishMonthsFull = arrayOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

private val spanishDaysOfWeek = arrayOf(
    "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
)

fun formatCurrency(amount: Long): String {
    val formatter = NumberFormat.getIntegerInstance(Locale("es", "CO"))
    val prefix = if (amount < 0L) "-$" else "$"
    return "$prefix${formatter.format(kotlin.math.abs(amount))}"
}

fun todayEpochDay(): Long {
    val local = Calendar.getInstance()
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }
    return utc.timeInMillis / MILLIS_PER_DAY
}

fun epochDayToUtcMillis(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

fun utcMillisToEpochDay(utcMillis: Long): Long = utcMillis / MILLIS_PER_DAY

private fun epochDayToUtcCalendar(epochDay: Long): Calendar {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = epochDay * MILLIS_PER_DAY
    }
}

fun formatShortDate(epochDay: Long): String {
    val cal = epochDayToUtcCalendar(epochDay)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = spanishMonthsShort[cal.get(Calendar.MONTH)]
    return "$day $month"
}

fun formatFriendlyDate(epochDay: Long): String {
    val today = todayEpochDay()
    val cal = epochDayToUtcCalendar(epochDay)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = spanishMonthsFull[cal.get(Calendar.MONTH)]
    return when (epochDay) {
        today -> "Hoy, $day de $month"
        today - 1 -> "Ayer, $day de $month"
        today + 1 -> "Mañana, $day de $month"
        else -> {
            val dow = spanishDaysOfWeek[cal.get(Calendar.DAY_OF_WEEK) - 1]
            "$dow, $day de $month"
        }
    }
}

fun formatFullDate(epochDay: Long): String {
    val cal = epochDayToUtcCalendar(epochDay)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = spanishMonthsFull[cal.get(Calendar.MONTH)]
    val year = cal.get(Calendar.YEAR)
    return "$day de $month de $year"
}

fun formatLunchesCountText(count: Int, hasPartial: Boolean = false): String {
    val base = when (count) {
        0 -> "Sin almuerzos pendientes"
        1 -> "1 almuerzo pendiente"
        else -> "$count almuerzos pendientes"
    }
    return if (hasPartial && count > 0) "$base (incluye saldo parcial)" else base
}

fun buildWhatsAppStatement(summary: ClientAccountSummary): String {
    val sb = StringBuilder()
    val firstName = summary.client.fullName.trim().substringBefore(" ").ifBlank { summary.client.fullName }
    sb.append("Hola $firstName, te saluda Pato 😊\n")
    sb.append("Aquí tienes el resumen de tu cuenta de almuerzos:\n\n")

    if (summary.pendingBalance > 0L) {
        if (summary.unpaidItems.isNotEmpty()) {
            sb.append("🍽️ Almuerzos pendientes:\n")
            summary.unpaidItems.takeLast(12).forEach { item ->
                val dateLabel = formatShortDate(item.entry.dateEpochDay)
                val qtyLabel = if (item.entry.quantity == 1L) "1 almuerzo" else "${item.entry.quantity} almuerzos"
                val amountText = if (item.remainingAmount < item.entry.totalAmount) {
                    "${formatCurrency(item.remainingAmount)} (saldo parcial de ${formatCurrency(item.entry.totalAmount)})"
                } else {
                    formatCurrency(item.entry.totalAmount)
                }
                sb.append("• $dateLabel: $qtyLabel — $amountText\n")
            }
            sb.append("\n")
        }
        sb.append("💰 *Total pendiente: ${formatCurrency(summary.pendingBalance)}*\n")
        if (summary.pendingLunchesCount > 0) {
            sb.append("(${formatLunchesCountText(summary.pendingLunchesCount, summary.hasPartialLunchBalance)})\n")
        }
        sb.append("\n¡Muchas gracias por tu preferencia! Quedo atenta, un abrazo.")
    } else if (summary.creditBalance > 0L) {
        sb.append("✅ Tu cuenta está al día y tienes un saldo a favor de *${formatCurrency(summary.creditBalance)}*.\n")
        sb.append("\n¡Muchas gracias por tu preferencia!")
    } else {
        sb.append("✅ ¡Tu cuenta de almuerzos está completamente al día ($0 pendiente)!\n")
        sb.append("\n¡Muchas gracias por tu preferencia!")
    }
    return sb.toString()
}

fun normalizeWhatsAppPhone(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    // Colombian 10-digit mobile numbers starting with 3 -> prepend country code 57
    return if (digits.length == 10 && digits.startsWith("3")) {
        "57$digits"
    } else {
        digits
    }
}

fun shareStatementOnWhatsApp(context: Context, summary: ClientAccountSummary) {
    val message = buildWhatsAppStatement(summary)
    val phone = normalizeWhatsAppPhone(summary.client.phone)
    val encoded = Uri.encode(message)

    if (phone.isNotEmpty()) {
        val waUri = Uri.parse("https://wa.me/$phone?text=$encoded")
        val viewIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(viewIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall through to standard share sheet
        }
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(shareIntent, "Enviar resumen de cuenta").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        // No app available to handle share
    }
}
