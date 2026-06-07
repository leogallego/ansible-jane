package io.github.leogallego.ansiblejane.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateFormatter {

    @Volatile
    var zoneOverride: ZoneId? = null

    @Volatile
    var timeFormat: TimeFormat = TimeFormat.SYSTEM

    private val dateOnlyFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    private val zone: ZoneId
        get() = zoneOverride ?: ZoneId.systemDefault()

    private val dateTimeFormatter: DateTimeFormatter
        get() = when (timeFormat) {
            TimeFormat.SYSTEM ->
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            TimeFormat.HOURS_24 ->
                DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.getDefault())
            TimeFormat.HOURS_12 ->
                DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault())
        }

    fun formatDateTime(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val local = instant.atZone(zone)
            dateTimeFormatter.format(local)
        } catch (_: Exception) {
            isoTimestamp
        }
    }

    fun formatRelative(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val now = Instant.now()
            val minutesAgo = ChronoUnit.MINUTES.between(instant, now)
            if (minutesAgo < 0) {
                val minutesUntil = -minutesAgo
                when {
                    minutesUntil < 1 -> "now"
                    minutesUntil < 60 -> "in ${minutesUntil}m"
                    minutesUntil < 1440 -> "in ${minutesUntil / 60}h"
                    minutesUntil < 10080 -> "in ${minutesUntil / 1440}d"
                    else -> formatDateTime(isoTimestamp)
                }
            } else {
                when {
                    minutesAgo < 1 -> "just now"
                    minutesAgo < 60 -> "${minutesAgo}m ago"
                    minutesAgo < 1440 -> "${minutesAgo / 60}h ago"
                    minutesAgo < 10080 -> "${minutesAgo / 1440}d ago"
                    else -> formatDateTime(isoTimestamp)
                }
            }
        } catch (_: Exception) {
            isoTimestamp
        }
    }

    fun formatDate(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val local = instant.atZone(zone)
            dateOnlyFormatter.format(local)
        } catch (_: Exception) {
            isoTimestamp
        }
    }
}
