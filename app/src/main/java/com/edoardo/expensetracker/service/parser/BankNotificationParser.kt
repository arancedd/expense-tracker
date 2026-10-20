package com.edoardo.expensetracker.service.parser

import java.util.regex.Pattern

data class ParsedCardPayment(
    val amount: Double,
    val payee: String?,
    val bankKeyword: String,
    val rawText: String
)

object BankNotificationParser {

    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:€\s*|EUR\s*)?(\d+[\.,]\d{1,2})\s*(?:€|EUR)?""",
        Pattern.CASE_INSENSITIVE
    )

    private val PAYEE_PATTERN = Pattern.compile(
        """(?:presso|da|a)\s+([A-Za-z0-9\s\.\-_&']{2,30}?)(?:\s+(?:con|effettuato|il|\.|$))""",
        Pattern.CASE_INSENSITIVE
    )

    // Lista identificativa di package bancari comuni
    private val BANK_PACKAGES = mapOf(
        "com.revolut.revolut" to "Revolut",
        "it.hype.app" to "Hype",
        "com.intesasanpaolo.panfc" to "Intesa Sanpaolo",
        "it.postepay.postepay" to "PostePay",
        "com.bbva.bbvaitaly" to "BBVA",
        "com.n26.android" to "N26"
    )

    fun isBankNotification(packageName: String, title: String?, text: String?): Boolean {
        if (BANK_PACKAGES.containsKey(packageName)) return true
        val combined = "${title.orEmpty()} ${text.orEmpty()}".lowercase()
        return combined.contains("pagamento") || 
               combined.contains("transazione") || 
               combined.contains("hai speso") || 
               combined.contains("addebitato")
    }

    fun parse(packageName: String, title: String?, text: String?): ParsedCardPayment? {
        val fullText = "${title.orEmpty()} ${text.orEmpty()}".trim()
        if (fullText.isEmpty()) return null

        val bankName = BANK_PACKAGES[packageName] ?: "Carta"

        // Estrazione importo
        val amountMatcher = AMOUNT_PATTERN.matcher(fullText)
        var amount: Double? = null
        while (amountMatcher.find()) {
            val candidate = amountMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull()
            if (candidate != null && candidate > 0) {
                amount = candidate
                break
            }
        }

        if (amount == null) return null

        // Estrazione esercente
        var payee: String? = null
        val payeeMatcher = PAYEE_PATTERN.matcher(fullText)
        if (payeeMatcher.find()) {
            payee = payeeMatcher.group(1)?.trim()
        }

        return ParsedCardPayment(
            amount = amount,
            payee = payee,
            bankKeyword = bankName,
            rawText = fullText
        )
    }
}
