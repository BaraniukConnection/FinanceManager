package com.oxygen.financemanager.core

import android.os.Build
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Форматування сум. Кількість знаків після коми бере java.util.Currency
 * (EUR — 2, JPY — 0), тож ніде не доводиться це хардкодити.
 *
 * BigDecimal використовується виключно для виводу — у схемі зберігається Long.
 */
fun formatMinor(
    amountMinor: Long,
    currencyCode: String,
    locale: Locale = Locale.getDefault(),
): String {
    val currency = Currency.getInstance(currencyCode)
    val digits = currency.defaultFractionDigits.coerceAtLeast(0)
    val format = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currency
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }
    // символ — після валюти: встановлення валюти скидає символ на стандартний
    (format as? DecimalFormat)?.let { df ->
        df.decimalFormatSymbols = df.decimalFormatSymbols.apply { currencySymbol = currencySymbol(currencyCode, locale) }
    }
    return format.format(BigDecimal.valueOf(amountMinor, digits))
}

/** Скільки мінорних одиниць в одиниці валюти: EUR → 100, JPY → 1. */
fun minorUnitsPer(currencyCode: String): Long {
    var factor = 1L
    repeat(Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)) { factor *= 10 }
    return factor
}

/**
 * Розбирає введене користувачем у мінорні одиниці. Повертає `null`, якщо це не число.
 *
 * Приймає і крапку, і кому як роздільник, ігнорує пробіли-розділювачі тисяч
 * (включно з нерозривним, який підставляє системна клавіатура). Зайві знаки
 * округлює за правилами валюти: для EUR «12.345» стає 1235.
 */
fun parseToMinor(input: String, currencyCode: String): Long? {
    val normalized = input.trim()
        .replace(' ', ' ')
        .replace(' ', ' ')
        .replace(" ", "")
        .replace(',', '.')
    if (normalized.isEmpty()) return null

    val digits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    return try {
        java.math.BigDecimal(normalized)
            .movePointRight(digits)
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .longValueExact()
    } catch (_: NumberFormatException) {
        null
    } catch (_: ArithmeticException) {
        null
    }
}

/**
 * Обернення [parseToMinor] для полів форми: `1234, EUR` → `12.34`. Без символу валюти й
 * розділювачів тисяч, щоб текст можна було редагувати і знову розібрати без втрат.
 */
fun minorToText(amountMinor: Long, currencyCode: String): String {
    val digits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    return BigDecimal.valueOf(amountMinor, digits).toPlainString()
}

/** Символ валюти для показу поруч із сумою: EUR → €, PLN → zł. */
fun currencySymbol(currencyCode: String, locale: Locale = Locale.getDefault()): String =
    narrowSymbol(currencyCode, locale) ?: Currency.getInstance(currencyCode).getSymbol(locale)

/**
 * Вузький символ з ICU: для української локалі стандартний символ євро — «EUR», а вузький — «€».
 * Є лише на Android 9+; на старіших і в JVM-тестах (там android.icu — заглушка) — `null`,
 * і лишається звичайний символ Java.
 */
private fun narrowSymbol(currencyCode: String, locale: Locale): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return runCatching {
        android.icu.util.Currency.getInstance(currencyCode)
            .getName(locale, android.icu.util.Currency.NARROW_SYMBOL_NAME, null)
    }.getOrNull()
}

/**
 * Сума без символу валюти, з розділювачами як прийнято в мові: 2400 → «2,400.00» чи «2 400,00».
 * `null`, якщо текст не число — тоді показується заглушка.
 */
fun formatAmountNumber(amountText: String, currencyCode: String, locale: Locale = Locale.getDefault()): String? {
    val minor = parseToMinor(amountText, currencyCode) ?: return null
    val digits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    return NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }.format(BigDecimal.valueOf(minor, digits))
}
