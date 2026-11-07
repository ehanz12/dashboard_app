package com.reihan.dashboard.util

import com.reihan.dashboard.data.model.CategoryResponse
import java.util.Locale

data class ParsedVoiceTransaction(
    val type: String,          // "income" or "expense"
    val amount: Double,        // parsed amount (e.g. 10000.0)
    val categoryId: String?,   // matched category ID if found
    val categoryName: String?, // matched category name
    val description: String,   // generated clean description
    val rawSpeechText: String  // raw speech input from Google Voice
)

object VoiceTransactionParser {

    private val incomeKeywords = listOf(
        "dapat", "dapatkan", "mendapatkan", "menambahkan", "tambah", "pemasukan",
        "masuk", "cair", "terima", "menerima", "gaji", "transfer", "debit", "bonus",
        "diberi", "dikasih", "dapet", "pendapatan", "hasil"
    )

    private val expenseKeywords = listOf(
        "jajan", "berbelanja", "belanja", "keluarkan", "keluar", "bayar", "membayar",
        "beli", "membeli", "pengeluaran", "pakai", "memakai", "potong", "biaya",
        "habis", "kredit", "ongkos"
    )

    fun parse(speechText: String, categories: List<CategoryResponse>): ParsedVoiceTransaction {
        val rawText = speechText.trim()
        val textLower = rawText.lowercase(Locale.getDefault())

        // 1. Detect Type (income vs expense)
        val hasIncomeKw = incomeKeywords.any { textLower.contains(it) }
        val hasExpenseKw = expenseKeywords.any { textLower.contains(it) }
        val type = if (hasIncomeKw && !hasExpenseKw) {
            "income"
        } else {
            "expense"
        }

        // 2. Parse Amount (Numeric & Spoken Indonesian numbers)
        val amount = parseAmount(textLower)

        // 3. Match Category
        var matchedCategoryId: String? = null
        var matchedCategoryName: String? = null

        // Look for explicit "kategori [name]" first
        val categoryPrefixIndex = textLower.indexOf("kategori")
        if (categoryPrefixIndex != -1) {
            val afterKategori = textLower.substring(categoryPrefixIndex + 8).trim()
            val bestMatch = categories.firstOrNull { cat ->
                afterKategori.startsWith(cat.name.lowercase(Locale.getDefault())) ||
                        afterKategori.contains(cat.name.lowercase(Locale.getDefault()))
            }
            if (bestMatch != null) {
                matchedCategoryId = bestMatch.id
                matchedCategoryName = bestMatch.name
            }
        }

        // If not found via prefix, search any category name in full text
        if (matchedCategoryId == null) {
            val bestMatch = categories.firstOrNull { cat ->
                textLower.contains(cat.name.lowercase(Locale.getDefault()))
            }
            if (bestMatch != null) {
                matchedCategoryId = bestMatch.id
                matchedCategoryName = bestMatch.name
            }
        }

        // 4. Generate Clean Description
        var cleanDesc = rawText
            .replace(Regex("(?i)kategori\\s+\\w+(\\s+\\w+)?"), "")
            .replace(Regex("(?i)sebesar\\s+"), "")
            .replace(Regex("(?i)sebanyak\\s+"), "")
            .replace(Regex("(?i)uang\\s+"), "")
            .replace(Regex("(?i)rupiah"), "")
            .replace(Regex("(?i)rp"), "")
            .replace(Regex("\\b\\d+([.,]\\d+)?\\b"), "")
            .replace(Regex("(?i)\\b(ribu|rb|jt|juta|ratus)\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleanDesc.isBlank()) {
            cleanDesc = if (type == "income") "Pemasukan Suara" else "Pengeluaran Suara"
        } else {
            cleanDesc = cleanDesc.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        return ParsedVoiceTransaction(
            type = type,
            amount = amount,
            categoryId = matchedCategoryId,
            categoryName = matchedCategoryName,
            description = cleanDesc,
            rawSpeechText = rawText
        )
    }

    private fun parseAmount(text: String): Double {
        // A. Direct digit matches with optional 'ribu', 'rb', 'juta', 'jt', 'k'
        val regexWithSuffix = Regex("(\\d+([.,]\\d+)?)\\s*(ribu|rb|k|juta|jt)?", RegexOption.IGNORE_CASE)
        val matches = regexWithSuffix.findAll(text).toList()

        for (match in matches) {
            val numStr = match.groupValues[1].replace(".", "").replace(",", ".")
            val num = numStr.toDoubleOrNull() ?: continue
            val suffix = match.groupValues[3].lowercase()

            val multiplier = when (suffix) {
                "ribu", "rb", "k" -> 1000.0
                "juta", "jt" -> 1000000.0
                else -> 1.0
            }

            val finalAmount = num * multiplier
            if (finalAmount > 0) return finalAmount
        }

        // B. Spoken Indonesian number words
        var total = 0.0
        val words = text.split("\\s+".toRegex())

        var currentVal = 0.0
        for (w in words) {
            val valOfWord = when (w) {
                "se", "satu" -> 1.0
                "dua" -> 2.0
                "tiga" -> 3.0
                "empat" -> 4.0
                "lima" -> 5.0
                "enam" -> 6.0
                "tujuh" -> 7.0
                "delapan" -> 8.0
                "sembilan" -> 9.0
                "sepuluh" -> 10.0
                "sebelas" -> 11.0
                "duabelas", "dua belas" -> 12.0
                "seratus" -> 100.0
                "seribu" -> 1000.0
                "sejuta" -> 1000000.0
                else -> 0.0
            }

            if (w == "puluh") {
                if (currentVal > 0) currentVal *= 10.0 else currentVal = 10.0
            } else if (w == "belas") {
                if (currentVal > 0) currentVal += 10.0 else currentVal = 10.0
            } else if (w == "ratus") {
                if (currentVal > 0) currentVal *= 100.0 else currentVal = 100.0
            } else if (w == "ribu" || w == "rb") {
                if (currentVal > 0) total += currentVal * 1000.0 else total += 1000.0
                currentVal = 0.0
            } else if (w == "juta" || w == "jt") {
                if (currentVal > 0) total += currentVal * 1000000.0 else total += 1000000.0
                currentVal = 0.0
            } else if (valOfWord > 0) {
                if (valOfWord == 1000.0 || valOfWord == 1000000.0) {
                    total += valOfWord
                } else {
                    currentVal += valOfWord
                }
            }
        }

        total += currentVal
        return total
    }
}
