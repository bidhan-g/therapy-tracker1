package com.bidh.therapytracker.sync

import com.bidh.therapytracker.data.Category
import com.bidh.therapytracker.data.Session
import com.bidh.therapytracker.data.SessionStatus
import com.bidh.therapytracker.util.DateTimeUtils

// Builds a plain-text, human-readable summary for one category. This is what
// gets written to Google Drive / OneDrive so the appointments can be viewed
// from a computer by just opening a .txt file - no app or account needed.
object CategoryExporter {

    fun buildFileName(category: Category): String {
        val safeName = category.name.trim().ifEmpty { "Category" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return "$safeName.txt"
    }

    fun buildContent(category: Category, sessions: List<Session>): String {
        val sb = StringBuilder()
        sb.appendLine(category.name)
        sb.appendLine("=".repeat(category.name.length.coerceAtLeast(1)))
        sb.appendLine()

        val completed = sessions.count { it.status == SessionStatus.COMPLETED }
        val target = category.targetCount
        if (target != null && target > 0) {
            sb.appendLine("Progress: $completed / $target completed")
        } else {
            sb.appendLine("Progress: $completed completed")
        }
        sb.appendLine()

        val upcoming = sessions.filter { it.status == SessionStatus.SCHEDULED }
            .sortedBy { it.dateTimeMillis }
        if (upcoming.isNotEmpty()) {
            sb.appendLine("Upcoming:")
            upcoming.forEach { sb.appendLine("  - ${DateTimeUtils.formatDateTime(it.dateTimeMillis)}") }
            sb.appendLine()
        }

        sb.appendLine("History:")
        val history = sessions.sortedByDescending { it.dateTimeMillis }
        if (history.isEmpty()) {
            sb.appendLine("  (none yet)")
        } else {
            history.forEach {
                val status = when (it.status) {
                    SessionStatus.COMPLETED -> "Completed"
                    SessionStatus.SCHEDULED -> "Scheduled"
                    SessionStatus.MISSED -> "Missed"
                }
                sb.appendLine("  - ${DateTimeUtils.formatDateTime(it.dateTimeMillis)}  [$status]")
            }
        }
        sb.appendLine()
        sb.appendLine("Last synced: ${DateTimeUtils.formatDateTime(System.currentTimeMillis())}")

        return sb.toString()
    }
}
