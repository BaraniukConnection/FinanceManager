package com.oxygen.financemanager.core

import com.oxygen.financemanager.core.Calculator.DIVIDE
import com.oxygen.financemanager.core.Calculator.MINUS
import com.oxygen.financemanager.core.Calculator.PLUS
import com.oxygen.financemanager.core.Calculator.TIMES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorTest {

    /** Натискає кнопки по черзі, як людина: "12+5=" → цифри, оператори, дорівнює. */
    private fun type(keys: String, start: String = ""): String = keys.fold(start) { expr, ch ->
        Calculator.press(
            expr,
            when {
                ch.isDigit() -> CalcKey.Digit(ch)
                ch == '.' -> CalcKey.Dot
                ch == '=' -> CalcKey.Equals
                ch == '<' -> CalcKey.Backspace
                ch == 'C' -> CalcKey.Clear
                else -> CalcKey.Op(ch)
            },
        )
    }

    private fun value(expr: String) = Calculator.evaluate(expr)?.let(Calculator::format)

    @Test
    fun `equals computes the expression`() {
        assertEquals("17", type("12${PLUS}5="))
    }

    @Test
    fun `confirming an unfinished expression evaluates it first`() {
        // «12+5» без «=» і підтвердити — приймається 17
        assertEquals("17", value(type("12${PLUS}5")))
    }

    @Test
    fun `a trailing operator is ignored, not lost`() {
        assertEquals("12", value(type("12$PLUS")))
    }

    @Test
    fun `multiplication and division go before addition`() {
        assertEquals("14", value("2${PLUS}3${TIMES}4"))
        assertEquals("7", value("10${MINUS}6${DIVIDE}2"))
    }

    @Test
    fun `decimal math is exact`() {
        assertEquals("0.3", value("0.1${PLUS}0.2"))
        assertEquals("12.35", value("4.20${PLUS}8.15"))
    }

    @Test
    fun `division by zero gives no value instead of a crash`() {
        assertNull(Calculator.evaluate("5${DIVIDE}0"))
        // «=» на ділення на нуль лишає вираз, щоб його можна було виправити
        assertEquals("5${DIVIDE}0", type("5${DIVIDE}0="))
    }

    @Test
    fun `long divisions are rounded to eight places`() {
        assertEquals("3.33333333", value("10${DIVIDE}3"))
    }

    @Test
    fun `equals can be chained`() {
        assertEquals("34", type("12${PLUS}5=${TIMES}2="))
    }

    @Test
    fun `pressing another operator replaces the previous one`() {
        assertEquals("12$MINUS", type("12$PLUS$MINUS"))
    }

    @Test
    fun `an expression cannot start with an operator`() {
        assertEquals("", type("$PLUS"))
    }

    @Test
    fun `a leading zero is replaced, not repeated`() {
        assertEquals("7", type("07"))
        assertEquals("0", type("00"))
        assertEquals("10${PLUS}7", type("10${PLUS}07"))
    }

    @Test
    fun `a number gets at most one dot, and a bare dot becomes 0 dot`() {
        assertEquals("1.25", type("1.2.5"))
        assertEquals("0.5", type(".5"))
        assertEquals("3${PLUS}0.5", type("3$PLUS.5"))
    }

    @Test
    fun `backspace and clear`() {
        assertEquals("12$PLUS", type("12${PLUS}5<"))
        assertEquals("", type("12${PLUS}5C"))
    }

    @Test
    fun `empty input has no value`() {
        assertNull(Calculator.evaluate(""))
    }

    @Test
    fun `the result feeds straight into the amount parser`() {
        assertEquals(1735L, parseToMinor(value("12.5${PLUS}4.85")!!, "EUR"))
    }
}

class AmountDisplayTest {
    @Test
    fun `amount is shown with the language's separators and without a symbol`() {
        assertEquals("2,400.00", formatAmountNumber("2400", "EUR", java.util.Locale.ENGLISH))
        // у німецькій і українській — кома; розділювач тисяч різний, тому перевіряємо лише кінець
        assertEquals(true, formatAmountNumber("12.5", "EUR", java.util.Locale.GERMAN)!!.endsWith("12,50"))
        assertEquals("100", formatAmountNumber("100", "JPY", java.util.Locale.ENGLISH))
        assertNull(formatAmountNumber("", "EUR"))
    }
}
