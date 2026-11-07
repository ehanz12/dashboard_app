package com.reihan.dashboard.util

import com.reihan.dashboard.data.model.CategoryResponse
import com.reihan.dashboard.data.model.HabitLogTodayResponse
import com.reihan.dashboard.data.model.HabitMini
import com.reihan.dashboard.data.model.TaskResponse
import java.util.Locale

sealed class VoiceActionResult {
    data class HabitCompleted(val habitId: String, val habitName: String) : VoiceActionResult()
    data class TaskCompleted(val taskId: String, val taskTitle: String) : VoiceActionResult()
    data class TransactionDetected(val parsedTransaction: ParsedVoiceTransaction) : VoiceActionResult()
    data class NoMatch(val spokenText: String, val reason: String) : VoiceActionResult()
}

object VoiceActionProcessor {

    fun processSpeech(
        spokenText: String,
        habitsToday: List<HabitLogTodayResponse>,
        allHabits: List<HabitMini>,
        allTasks: List<TaskResponse>,
        categories: List<CategoryResponse>
    ): VoiceActionResult {
        val raw = spokenText.trim()
        val textLower = raw.lowercase(Locale.getDefault())

        val completionKeywords = listOf(
            "selesai", "sudah", "beres", "check", "centang", "tuntas", "kelar", "berhasil", "lakukan", "dilakukan"
        )
        val hasCompletionWord = completionKeywords.any { textLower.contains(it) }

        var cleanQuery = textLower
            .replace(Regex("(?i)\\b(aku|saya|tadi|sudah|selesai|beres|check|centang|tuntas|kelar|berhasil|telah|habis|dengan)\\b"), "")
            .trim()

        if (cleanQuery.isBlank()) cleanQuery = textLower

        // 1. Try Matching Habits (Priority today's habits log)
        val habitMatch = findBestHabitMatch(cleanQuery, textLower, habitsToday, allHabits)
        if (habitMatch != null && (hasCompletionWord || isStrongMatch(cleanQuery, habitMatch.second))) {
            return VoiceActionResult.HabitCompleted(
                habitId = habitMatch.first,
                habitName = habitMatch.second
            )
        }

        // 2. Try Matching Tasks
        val taskMatch = findBestTaskMatch(cleanQuery, textLower, allTasks)
        if (taskMatch != null && (hasCompletionWord || isStrongMatch(cleanQuery, taskMatch.second))) {
            return VoiceActionResult.TaskCompleted(
                taskId = taskMatch.first,
                taskTitle = taskMatch.second
            )
        }

        // 3. Check for money/transaction keywords
        val hasMoneyOrNumbers = textLower.contains("rp") || textLower.contains("rupiah") ||
                textLower.contains("ribu") || textLower.contains("rb") || textLower.contains("juta") ||
                textLower.contains("jt") || Regex("\\d+").containsMatchIn(textLower) ||
                textLower.contains("jajan") || textLower.contains("belanja") || textLower.contains("bayar") ||
                textLower.contains("dapat") || textLower.contains("gaji") || textLower.contains("uang") ||
                textLower.contains("kategori")

        if (hasMoneyOrNumbers) {
            val parsedTx = VoiceTransactionParser.parse(raw, categories)
            return VoiceActionResult.TransactionDetected(parsedTx)
        }

        // 4. Fallbacks if matched habit or task without explicit completion keyword
        if (habitMatch != null) {
            return VoiceActionResult.HabitCompleted(
                habitId = habitMatch.first,
                habitName = habitMatch.second
            )
        }

        if (taskMatch != null) {
            return VoiceActionResult.TaskCompleted(
                taskId = taskMatch.first,
                taskTitle = taskMatch.second
            )
        }

        return VoiceActionResult.NoMatch(
            spokenText = raw,
            reason = "Tidak dapat menemukan Habit, Task, atau Transaksi yang sesuai dengan ucapan \"$raw\"."
        )
    }

    private fun findBestHabitMatch(
        cleanQuery: String,
        fullText: String,
        habitsToday: List<HabitLogTodayResponse>,
        allHabits: List<HabitMini>
    ): Pair<String, String>? {
        for (item in habitsToday.filter { !it.completed }) {
            val nameLower = item.name.lowercase(Locale.getDefault())
            if (isMatch(cleanQuery, fullText, nameLower)) {
                return item.habitId to item.name
            }
        }

        for (item in habitsToday) {
            val nameLower = item.name.lowercase(Locale.getDefault())
            if (isMatch(cleanQuery, fullText, nameLower)) {
                return item.habitId to item.name
            }
        }

        for (item in allHabits) {
            val nameLower = item.name.lowercase(Locale.getDefault())
            if (isMatch(cleanQuery, fullText, nameLower)) {
                return item.id to item.name
            }
        }

        return null
    }

    private fun findBestTaskMatch(
        cleanQuery: String,
        fullText: String,
        allTasks: List<TaskResponse>
    ): Pair<String, String>? {
        for (task in allTasks.filter { !it.isCompleted }) {
            val titleLower = task.title.lowercase(Locale.getDefault())
            if (isMatch(cleanQuery, fullText, titleLower)) {
                return task.id to task.title
            }
        }
        for (task in allTasks) {
            val titleLower = task.title.lowercase(Locale.getDefault())
            if (isMatch(cleanQuery, fullText, titleLower)) {
                return task.id to task.title
            }
        }
        return null
    }

    private fun isMatch(cleanQuery: String, fullText: String, targetName: String): Boolean {
        if (targetName.isBlank()) return false
        if (fullText.contains(targetName) || cleanQuery.contains(targetName)) return true
        if (targetName.contains(cleanQuery) && cleanQuery.length >= 3) return true

        val targetWords = targetName.split("\\s+".toRegex()).filter { it.length > 2 }
        val queryWords = cleanQuery.split("\\s+".toRegex()).filter { it.length > 2 }

        val overlapCount = targetWords.count { tw -> queryWords.any { qw -> qw.contains(tw) || tw.contains(qw) } }
        return overlapCount >= 1 && (overlapCount >= targetWords.size / 2 || targetWords.size == 1)
    }

    private fun isStrongMatch(cleanQuery: String, targetName: String): Boolean {
        val targetLower = targetName.lowercase(Locale.getDefault())
        return cleanQuery.contains(targetLower) || targetLower.contains(cleanQuery)
    }
}
