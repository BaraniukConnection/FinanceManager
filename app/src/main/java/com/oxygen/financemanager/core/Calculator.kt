package com.oxygen.financemanager.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Кнопки калькулятора суми. */
sealed interface CalcKey {
    data class Digit(val digit: Char) : CalcKey
    data object Dot : CalcKey
    data class Op(val symbol: Char) : CalcKey
    data object Equals : CalcKey
    data object Backspace : CalcKey
    data object Clear : CalcKey
}

/**
 * Калькулятор для поля суми. Вираз — звичайний рядок на кшталт `12+5.5×3`, уся логіка —
 * чисті функції, тож перевіряється на JVM без емулятора.
 *
 * Обчислення в [BigDecimal]: 0.1 + 0.2 тут дорівнює 0.3, а не 0.30000000000000004.
 * Множення й ділення мають пріоритет над додаванням і відніманням, як у звичайному калькуляторі.
 */
object Calculator {
    const val PLUS = '+'
    const val MINUS = '−'
    const val TIMES = '×'
    const val DIVIDE = '÷'

    private val ops = setOf(PLUS, MINUS, TIMES, DIVIDE)
    private const val MAX_LENGTH = 24

    fun isOp(ch: Char) = ch in ops

    /** Число, яке зараз набирається: хвіст виразу після останнього оператора. */
    private fun lastNumber(expr: String) = expr.takeLastWhile { !isOp(it) }

    fun press(expr: String, key: CalcKey): String {
        val next = when (key) {
            is CalcKey.Digit -> {
                // «0» на початку числа замінюється, а не множиться: 0 → 7, а не 07
                if (lastNumber(expr) == "0") expr.dropLast(1) + key.digit else expr + key.digit
            }
            CalcKey.Dot -> {
                val number = lastNumber(expr)
                when {
                    '.' in number -> expr
                    number.isEmpty() -> "${expr}0."
                    else -> "$expr."
                }
            }
            is CalcKey.Op -> when {
                expr.isEmpty() -> expr // сума не починається з оператора
                isOp(expr.last()) -> expr.dropLast(1) + key.symbol // передумав: «12+» → «12−»
                else -> expr + key.symbol
            }
            CalcKey.Equals -> evaluate(expr)?.let(::format) ?: expr
            CalcKey.Backspace -> expr.dropLast(1)
            CalcKey.Clear -> ""
        }
        return if (next.length > MAX_LENGTH) expr else next
    }

    /**
     * Значення виразу. Оператор у кінці ігнорується: «12+» — це 12, тож підтвердження
     * незавершеного виразу не губить уже введене. `null` — порожньо або ділення на нуль.
     */
    fun evaluate(expr: String): BigDecimal? {
        val trimmed = expr.trimEnd { isOp(it) }
        if (trimmed.isEmpty()) return null

        val numbers = mutableListOf<BigDecimal>()
        val operators = mutableListOf<Char>()
        val current = StringBuilder()
        for (ch in trimmed) {
            if (isOp(ch)) {
                numbers += current.toString().toBigDecimalOrNull() ?: return null
                operators += ch
                current.clear()
            } else {
                current.append(ch)
            }
        }
        numbers += current.toString().toBigDecimalOrNull() ?: return null

        // спершу × і ÷, потім + і −
        val terms = mutableListOf(numbers.first())
        val additive = mutableListOf<Char>()
        operators.forEachIndexed { i, op ->
            val operand = numbers[i + 1]
            when (op) {
                TIMES -> terms[terms.lastIndex] = terms.last().multiply(operand)
                DIVIDE -> {
                    if (operand.signum() == 0) return null
                    terms[terms.lastIndex] = terms.last().divide(operand, MathContext.DECIMAL64)
                }
                else -> {
                    terms += operand
                    additive += op
                }
            }
        }
        var result = terms.first()
        additive.forEachIndexed { i, op ->
            result = if (op == PLUS) result + terms[i + 1] else result - terms[i + 1]
        }
        return result
    }

    /** Результат як рядок без зайвих нулів: 17, 12.5, 0.3333333333 обрізається до 8 знаків. */
    fun format(value: BigDecimal): String =
        value.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
