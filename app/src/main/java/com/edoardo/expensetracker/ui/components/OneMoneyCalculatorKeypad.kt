package com.edoardo.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.ui.theme.*

/**
 * Tastierino calcolatrice moderno FinTech (Ivy Wallet aesthetic).
 * - Tasti numerici elevati con bordo hairline
 * - Operatori matematici con accento PrimaryMint
 * - Tasto backspace e calendario raffinati
 * - Tasto conferma ad alto contrasto con gradiente
 */
@Composable
fun OneMoneyCalculatorKeypad(
    currentExpression: String,
    onExpressionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDateClick: () -> Unit,
    confirmColor: Color,
    modifier: Modifier = Modifier
) {
    // Flag per resettare il valore iniziale se l'utente digita una cifra (comportamento standard POS/Calcolatrice)
    var shouldOverwrite by remember { mutableStateOf(currentExpression.isNotEmpty() && currentExpression != "0") }

    fun append(char: String) {
        if (char == ",") {
            if (shouldOverwrite) {
                shouldOverwrite = false
                onExpressionChange("0.")
                return
            }
            val parts = currentExpression.split("+", "-", "×", "÷", "*", "/")
            val last = parts.lastOrNull() ?: ""
            if (!last.contains(".")) {
                onExpressionChange(if (last.isEmpty()) currentExpression + "0." else currentExpression + ".")
            }
            return
        }

        if (char in listOf("+", "-", "×", "÷", "—")) {
            val mathOp = when (char) {
                "×" -> "*"
                "÷" -> "/"
                "—" -> "-"
                else -> char
            }

            // Se l'utente digita '-' e l'espressione è vuota, "0" o deve essere sovrascritta,
            // è un numero negativo diretto: non è un'operazione che necessita dell' =
            if ((char == "-" || char == "—") && (currentExpression.isEmpty() || currentExpression == "0" || shouldOverwrite)) {
                shouldOverwrite = false
                onExpressionChange("-")
                return
            }

            shouldOverwrite = false
            if (currentExpression.isNotEmpty() && currentExpression.last().isDigit()) {
                onExpressionChange(currentExpression + mathOp)
            }
            return
        }

        if (char == "€") return // tasto estetico valuta

        if (shouldOverwrite) {
            shouldOverwrite = false
            onExpressionChange(char)
            return
        }

        if (currentExpression == "0") {
            onExpressionChange(char)
        } else {
            onExpressionChange(currentExpression + char)
        }
    }

    fun backspace() {
        if (shouldOverwrite) {
            shouldOverwrite = false
            onExpressionChange("0")
            return
        }
        if (currentExpression.isNotEmpty()) {
            val dropped = currentExpression.dropLast(1)
            onExpressionChange(if (dropped.isEmpty() || dropped == "-") "0" else dropped)
        }
    }

    val buttonCorner = RoundedCornerShape(14.dp)
    val numberBg = Color(0xFF181818)
    val numberBorder = Color.White.copy(alpha = 0.05f)
    val textNumberColor = TextPrimary

    // Tasti Operatori con contrasto raffinato neutro puro e colore coordinato
    val opBg = Color(0xFF242424)
    val opBorder = Color.White.copy(alpha = 0.08f)
    val textOpColor = confirmColor

    // Rileva se è in corso un'operazione aritmetica (+, -, *, /)
    // Se c'è solo un meno iniziale per valore negativo (es. "-35"), non è un'operazione pendente
    val hasPendingOperation = currentExpression.drop(if (currentExpression.startsWith("-")) 1 else 0).any { it in "+-×÷*/—" }

    fun handleConfirmOrEqual() {
        if (hasPendingOperation) {
            val result = evaluateMathExpression(currentExpression)
            if (result != null) {
                val formatted = if (result % 1.0 == 0.0) {
                    String.format(java.util.Locale.US, "%.0f", result)
                } else {
                    String.format(java.util.Locale.US, "%.2f", result)
                }
                onExpressionChange(formatted)
                shouldOverwrite = true
            }
        } else {
            onConfirm()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // Riga 1: ÷, 7, 8, 9, ⌫
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            KeypadCell("÷", modifier = Modifier.weight(1f), bg = opBg, border = opBorder, textColor = textOpColor) { append("÷") }
            KeypadCell("7", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = textNumberColor) { append("7") }
            KeypadCell("8", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = textNumberColor) { append("8") }
            KeypadCell("9", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = textNumberColor) { append("9") }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(buttonCorner)
                    .background(ExpenseRed.copy(alpha = 0.12f))
                    .border(1.dp, ExpenseRed.copy(alpha = 0.25f), buttonCorner)
                    .clickable { backspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Cancella",
                    tint = ExpenseRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Riga 2: ×, 4, 5, 6, 📅
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            KeypadCell("×", modifier = Modifier.weight(1f), bg = opBg, border = opBorder, textColor = textOpColor) { append("×") }
            KeypadCell("4", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = textNumberColor) { append("4") }
            KeypadCell("5", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = textNumberColor) { append("5") }
            KeypadCell("6", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = textNumberColor) { append("6") }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(buttonCorner)
                    .background(opBg)
                    .border(1.dp, opBorder, buttonCorner)
                    .clickable { onDateClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "Data",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Righe 3 e 4 con tasto Conferma alto 2 righe a destra
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Column(
                modifier = Modifier.weight(4f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Riga 3: —, 1, 2, 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    KeypadCell("—", modifier = Modifier.weight(1f), bg = opBg, border = opBorder, textColor = textOpColor) { append("—") }
                    KeypadCell("1", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = textNumberColor) { append("1") }
                    KeypadCell("2", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = textNumberColor) { append("2") }
                    KeypadCell("3", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = textNumberColor) { append("3") }
                }

                // Riga 4: +, €, 0, ,
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    KeypadCell("+", modifier = Modifier.weight(1f), bg = opBg, border = opBorder, textColor = textOpColor) { append("+") }
                    KeypadCell("€", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = TextMuted, isCurrency = true) { append("€") }
                    KeypadCell("0", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = textNumberColor) { append("0") }
                    KeypadCell(",", modifier = Modifier.weight(1f), bg = numberBg, border = numberBorder, textColor = textNumberColor) { append(",") }
                }
            }

            // Tasto Conferma verticale (alto 2 righe = ~103dp)
            val confirmCorner = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(103.dp)
                    .clip(confirmCorner)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                confirmColor,
                                confirmColor.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .border(1.dp, confirmColor.copy(alpha = 0.6f), confirmCorner)
                    .clickable { handleConfirmOrEqual() },
                contentAlignment = Alignment.Center
            ) {
                if (hasPendingOperation) {
                    Text(
                        text = "=",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Conferma Spesa",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadCell(
    text: String,
    modifier: Modifier = Modifier,
    bg: Color,
    border: Color,
    textColor: Color,
    isCurrency: Boolean = false,
    onClick: () -> Unit
) {
    val corner = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(corner)
            .background(bg)
            .border(1.dp, border, corner)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (isCurrency) 15.sp else 19.sp,
            fontWeight = if (isCurrency) FontWeight.Light else FontWeight.SemiBold,
            color = textColor
        )
    }
}

/**
 * Valuta espressioni aritmetiche semplici con precedenza degli operatori (+, -, *, /).
 * Gestisce virgole/punti decimali e tronca operatori finali incompleti.
 */
fun evaluateMathExpression(raw: String): Double? {
    if (raw.isBlank()) return null
    val clean = raw.trim()
        .replace(',', '.')
        .replace('×', '*')
        .replace('÷', '/')
        .replace('—', '-')
        .replace(" ", "")

    var expr = clean
    while (expr.isNotEmpty() && (expr.last() == '+' || expr.last() == '-' || expr.last() == '*' || expr.last() == '/')) {
        expr = expr.dropLast(1)
    }
    if (expr.isEmpty()) return null

    return try {
        val tokens = mutableListOf<String>()
        val currentNum = StringBuilder()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c in "+-*/") {
                if (c == '-' && (tokens.isEmpty() || tokens.last() in "+-*/") && currentNum.isEmpty()) {
                    currentNum.append(c)
                } else {
                    if (currentNum.isNotEmpty()) {
                        tokens.add(currentNum.toString())
                        currentNum.clear()
                    }
                    tokens.add(c.toString())
                }
            } else if (c.isDigit() || c == '.') {
                currentNum.append(c)
            }
            i++
        }
        if (currentNum.isNotEmpty()) {
            tokens.add(currentNum.toString())
        }

        if (tokens.isEmpty()) return null

        // Pass 1: Moltiplicazione e Divisione
        val step1 = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]
            if (token == "*" || token == "/") {
                val prev = step1.removeAt(step1.lastIndex).toDouble()
                val next = tokens.getOrNull(idx + 1)?.toDouble() ?: 1.0
                val res = if (token == "*") prev * next else if (next != 0.0) prev / next else prev
                step1.add(res.toString())
                idx += 2
            } else {
                step1.add(token)
                idx++
            }
        }

        // Pass 2: Addizione e Sottrazione
        var total = step1.firstOrNull()?.toDouble() ?: return null
        var opIdx = 1
        while (opIdx < step1.size) {
            val op = step1[opIdx]
            val nextVal = step1.getOrNull(opIdx + 1)?.toDouble() ?: 0.0
            if (op == "+") {
                total += nextVal
            } else if (op == "-") {
                total -= nextVal
            }
            opIdx += 2
        }
        total
    } catch (_: Exception) {
        clean.toDoubleOrNull()
    }
}

